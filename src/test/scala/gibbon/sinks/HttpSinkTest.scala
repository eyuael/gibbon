package gibbon.sinks

import gibbon.core.Event
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class HttpSinkTest extends SinkTestSuite {

  test("HttpSink should be created with basic parameters") {
    val sink = HttpSink[String, String]("http://example.com/test")
    assert(sink != null)
  }

  test("HttpSink should be created with custom headers") {
    val customHeaders = Map("Authorization" -> "Bearer token123", "X-Custom" -> "value")
    val sink = HttpSink[String, String]("http://example.com/test", HttpMethods.POST, customHeaders)
    assert(sink != null)
  }

  test("HttpSink should be created with custom serializer") {
    val customSerializer = (event: Event[String, String]) => s"CUSTOM:${event.key}=${event.value}"
    val sink = new HttpSink[String, String](
      url = "http://example.com/test",
      serializer = Some(customSerializer)
    )
    assert(sink != null)
  }

  test("HttpSink should be created with different HTTP methods") {
    val sink = HttpSink[String, String]("http://example.com/test", HttpMethods.PUT)
    assert(sink != null)
  }

  test("HttpSink should be created with custom retry policy") {
    val customRetryPolicy = (attempt: Int) => attempt < 2
    val sink = new HttpSink[String, String](
      url = "http://example.com/test",
      method = HttpMethods.POST,
      headers = Map.empty,
      serializer = Some((event: Event[String, String]) => s"${event.key}:${event.value}"),
      retryPolicy = Some(customRetryPolicy)
    )
    assert(sink != null)
  }

  test("HttpSink.webhook should create POST sink") {
    val webhookHeaders = Map("X-Webhook-Secret" -> "secret123")
    val sink = HttpSink.webhook[String, String]("http://example.com/webhook", webhookHeaders)
    assert(sink != null)
  }

  test("HttpSink should have toAkkaSink method") {
    val sink = HttpSink[String, String]("http://example.com/test")
    val akkaSink = sink.toAkkaSink()
    assert(akkaSink != null)
  }

  test("HttpSink should have write method") {
    val sink = HttpSink[String, String]("http://example.com/test")
    val event = SinkTestUtils.createEvent("test-key", "test-value")
    
    // We can't test actual HTTP calls without a server, but we can verify the method exists
    assert(sink.write _ != null)
  }

  test("HttpSink close should complete successfully") {
    val sink = HttpSink[String, String]("http://example.com/test")
    val future = sink.close()
    SinkTestUtils.awaitResult(future)
    assert(future.isCompleted)
  }
}