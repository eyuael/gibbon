package gibbon.core

trait Source[E] {
  def toAkkaSource(): akka.stream.scaladsl.Source[E, Any]
}
