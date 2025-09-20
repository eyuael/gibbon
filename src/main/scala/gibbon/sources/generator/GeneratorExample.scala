package gibbon.sources.generator

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink => AkkaSink}
import scala.concurrent.duration._

object GeneratorExample extends App {
  implicit val system: ActorSystem = ActorSystem("GeneratorExample")
  import system.dispatcher
  
  // Example 1: Generate string events at 2 events per second
  val stringGenerator = GeneratorSource.stringEvents(
    keyPrefix = "user",
    valuePrefix = "login",
    eventsPerSecond = 2,
    maxEvents = Some(10) // Generate only 10 events
  )
  
  println("=== String Events Example ===")
  stringGenerator.toAkkaSource()
    .runWith(AkkaSink.foreach(event => println(s"String Event: $event")))
    .onComplete { _ =>
      // Example 2: Generate numeric events continuously
      println("\n=== Numeric Events Example ===")
      GeneratorSource.numericEvents(
        startKey = 1000,
        startValue = 500,
        eventsPerSecond = 3
      ).toAkkaSource()
        .take(5) // Take only first 5 events
        .runWith(AkkaSink.foreach(event => println(s"Numeric Event: $event")))
        .onComplete { _ =>
          // Example 3: Generate JSON events
          println("\n=== JSON Events Example ===")
          GeneratorSource.jsonEvents(
            eventsPerSecond = 1,
            maxEvents = Some(3)
          ).toAkkaSource()
            .runWith(AkkaSink.foreach(event => println(s"JSON Event: $event")))
            .onComplete { _ =>
              system.terminate()
            }
        }
    }
}