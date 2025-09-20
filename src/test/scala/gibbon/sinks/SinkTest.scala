package gibbon.sinks

import gibbon.core.{Event, Sink}
import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}
import org.scalatest.funsuite.AnyFunSuite
import scala.concurrent.duration._

class SinkTest extends AnyFunSuite {
  implicit val system: ActorSystem = ActorSystem("SinkTest")
  implicit val ec: ExecutionContext = system.dispatcher
  
  test("DatabaseSink should compile with ExecutionContext parameter") {
    // This test just verifies that the DatabaseSink compiles with the new signature
    val sink = new DatabaseSink[String]("jdbc:h2:mem:test", "test_table")
    
    // Test that writeBatch method exists and accepts ExecutionContext
    val event = Event("key1", "value1", System.currentTimeMillis())
    val result: Future[Unit] = sink.writeBatch(Seq(event))
    
    assert(result.isInstanceOf[Future[Unit]])
  }
  
  test("FileSink should compile with ExecutionContext parameter") {
    // This test just verifies that the FileSink compiles with the new signature
    val sink = new FileSink[String]("/tmp/test.txt")
    
    // Test that writeBatch method exists and accepts ExecutionContext
    val event = Event("key1", "value1", System.currentTimeMillis())
    val result: Future[Unit] = sink.writeBatch(Seq(event))
    
    assert(result.isInstanceOf[Future[Unit]])
  }
  
  test("HttpSink should compile with ExecutionContext parameter") {
    // This test just verifies that the HttpSink compiles with the new signature
    val sink = new HttpSink[String]("http://example.com")
    
    // Test that writeBatch method exists and accepts ExecutionContext
    val event = Event("key1", "value1", System.currentTimeMillis())
    val result: Future[Unit] = sink.writeBatch(Seq(event))
    
    assert(result.isInstanceOf[Future[Unit]])
  }
  
  override def afterAll(): Unit = {
    system.terminate()
  }
}