package gibbon.sources.generator

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink => AkkaSink}
import scala.concurrent.duration._
import scala.concurrent.Await

object GeneratorExampleVerbose extends App {
  implicit val system: ActorSystem = ActorSystem("GeneratorExampleVerbose")
  import system.dispatcher
  
  println("=== Gibbon GeneratorSource Demo ===\n")
  
  // Example 1: String Events (10 events at 2 per second)
  println("1. String Events (10 events, 2 per second):")
  val stringGen = GeneratorSource.stringEvents(
    keyPrefix = "user",
    valuePrefix = "login",
    eventsPerSecond = 2,
    maxEvents = Some(10)
  )
  
  val stringResult = stringGen.toAkkaSource()
    .runWith(AkkaSink.foreach(event => println(s"   Generated: $event")))
  
  Await.result(stringResult, 30.seconds)
  println(s"   ✓ Completed 10 string events\n")
  
  // Example 2: Numeric Events (5 events at 3 per second)
  println("2. Numeric Events (5 events, 3 per second):")
  val numericResult = GeneratorSource.numericEvents(
    startKey = 1000,
    startValue = 500,
    eventsPerSecond = 3
  ).toAkkaSource()
    .take(5)
    .runWith(AkkaSink.foreach(event => println(s"   Generated: $event")))
  
  Await.result(numericResult, 30.seconds)
  println(s"   ✓ Completed 5 numeric events\n")
  
  // Example 3: JSON Events (3 events at 1 per second)
  println("3. JSON Events (3 events, 1 per second):")
  val jsonResult = GeneratorSource.jsonEvents(
    eventsPerSecond = 1,
    maxEvents = Some(3)
  ).toAkkaSource()
    .runWith(AkkaSink.foreach(event => {
      println(s"   Generated: $event")
      println(s"   JSON Value: ${event.value}")
    }))
  
  Await.result(jsonResult, 30.seconds)
  println(s"   ✓ Completed 3 JSON events\n")
  
  // Example 4: Custom Generator
  println("4. Custom Generator (events with random delays):")
  var counter = 0
  val customGen = new GeneratorSource[String, Double](
    generator = () => {
      counter += 1
      gibbon.core.Event(
        key = s"sensor-$counter",
        value = Math.random() * 100,
        eventTime = System.currentTimeMillis(),
        timestamp = System.currentTimeMillis()
      )
    },
    eventsPerSecond = 4,
    maxEvents = Some(5),
    randomDelay = true
  )
  
  val customResult = customGen.toAkkaSource()
    .runWith(AkkaSink.foreach(event => println(s"   Generated: $event")))
  
  Await.result(customResult, 30.seconds)
  println(s"   ✓ Completed 5 custom events\n")
  
  println("=== All Generator Examples Completed Successfully! ===")
  
  system.terminate()
}