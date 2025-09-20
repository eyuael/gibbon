package gibbon.sources.http

import gibbon.core.{Event, Source}
import akka.stream.scaladsl.{Source => AkkaSource}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.actor.ActorSystem
import akka.NotUsed
import scala.util.{Failure, Success}

class HttpSource[K, V](
  url: String,
  parser: String => Event[K, V],
  method: HttpMethod = HttpMethods.GET,
  headers: List[HttpHeader] = List.empty,
  entity: Option[RequestEntity] = None
)(implicit system: ActorSystem) extends Source[Event[K, V]] {
  
  import system.dispatcher
  
  override def toAkkaSource(): AkkaSource[Event[K, V], NotUsed] = {
    val request = HttpRequest(
      method = method,
      uri = url,
      headers = headers,
      entity = entity.getOrElse(HttpEntity.Empty)
    )
    
    // Create a source that completes the request and processes the response
    AkkaSource.lazySingle(() => ())
      .flatMapConcat { _ =>
        AkkaSource.futureSource {
          Http().singleRequest(request).map { response =>
            response.status match {
              case StatusCodes.OK =>
                response.entity.dataBytes
                  .map(_.utf8String)
                  .map(parser)
              case status =>
                // Return empty source for failed requests
                AkkaSource.empty[Event[K, V]]
            }
          }
        }
      }
  }
}