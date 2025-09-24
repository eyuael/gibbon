package gibbon.flows

import gibbon.core.Event
import gibbon.runtime.{TestStreamingRuntime, TestSource}
import munit.FunSuite
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class FlowSpec extends FunSuite {
  
  val runtime = new TestStreamingRuntime()
  implicit val testRuntime: TestStreamingRuntime = runtime
  
  test("FilterFlow should filter events based on predicate") {
    val events = List(
      Event("key1", "value1", System.currentTimeMillis()),
      Event("key2", "value2", System.currentTimeMillis()),
      Event("key3", "value3", System.currentTimeMillis())
    )
    
    val filterFlow = FilterFlow[Event[String, String]](_.key != "key2")
    val source = TestSource(events: _*)
    val sink = runtime.seqSink[Event[String, String]]
    
    val result = source.via(runtime.asTestFlow(filterFlow.toRuntimeFlow())).runWith(sink)
    val filteredEvents = Await.result(result, 3.seconds)
    
    assertEquals(filteredEvents.size, 2)
    assert(!filteredEvents.exists(_.key == "key2"))
  }
  
  test("TransformFlow should transform events") {
    val events = List(
      Event("key1", "100", System.currentTimeMillis()),
      Event("key2", "200", System.currentTimeMillis())
    )
    
    val transformFlow = TransformFlow[Event[String, String], Event[String, Int]](event => 
      event.copy(value = event.value.toInt)
    )
    
    val source = TestSource(events: _*)
    val sink = runtime.seqSink[Event[String, Int]]
    
    val result = source.via(runtime.asTestFlow(transformFlow.toRuntimeFlow())).runWith(sink)
    val transformedEvents = Await.result(result, 3.seconds)
    
    assertEquals(transformedEvents.size, 2)
    assertEquals(transformedEvents.head.value, 100)
    assertEquals(transformedEvents(1).value, 200)
  }
  
  test("EnrichFlow should add data to events") {
    val events = List(
      Event("key1", "value1", System.currentTimeMillis())
    )
    
    val enrichFlow = EnrichFlow[Event[String, String], (Event[String, String], String)](event => 
      (event, s"enriched-${event.key}")
    )
    
    val source = TestSource(events: _*)
    val sink = runtime.seqSink[(Event[String, String], String)]
    
    val result = source.via(runtime.asTestFlow(enrichFlow.toRuntimeFlow())).runWith(sink)
    val enrichedEvents = Await.result(result, 3.seconds)
    
    assertEquals(enrichedEvents.size, 1)
    assertEquals(enrichedEvents.head._2, "enriched-key1")
  }
}