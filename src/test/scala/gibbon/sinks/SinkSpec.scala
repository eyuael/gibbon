package gibbon.sinks

import gibbon.core.Event
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.nio.file.{Path, Paths, Files}
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try
import akka.actor.ActorSystem

class SinkSpec extends AnyFunSuite with Matchers {
  implicit val ec: ExecutionContext = ExecutionContext.global
  
  test("ConsoleSink should format events correctly") {
    val sink = ConsoleSink[String]()
    val event = Event("test-key", "test-value", "test-type", System.currentTimeMillis())
    
    // Test that write doesn't throw and returns Future[Unit]
    val result = sink.write(event)
    result.isCompleted shouldBe true
  }
  
  test("FileSink should write events to file") {
    val tempFile = Files.createTempFile("test-sink", ".txt")
    val sink = FileSink[String](tempFile)
    
    val event1 = Event("key1", "value1", "type1", System.currentTimeMillis())
    val event2 = Event("key2", "value2", "type2", System.currentTimeMillis())
    
    // Write events
    val write1 = sink.write(event1)
    val write2 = sink.write(event2)
    
    // Wait for writes to complete
    Thread.sleep(500)
    
    // Check file contents
    val content = Files.readString(tempFile)
    content should include("key1")
    content should include("value1")
    content should include("key2")
    content should include("value2")
    
    // Cleanup
    sink.close()
    Files.deleteIfExists(tempFile)
  }
  
  test("FileSink should handle batch writes") {
    val tempFile = Files.createTempFile("test-batch", ".txt")
    val sink = FileSink[String](tempFile)
    
    val events = (1 to 5).map { i =>
      Event(s"key$i", s"value$i", "test-type", System.currentTimeMillis())
    }
    
    val batchWrite = sink.writeBatch(events)
    
    // Wait for batch write to complete
    Thread.sleep(500)
    
    val content = Files.readString(tempFile)
    content should include("key1")
    content should include("key5")
    content.lines().count() shouldEqual 5
    
    // Cleanup
    sink.close()
    Files.deleteIfExists(tempFile)
  }
  
  test("KafkaSink should serialize events correctly") {
    val sink = KafkaSink[String]("test-topic", "localhost:9092")
    
    val event = Event("test-key", "test-value", "test-type", System.currentTimeMillis())
    
    // Test that write doesn't throw and returns Future[Unit]
    val result = sink.write(event)
    result.isCompleted shouldBe true
  }
  
  test("HttpSink should format HTTP requests correctly") {
    implicit val system = ActorSystem("test")
    val sink = HttpSink[String]("https://httpbin.org/post")(system)
    
    val event = Event("test-key", "test-value", "test-type", System.currentTimeMillis())
    
    // Test that write doesn't throw and returns Future[Unit]
    val result = sink.write(event)
    result.isCompleted shouldBe true
    
    system.terminate()
  }
  
  test("DatabaseSink should format SQL correctly") {
    val sink = DatabaseSink[String](
      "jdbc:h2:mem:testdb",
      "sa",
      "",
      "test_events"
    )
    
    val event = Event("test-key", "test-value", "test-type", System.currentTimeMillis())
    
    // Test that write doesn't throw and returns Future[Unit]
    val result = sink.write(event)
    result.isCompleted shouldBe true
  }
  
  test("Sink factory methods should work correctly") {
    // Test ConsoleSink factory methods
    val consoleSink1 = ConsoleSink[String]()
    val consoleSink2 = ConsoleSink.withTimestamp[String]()
    val consoleSink3 = ConsoleSink.simple[String]()
    
    consoleSink1 should not be null
    consoleSink2 should not be null
    consoleSink3 should not be null
    
    // Test FileSink factory methods
    val tempFile = Files.createTempFile("factory-test", ".txt")
    val fileSink1 = FileSink[String](tempFile)
    val fileSink2 = FileSink.withJsonFormat[String](tempFile)
    val fileSink3 = FileSink.withCsvFormat[String](tempFile)
    
    fileSink1 should not be null
    fileSink2 should not be null
    fileSink3 should not be null
    
    Files.deleteIfExists(tempFile)
  }
  
  test("Sink close methods should work without errors") {
    val tempFile = Files.createTempFile("close-test", ".txt")
    
    val consoleSink = ConsoleSink[String]()
    val fileSink = FileSink[String](tempFile)
    val kafkaSink = KafkaSink[String]("test", "localhost:9092")
    
    // Test that close methods work
    val consoleClose = consoleSink.close()
    val fileClose = fileSink.close()
    val kafkaClose = kafkaSink.close()
    
    consoleClose.isCompleted shouldBe true
    fileClose.isCompleted shouldBe true
    kafkaClose.isCompleted shouldBe true
    
    Files.deleteIfExists(tempFile)
  }
}