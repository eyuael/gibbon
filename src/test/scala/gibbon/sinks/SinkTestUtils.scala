package gibbon.sinks

import gibbon.core.Event
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID

/**
 * Shared test utilities and fixtures for sink tests
 */
object SinkTestUtils {
  
  /**
   * Creates a sample event for testing
   */
  def createEvent[K, V](key: K, value: V): Event[K, V] = {
    Event(
      key = key,
      value = value,
      eventTime = System.currentTimeMillis(),
      timestamp = System.currentTimeMillis()
    )
  }
  
  /**
   * Creates multiple sample events for batch testing
   */
  def createEvents[K, V](keyValuePairs: (K, V)*): List[Event[K, V]] = {
    keyValuePairs.map { case (k, v) => createEvent(k, v) }.toList
  }
  
  /**
   * Creates a source of events for testing
   */
  def createEventSource[K, V](events: Event[K, V]*): Source[Event[K, V], akka.NotUsed] = {
    Source(events.toList)
  }
  
  /**
   * Creates a source of string events for simple testing
   */
  def createStringEventSource(values: String*): Source[Event[String, String], akka.NotUsed] = {
    val events = values.zipWithIndex.map { case (value, index) =>
      createEvent(s"key-$index", value)
    }
    Source(events.toList)
  }
  
  /**
   * Waits for a future with a reasonable timeout for tests
   */
  def awaitResult[T](future: Future[T])(implicit ec: ExecutionContext): T = {
    scala.concurrent.Await.result(future, 10.seconds)
  }
  
  /**
   * Generates a unique test identifier
   */
  def generateTestId(): String = UUID.randomUUID().toString.take(8)
  
  /**
   * Creates a temporary file path for testing
   */
  def createTempFilePath(prefix: String = "test", suffix: String = ".tmp"): String = {
    val tempDir = System.getProperty("java.io.tmpdir")
    s"$tempDir/$prefix-${generateTestId()}$suffix"
  }
}

/**
 * Base test class for sink tests with common setup
 */
abstract class SinkTestSuite extends munit.FunSuite {
  
  implicit val system: ActorSystem = ActorSystem("test-system")
  implicit val ec: ExecutionContext = system.dispatcher
  
  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
  
  /**
   * Helper method to run a sink test with proper cleanup
   */
  def testSink[T](testName: String)(testBody: => Future[T]): Unit = {
    test(testName) {
      SinkTestUtils.awaitResult(testBody)
    }
  }
}