//ConsoleSink for Debugging and developement  

package gibbon.sinks

import gibbon.core.{Event, Sink}
import gibbon.runtime.StreamingRuntime
import scala.concurrent.{Future, ExecutionContext}

class ConsoleSink[K, V](
  prefix: String = "Event",
  formatter: Option[Event[K, V] => String] = None
)(implicit ec: ExecutionContext) extends Sink[Event[K, V]] {

  private val formatFunc = formatter.getOrElse { event: Event[K, V] =>
    s"$prefix: key=${event.key}, value=${event.value}, eventTime=${event.eventTime}, timestamp=${event.timestamp}"
  }

  override def toRuntimeSink[R <: StreamingRuntime]()(implicit runtime: R): runtime.Sink[Event[K, V], Future[Unit]] = {
    runtime.foreachSink { event: Event[K, V] =>
      println(formatFunc(event))
    }
  }

  def close(): Future[Unit] = Future.successful(())
}

object ConsoleSink {
  def apply[K, V]()(implicit ec: ExecutionContext): ConsoleSink[K, V] = 
    new ConsoleSink[K, V]()
    
  def apply[K, V](prefix: String)(implicit ec: ExecutionContext): ConsoleSink[K, V] = 
    new ConsoleSink[K, V](prefix)
    
  def apply[K, V](prefix: String, formatter: Event[K, V] => String)(implicit ec: ExecutionContext): ConsoleSink[K, V] = 
    new ConsoleSink[K, V](prefix, Some(formatter))
}