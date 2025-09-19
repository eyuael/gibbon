package gibbon

trait Source[E] {
  def toAkkaSource(): akka.stream.scaladsl.Source[E, _]
}
