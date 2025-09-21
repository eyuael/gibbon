package gibbon.sinks

import gibbon.core.{Event, Sink}
import akka.stream.scaladsl.{Sink => AkkaSink}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try, Success, Failure}
import java.io.{FileWriter, BufferedWriter}
import java.nio.file.{Files, Paths}

class FileSink[K, V](
  filePath: String,
  append: Boolean = true,
  formatter: Option[Event[K, V] => String] = None,
  autoFlush: Boolean = true
)(implicit ec: ExecutionContext) extends Sink[Event[K, V]] {

  private val formatFunc = formatter.getOrElse { event: Event[K, V] =>
    s"${event.timestamp},${event.key},${event.value},${event.eventTime}\n"
  }

  @volatile private var writer: Option[BufferedWriter] = None

  // Initialize writer
  private def initWriter(): Try[BufferedWriter] = Try {
    // Create parent directories if they don't exist
    val path = Paths.get(filePath)
    Option(path.getParent).foreach(Files.createDirectories(_))
    
    val fileWriter = new FileWriter(filePath, append)
    new BufferedWriter(fileWriter)
  }

  // Lazy initialization of writer
  private def getWriter(): Future[BufferedWriter] = Future {
    writer match {
      case Some(w) => w
      case None =>
        initWriter() match {
          case Success(w) =>
            writer = Some(w)
            w
          case Failure(ex) =>
            throw new RuntimeException(s"Failed to initialize FileSink writer: ${ex.getMessage}", ex)
        }
    }
  }

  def toAkkaSink(): AkkaSink[Event[K, V], Future[Unit]] = 
    AkkaSink.foreachAsync[Event[K, V]](1) { event =>
      write(event)
    }.mapMaterializedValue(_.map(_ => ()))

  def write(event: Event[K, V]): Future[Unit] = {
    getWriter().flatMap { w =>
      Future {
        w.write(formatFunc(event))
        if (autoFlush) w.flush()
      }
    }
  }

  def writeBatch(events: Seq[Event[K, V]]): Future[Unit] = {
    getWriter().flatMap { w =>
      Future {
        events.foreach { event =>
          w.write(formatFunc(event))
        }
        w.flush()
      }
    }
  }

  def close(): Future[Unit] = Future {
    writer.foreach { w =>
      Try {
        w.flush()
        w.close()
      }
    }
    writer = None
  }
}

object FileSink {
  def apply[K, V](filePath: String)(implicit ec: ExecutionContext): FileSink[K, V] = 
    new FileSink[K, V](filePath)
    
  def apply[K, V](filePath: String, append: Boolean)(implicit ec: ExecutionContext): FileSink[K, V] = 
    new FileSink[K, V](filePath, append)
    
  def apply[K, V](
    filePath: String, 
    append: Boolean, 
    formatter: Event[K, V] => String
  )(implicit ec: ExecutionContext): FileSink[K, V] = 
    new FileSink[K, V](filePath, append, Some(formatter))

  def csv[K, V](filePath: String, append: Boolean = true)(implicit ec: ExecutionContext): FileSink[K, V] = {
    val csvFormatter = { event: Event[K, V] =>
      s"${event.timestamp},${event.key},${event.value},${event.eventTime}\n"
    }
    new FileSink[K, V](filePath, append, Some(csvFormatter))
  }

  def json[K, V](filePath: String, append: Boolean = true)(implicit ec: ExecutionContext): FileSink[K, V] = {
    val jsonFormatter = { event: Event[K, V] =>
      s"""{"timestamp":${event.timestamp},"key":"${event.key}","value":"${event.value}","eventTime":${event.eventTime}}""" + "\n"
    }
    new FileSink[K, V](filePath, append, Some(jsonFormatter))
  }
}