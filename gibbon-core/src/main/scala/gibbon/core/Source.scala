package gibbon.core
import gibbon.runtime.StreamingRuntime

trait Source[E] {
  def toRuntimeSource[R <: StreamingRuntime]()(implicit runtime: R): runtime.Source[E, runtime.NotUsed]
}