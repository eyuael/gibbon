package gibbon.sources.file

import gibbon.core.{Event, Source}
import akka.stream.scaladsl.{Source => AkkaSource}
import scala.io.{Source => ScalaSource}
import akka.NotUsed

class FileSource[K, V] (
  filePath: String,
  parser: String => Event[K, V]
) extends Source[Event[K, V]] {
  override def toAkkaSource(): AkkaSource[Event[K, V], NotUsed] = {
    AkkaSource.fromIterator(() => ScalaSource.fromFile(filePath).getLines().map(parser))
  }
}
