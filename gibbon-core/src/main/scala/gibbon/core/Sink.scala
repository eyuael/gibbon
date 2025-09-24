package gibbon.core
import gibbon.runtime.StreamingRuntime
import scala.concurrent.Future

trait Sink[E] {
  def toRuntimeSink[R <: StreamingRuntime]()(implicit runtime: R): runtime.Sink[E, Future[Unit]]
  def close(): Future[Unit]
}