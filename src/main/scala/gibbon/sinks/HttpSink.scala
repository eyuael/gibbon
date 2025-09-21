package gibbon.sinks

import gibbon.core.{Event, Sink}
import akka.stream.scaladsl.{Sink => AkkaSink}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}

class HttpSink[K, V](
  url: String,
  method: HttpMethod = HttpMethods.POST,
  headers: Map[String, String] = Map.empty,
  serializer: Option[Event[K, V] => String] = None,
  retryPolicy: Option[Int => Boolean] = None
)(implicit system: ActorSystem, ec: ExecutionContext) extends Sink[Event[K, V]] {

  private val serFunc = serializer.getOrElse { event: Event[K, V] =>
    s"""{"key":"${event.key}","value":"${event.value}","timestamp":${event.timestamp},"eventTime":${event.eventTime}}"""
  }
  
  private val retryFunc = retryPolicy.getOrElse((attempt: Int) => attempt < 3)

  def toAkkaSink(): AkkaSink[Event[K, V], Future[Unit]] = 
    AkkaSink.foreachAsync[Event[K, V]](1) { event =>
      write(event)
    }.mapMaterializedValue(_.map(_ => ()))

  def write(event: Event[K, V]): Future[Unit] = {
    sendWithRetry(event, 0)
  }

  private def sendWithRetry(event: Event[K, V], attempt: Int): Future[Unit] = {
    val jsonData = serFunc(event)
    
    val httpHeaders = headers.map { case (k, v) => 
      HttpHeader.parse(k, v) match {
        case HttpHeader.ParsingResult.Ok(header, _) => header
        case _ => throw new IllegalArgumentException(s"Invalid header: $k: $v")
      }
    }.toList

    val request = HttpRequest(
      method = method,
      uri = url,
      headers = httpHeaders,
      entity = HttpEntity(ContentTypes.`application/json`, jsonData)
    )

    Http().singleRequest(request).flatMap { response =>
      if (response.status.isSuccess()) {
        response.discardEntityBytes()
        Future.successful(())
      } else {
        response.discardEntityBytes()
        if (retryFunc(attempt)) {
          sendWithRetry(event, attempt + 1)
        } else {
          Future.failed(new RuntimeException(s"HTTP request failed with status: ${response.status}"))
        }
      }
    }.recoverWith {
      case ex if retryFunc(attempt) =>
        sendWithRetry(event, attempt + 1)
      case ex =>
        Future.failed(ex)
    }
  }

  def close(): Future[Unit] = Future.successful(())
}

object HttpSink {
  def apply[K, V](url: String)(implicit system: ActorSystem, ec: ExecutionContext): HttpSink[K, V] = 
    new HttpSink[K, V](url)
    
  def apply[K, V](
    url: String, 
    method: HttpMethod
  )(implicit system: ActorSystem, ec: ExecutionContext): HttpSink[K, V] = 
    new HttpSink[K, V](url, method)
    
  def apply[K, V](
    url: String, 
    method: HttpMethod, 
    headers: Map[String, String]
  )(implicit system: ActorSystem, ec: ExecutionContext): HttpSink[K, V] = 
    new HttpSink[K, V](url, method, headers)

  def webhook[K, V](
    url: String, 
    headers: Map[String, String] = Map.empty
  )(implicit system: ActorSystem, ec: ExecutionContext): HttpSink[K, V] = 
    new HttpSink[K, V](url, HttpMethods.POST, headers)
}