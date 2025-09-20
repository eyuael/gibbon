package gibbon.flows

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import gibbon.core.Event
import munit.FunSuite
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Unit tests for all built-in flows using MUnit
 */
class FlowSpec extends FunSuite {
  
  implicit val system: ActorSystem = ActorSystem("FlowSpec")
  
  override def afterAll(): Unit = {
    Await.result(system.terminate(), 10.seconds)
    super.afterAll()
  }
  
  test("FilterFlow should filter events based on predicate") {
    val events = List(
      Event("key1", "value1", System.currentTimeMillis()),
      Event("key2", "value2", System.currentTimeMillis()),
      Event("key3", "value3", System.currentTimeMillis())
    )
    
    val filterFlow = FilterFlow[Event[String, String]](event => event.key.contains("1"))
    
    val result = Source(events)
      .via(filterFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val filteredEvents = Await.result(result, 3.seconds)
    assertEquals(filteredEvents.size, 1)
    assert(filteredEvents.forall(_.key.contains("1")))
  }
  
  test("FilterFlow should filter out null values") {
    val values = List("value1", "null", "value2", "null", "value3") // Use string "null" instead of actual null
    val filterFlow = FilterFlow[String](_ != "null")
    
    val result = Source(values)
      .via(filterFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val filteredValues = Await.result(result, 3.seconds)
    assertEquals(filteredValues.size, 3)
    assert(!filteredValues.contains("null"))
  }
  
  test("FilterFlow should filter out None values") {
    val options = List(Some("value1"), None, Some("value2"), None, Some("value3"))
    val filterFlow = FilterFlow.filterNone[String]
    
    val result = Source(options)
      .via(filterFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val filteredOptions = Await.result(result, 3.seconds)
    assertEquals(filteredOptions.size, 3)
    assert(!filteredOptions.contains(None))
  }
  
  test("TransformFlow should transform event structure") {
    val events = List(
      Event("key1", "100", System.currentTimeMillis()),
      Event("key2", "200", System.currentTimeMillis())
    )
    
    val transformFlow = TransformFlow[Event[String, String], Event[String, Int]](event => 
      event.copy(value = event.value.toInt)
    )
    
    val result = Source(events)
      .via(transformFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val transformedEvents = Await.result(result, 3.seconds)
    assertEquals(transformedEvents.size, 2)
    assertEquals(transformedEvents.head.value, 100)
    assertEquals(transformedEvents(1).value, 200)
  }
  
  test("TransformFlow should flatten Option with default value") {
    val options = List(Some("value1"), None, Some("value2"))
    val transformFlow = TransformFlow.flattenOptionWithDefault("default")
    
    val result = Source(options)
      .via(transformFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val transformedValues = Await.result(result, 3.seconds)
    assertEquals(transformedValues.size, 3)
    assert(transformedValues.contains("default"))
  }
  
  test("EnrichFlow should add additional data to events") {
    val events = List(
      Event("key1", "value1", System.currentTimeMillis()),
      Event("key2", "value2", System.currentTimeMillis())
    )
    
    val enrichFlow = EnrichFlow[Event[String, String], (Event[String, String], String)](event => 
      (event, s"enriched-${event.key}")
    )
    
    val result = Source(events)
      .via(enrichFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val enrichedEvents = Await.result(result, 3.seconds)
    assertEquals(enrichedEvents.size, 2)
    assertEquals(enrichedEvents.head._2, "enriched-key1")
    assertEquals(enrichedEvents(1)._2, "enriched-key2")
  }
  
  test("EnrichFlow should add timestamp to events") {
    val events = List(
      Event("key1", "value1", System.currentTimeMillis())
    )
    
    val enrichFlow = EnrichFlow.addTimestamp[Event[String, String]]
    
    val result = Source(events)
      .via(enrichFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val enrichedEvents = Await.result(result, 3.seconds)
    assertEquals(enrichedEvents.size, 1)
    assert(enrichedEvents.head._2 > 0L)
  }
  
  test("EnrichFlow should add unique ID to events") {
    val events = List(
      Event("key1", "value1", System.currentTimeMillis()),
      Event("key2", "value2", System.currentTimeMillis())
    )
    
    val enrichFlow = EnrichFlow.addUniqueId[Event[String, String]]
    
    val result = Source(events)
      .via(enrichFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val enrichedEvents = Await.result(result, 3.seconds)
    assertEquals(enrichedEvents.size, 2)
    assert(enrichedEvents.head._2 != enrichedEvents(1)._2) // IDs should be unique
  }
  
  test("WindowFlow should aggregate events in count windows") {
    val events = (1 to 10).map(i => Event(s"key$i", s"value$i", System.currentTimeMillis()))
    
    val countWindowFlow = WindowFlow.count[Event[String, String], Int](
      windowSize = 3,
      extractor = _ => 1, // Count each event as 1
      aggregator = values => values.sum
    )
    
    val result = Source(events)
      .via(countWindowFlow.toAkkaFlow())
      .runWith(Sink.seq)
    
    val windowedResults = Await.result(result, 3.seconds)
    assertEquals(windowedResults.size, 4) // 10 events in windows of 3 = 3 full windows + 1 partial
    assertEquals(windowedResults.head, 3) // First window has 3 events
  }
  
  test("Complex flows should work together in a pipeline") {
    val events = List(
      Event("key1", "100", System.currentTimeMillis()),
      Event("key2", "200", System.currentTimeMillis()),
      Event("key3", "50", System.currentTimeMillis()),
      Event("key4", "300", System.currentTimeMillis())
    )
    
    val pipeline = FilterFlow[Event[String, String]](event => event.value.toInt > 100)
      .toAkkaFlow()
      .via(TransformFlow[Event[String, String], Event[String, Int]](event => 
        event.copy(value = event.value.toInt)
      ).toAkkaFlow())
      .via(EnrichFlow[Event[String, Int], (Event[String, Int], Boolean)](event => 
        (event, event.value > 150)
      ).toAkkaFlow())
    
    val result = Source(events)
      .via(pipeline)
      .runWith(Sink.seq)
    
    val pipelineResults = Await.result(result, 3.seconds)
    assertEquals(pipelineResults.size, 2) // Only 2 events have value > 100 (200 and 300)
    assertEquals(pipelineResults.head._2, true)   // 200 is > 150
    assertEquals(pipelineResults(1)._2, true)   // 300 is > 150
  }
}