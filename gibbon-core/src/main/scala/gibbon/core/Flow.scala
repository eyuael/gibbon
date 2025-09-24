package gibbon.core
import gibbon.runtime.StreamingRuntime

trait Flow[I,O] {
  def toRuntimeFlow[R <: StreamingRuntime]()(implicit runtime: R): runtime.Flow[I, O, runtime.NotUsed]
}