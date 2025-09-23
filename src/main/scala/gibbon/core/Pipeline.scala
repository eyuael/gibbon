package gibbon.core
import gibbon.core.Source
import gibbon.core.Flow
import gibbon.core.Sink
import gibbon.runtime.StreamingRuntime

final case class Pipeline[I,O](
  source: Source[I],
  flow: Flow[I,O],
  sink: Sink[O]
) {
  def toRunnableGraph(): akka.stream.scaladsl.RunnableGraph[Any] = {
    val base = source.toAkkaSource()
    val withFlows = base.via(flow.toAkkaFlow())
    withFlows.to(sink.toAkkaSink())
  }
}
