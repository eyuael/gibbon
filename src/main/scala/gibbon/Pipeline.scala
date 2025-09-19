package gibbon

final case class Pipeline[I,O](
    source: Source[I],
    flow: Flow[I,O],
    sink: Sink[O]
) {
  def toRunnableGraph(): akka.stream.scaladsl.RunnableGraph[_] = {
    val base = source.toAkkaSource()
    val withFlows = base.via(flow.toAkkaFlow())
    withFlows.to(sink.toAkkaSink())
  }
}
