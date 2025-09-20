package gibbon.core
import gibbon.core.Event

trait Sink[E] {
  def toAkkaSink(): akka.stream.scaladsl.Sink[E, _]
}

case object ConsoleSink extends Sink[Event[_,_]] {
  def toAkkaSink() = akka.stream.scaladsl.Sink.foreach(println)
}
