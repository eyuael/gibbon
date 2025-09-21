package gibbon.sinks

import gibbon.core.Event
import akka.stream.scaladsl.Source
import java.io.{ByteArrayOutputStream, PrintStream}
import scala.concurrent.Future

class ConsoleSinkTest extends SinkTestSuite {
  
  test("ConsoleSink should create instance successfully") {
    val sink = ConsoleSink[String, String]()
    assert(sink != null)
  }
  
  test("ConsoleSink should create instance with custom prefix") {
    val sink = ConsoleSink[String, String]("DEBUG")
    assert(sink != null)
  }
  
  test("ConsoleSink should create instance with custom formatter") {
    val customFormatter = (event: Event[String, String]) => s"CUSTOM: ${event.key} -> ${event.value}"
    val sink = ConsoleSink[String, String]("PREFIX", customFormatter)
    assert(sink != null)
  }
  
  test("ConsoleSink should handle empty stream") {
    val sink = ConsoleSink[String, String]()
    val source = Source.empty[Event[String, String]]
    
    val future = source.runWith(sink.toAkkaSink())
    SinkTestUtils.awaitResult(future)
    assert(future.isCompleted)
  }
  
  test("ConsoleSink close should complete successfully") {
    val sink = ConsoleSink[String, String]()
    val future = sink.close()
    SinkTestUtils.awaitResult(future)
    assert(future.isCompleted)
  }
}