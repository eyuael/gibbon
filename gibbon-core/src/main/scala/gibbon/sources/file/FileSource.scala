package gibbon.sources.file

import gibbon.core.{Event, Source}
import gibbon.runtime.StreamingRuntime
import scala.io.{Source => ScalaSource}

class FileSource[K, V] (
  filePath: String,
  parser: String => Event[K, V]
) extends Source[Event[K, V]] {
  override def toRuntimeSource[R <: StreamingRuntime]()(implicit runtime: R): runtime.Source[Event[K, V], runtime.NotUsed] = {
    runtime.fromIterator(() => ScalaSource.fromFile(filePath).getLines().map(parser))
  }
}
