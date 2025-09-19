package gibbon

trait Flow[I,O] {
  def toAkkaFlow(): akka.stream.scaladsl.Flow[I, O, _]
}
