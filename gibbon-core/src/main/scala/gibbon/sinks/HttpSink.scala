package gibbon.sinks

import gibbon.core.{Event, Sink}
import gibbon.runtime.StreamingRuntime
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}

class HttpSink[K, V](
  url: String,
  method: String = "POST",
  headers: Map[String, String] = Map.empty,
  serializer: Option[Event[K, V] => String] = None,
  retryPolicy: Option[Int => Boolean] = None
)(implicit ec: ExecutionContext) extends Sink[Event[K, V]] {

  private val serFunc = serializer.getOrElse { event: Event[K, V] =>
    s"""{"key":"${event.key}","value":"${event.value}","timestamp":${event.timestamp},"eventTime":${event.eventTime}}"""
  }
  
  private val retryFunc = retryPolicy.getOrElse((attempt: Int) => attempt < 3)

  def toRuntimeSink[R <: StreamingRuntime]()(implicit runtime: R): runtime.Sink[Event[K, V], Future[Unit]] = {
    runtime.foreachSink[Event[K, V]] { event =>
      write(event)
      () // Convert Future[Unit] to Unit for the sink
    }
  }

  def write(event: Event[K, V]): Future[Unit] = {
    // Simplified implementation - HTTP client functionality would be implemented in runtime-specific modules
    // For now, just log the event
    println(s"Would send HTTP request to $url with data: ${serFunc(event)}")
    Future.successful(())
  }

  def close(): Future[Unit] = Future.successful(())
}

object HttpSink {
  def apply[K, V](url: String)(implicit ec: ExecutionContext): HttpSink[K, V] = 
    new HttpSink[K, V](url)
    
  def apply[K, V](
    url: String, 
    method: String
  )(implicit ec: ExecutionContext): HttpSink[K, V] = 
    new HttpSink[K, V](url, method)
    
  def apply[K, V](
    url: String, 
    method: String, 
    headers: Map[String, String]
  )(implicit ec: ExecutionContext): HttpSink[K, V] = 
    new HttpSink[K, V](url, method, headers)

  def webhook[K, V](
    url: String, 
    headers: Map[String, String] = Map.empty
  )(implicit ec: ExecutionContext): HttpSink[K, V] = 
    new HttpSink[K, V](url, "POST", headers)
}