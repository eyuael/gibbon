package gibbon.core

import akka.stream.scaladsl.{Sink => AkkaSink}
import scala.concurrent.Future

trait Sink[E] {
  def toAkkaSink(): AkkaSink[E, Future[Unit]]
  def close(): Future[Unit]
}
