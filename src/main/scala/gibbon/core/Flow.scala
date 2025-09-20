package gibbon.core

trait Flow[I,O] {
  def toAkkaFlow(): akka.stream.scaladsl.Flow[I, O, _]
}
