package gibbon.core
import akka.stream.scaladsl.{Flow => AkkaFlow}

trait Flow[I,O] {
  def toAkkaFlow(): AkkaFlow[I, O, Any]
}
