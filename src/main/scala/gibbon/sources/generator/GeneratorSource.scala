package gibbon.sources.generator

import gibbon.core.{Event, Source}
import akka.stream.scaladsl.{Source => AkkaSource}
import akka.NotUsed
import scala.util.Random
import java.time.Instant

class GeneratorSource[K, V](
  generator: () => Event[K, V],
  eventsPerSecond: Int = 1,
  maxEvents: Option[Long] = None,
  randomDelay: Boolean = false
) extends Source[Event[K, V]] {
  
  override def toAkkaSource(): AkkaSource[Event[K, V], NotUsed] = {
    import scala.concurrent.duration._
    
    val baseSource = maxEvents match {
      case Some(limit) => AkkaSource.fromIterator(() => Iterator.fill(limit.toInt)(generator()))
      case None => AkkaSource.fromIterator(() => Iterator.continually(generator()))
    }
    
    if (randomDelay) {
      baseSource
        .throttle(eventsPerSecond, 1.second)
        .map { event =>
          // Add small random delay to simulate real-world timing
          Thread.sleep(Random.nextInt(50))
          event
        }
    } else {
      baseSource
        .throttle(eventsPerSecond, 1.second)
    }
  }
}

object GeneratorSource {
  
  // Pre-built generators for common use cases
  
  def stringEvents(
    keyPrefix: String = "key",
    valuePrefix: String = "value",
    eventsPerSecond: Int = 1,
    maxEvents: Option[Long] = None
  ): GeneratorSource[String, String] = {
    var counter = 0L
    
    new GeneratorSource[String, String](
      generator = () => {
        counter += 1
        Event(
          key = s"$keyPrefix-$counter",
          value = s"$valuePrefix-$counter",
          eventTime = System.currentTimeMillis(),
          timestamp = System.currentTimeMillis()
        )
      },
      eventsPerSecond = eventsPerSecond,
      maxEvents = maxEvents
    )
  }
  
  def numericEvents(
    startKey: Int = 1,
    startValue: Int = 100,
    eventsPerSecond: Int = 1,
    maxEvents: Option[Long] = None
  ): GeneratorSource[Int, Int] = {
    var keyCounter = startKey
    var valueCounter = startValue
    
    new GeneratorSource[Int, Int](
      generator = () => {
        keyCounter += 1
        valueCounter += Random.nextInt(10)
        Event(
          key = keyCounter,
          value = valueCounter,
          eventTime = System.currentTimeMillis(),
          timestamp = System.currentTimeMillis()
        )
      },
      eventsPerSecond = eventsPerSecond,
      maxEvents = maxEvents,
      randomDelay = true
    )
  }
  
  def jsonEvents(
    eventsPerSecond: Int = 1,
    maxEvents: Option[Long] = None
  ): GeneratorSource[String, String] = {
    var counter = 0L
    
    new GeneratorSource[String, String](
      generator = () => {
        counter += 1
        val jsonValue = s"""{"id":$counter,"timestamp":"${Instant.now()}","data":{"value":${Random.nextInt(100)}}}"""
        Event(
          key = s"event-$counter",
          value = jsonValue,
          eventTime = System.currentTimeMillis(),
          timestamp = System.currentTimeMillis()
        )
      },
      eventsPerSecond = eventsPerSecond,
      maxEvents = maxEvents,
      randomDelay = true
    )
  }
}