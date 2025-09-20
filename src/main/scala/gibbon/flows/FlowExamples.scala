package gibbon.flows

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import gibbon.core.Event
import gibbon.sources.generator.GeneratorSource
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Comprehensive examples demonstrating all built-in flows
 */
object FlowExamples extends App {
  
  implicit val system: ActorSystem = ActorSystem("FlowExamples")
  
  println("=== Gibbon Built-in Flows Examples ===\n")
  
  // 1. FilterFlow Example
  println("1. FilterFlow Example - Filter events with value > 50")
  val filterSource = GeneratorSource.numericEvents(startKey = 1, startValue = 40, eventsPerSecond = 10, maxEvents = Some(10))
  val filterFlow = FilterFlow[Event[Int, Int]](event => event.value > 50)
  
  val filterResult = filterSource.toAkkaSource()
    .via(filterFlow.toAkkaFlow())
    .runWith(Sink.foreach(event => println(s"Filtered: key${event.key} = ${event.value}")))
  
  Await.result(filterResult, 5.seconds)
  println()
  
  // 2. TransformFlow Example
  println("2. TransformFlow Example - Convert string values to uppercase")
  val transformSource = GeneratorSource.stringEvents(keyPrefix = "transform", valuePrefix = "hello", eventsPerSecond = 5, maxEvents = Some(5))
  val transformFlow = TransformFlow[Event[String, String], Event[String, String]](event => 
    event.copy(value = event.value.toUpperCase)
  )
  
  val transformResult = transformSource.toAkkaSource()
    .via(transformFlow.toAkkaFlow())
    .runWith(Sink.foreach(event => println(s"Transformed: ${event.key} = ${event.value}")))
  
  Await.result(transformResult, 5.seconds)
  println()
  
  // 3. EnrichFlow Example
  println("3. EnrichFlow Example - Add timestamp to events")
  val enrichSource = GeneratorSource.stringEvents(keyPrefix = "enrich", valuePrefix = "data", eventsPerSecond = 3, maxEvents = Some(5))
  val enrichFlow = EnrichFlow[Event[String, String], (Event[String, String], Long)](event => 
    (event, System.currentTimeMillis())
  )
  
  val enrichResult = enrichSource.toAkkaSource()
    .via(enrichFlow.toAkkaFlow())
    .runWith(Sink.foreach { case (event, timestamp) => 
      println(s"Enriched: ${event.key} = ${event.value} at $timestamp")
    })
  
  Await.result(enrichResult, 5.seconds)
  println()
  
  // 4. WindowFlow Example
  println("4. WindowFlow Example - Sum values in 1-second windows")
  val windowSource = GeneratorSource.numericEvents(startKey = 1, startValue = 1, eventsPerSecond = 10, maxEvents = Some(20))
  val windowFlow = WindowFlow.tumbling[Event[Int, Int], Int](
    windowDuration = 1.second,
    extractor = _.value,
    aggregator = values => values.sum
  )
  
  val windowResult = windowSource.toAkkaSource()
    .via(windowFlow.toAkkaFlow())
    .runWith(Sink.foreach(sum => println(s"Window sum: $sum")))
  
  Await.result(windowResult, 10.seconds)
  println()
  
  // 5. Complex Pipeline Example
  println("5. Complex Pipeline Example - Filter, Transform, and Enrich")
  val complexSource = GeneratorSource.jsonEvents(eventsPerSecond = 6, maxEvents = Some(8))
  
  val complexFlow = FilterFlow[Event[String, String]](event => event.value.contains("data"))
    .toAkkaFlow()
    .via(TransformFlow[Event[String, String], Event[String, String]](event => 
      event.copy(value = s"PROCESSED: ${event.value}")
    ).toAkkaFlow())
    .via(EnrichFlow[Event[String, String], (Event[String, String], String)](event => 
      (event, s"enriched-${java.util.UUID.randomUUID().toString.take(8)}")
    ).toAkkaFlow())
  
  val complexResult = complexSource.toAkkaSource()
    .via(complexFlow)
    .runWith(Sink.foreach { case (event, enrichmentId) => 
      println(s"Complex: ${event.key} = ${event.value} [ID: $enrichmentId]")
    })
  
  Await.result(complexResult, 5.seconds)
  println()
  
  // 6. Utility Flows Examples
  println("6. Utility Flows Examples")
  
  // Filter null values
  val nullFilterSource = Source(List(
    Some("value1"), None, Some("value2"), None, Some("value3")
  ))
  val nullFilterFlow = FilterFlow.filterNone[String]
  
  val nullResult = nullFilterSource
    .via(nullFilterFlow.toAkkaFlow())
    .runWith(Sink.foreach(value => println(s"Non-null value: $value")))
  
  Await.result(nullResult, 5.seconds)
  
  // Transform with default
  val optionTransformFlow = TransformFlow.flattenOptionWithDefault("default")
  val optionResult = nullFilterSource
    .via(optionTransformFlow.toAkkaFlow())
    .runWith(Sink.foreach(value => println(s"Transformed option: $value")))
  
  Await.result(optionResult, 5.seconds)
  
  println("\n=== All Flow Examples Completed Successfully! ===")
  
  // Shutdown
  Await.result(system.terminate(), 10.seconds)
}