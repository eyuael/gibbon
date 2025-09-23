package gibbon.core
import akka.stream.scaladsl.{Source => AkkaSource}
import akka.NotUsed

trait Source[E] {
  def toAkkaSource(): AkkaSource[E, NotUsed]
}
