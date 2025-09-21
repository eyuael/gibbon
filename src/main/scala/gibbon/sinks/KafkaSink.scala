package gibbon.sinks

import gibbon.core.{Event, Sink}
import akka.stream.scaladsl.{Sink => AkkaSink}
import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.util.{Try, Success, Failure}
import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, Callback, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

class KafkaSink[K, V](
  topic: String,
  bootstrapServers: String,
  keySerializer: Option[String] = None,
  valueSerializer: Option[String] = None,
  producerSettings: Map[String, String] = Map.empty
)(implicit ec: ExecutionContext) extends Sink[Event[K, V]] {

  private val producerProps = new Properties()
  producerProps.put("bootstrap.servers", bootstrapServers)
  producerProps.put("key.serializer", keySerializer.getOrElse(classOf[StringSerializer].getName))
  producerProps.put("value.serializer", valueSerializer.getOrElse(classOf[StringSerializer].getName))
  producerProps.put("acks", "all")
  producerProps.put("retries", "3")
  producerProps.put("batch.size", "16384")
  producerProps.put("linger.ms", "1")
  producerProps.put("buffer.memory", "33554432")
  
  // Apply custom producer settings
  producerSettings.foreach { case (k, v) => producerProps.put(k, v) }

  private val producer = new KafkaProducer[String, String](producerProps)

  def toAkkaSink(): AkkaSink[Event[K, V], Future[Unit]] = 
    AkkaSink.foreachAsync[Event[K, V]](1) { event =>
      write(event)
    }.mapMaterializedValue(_.map(_ => ()))

  def write(event: Event[K, V]): Future[Unit] = {
    val promise = Promise[Unit]()
    
    val record = new ProducerRecord[String, String](
      topic, 
      event.key.toString, 
      event.value.toString
    )

    producer.send(record, new Callback {
      override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
        if (exception != null) {
          promise.failure(exception)
        } else {
          promise.success(())
        }
      }
    })

    promise.future
  }

  def writeBatch(events: Seq[Event[K, V]]): Future[Unit] = {
    val futures = events.map(write)
    Future.sequence(futures).map(_ => ())
  }

  def close(): Future[Unit] = Future {
    Try {
      producer.flush()
      producer.close()
    }
  }
}

object KafkaSink {
  def apply[K, V](
    topic: String, 
    bootstrapServers: String
  )(implicit ec: ExecutionContext): KafkaSink[K, V] = 
    new KafkaSink[K, V](topic, bootstrapServers)
    
  def apply[K, V](
    topic: String, 
    bootstrapServers: String, 
    producerSettings: Map[String, String]
  )(implicit ec: ExecutionContext): KafkaSink[K, V] = 
    new KafkaSink[K, V](topic, bootstrapServers, producerSettings = producerSettings)
}