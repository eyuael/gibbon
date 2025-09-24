package gibbon.strategy

import scala.collection.mutable
import gibbon.error.{StreamError}

final case class DeadLetter(
  originalElement: Any,
  error: StreamError,
  timestamp: java.time.Instant
)

trait DeadLetterQueue {
  def enqueue(deadLetter: DeadLetter): Unit
  def dequeue(): Option[DeadLetter]
  def size: Int
}

class InMemoryDeadLetterQueue extends DeadLetterQueue {
  private val queue = mutable.Queue.empty[DeadLetter]
  
  def enqueue(deadLetter: DeadLetter): Unit = queue.enqueue(deadLetter)
  def dequeue(): Option[DeadLetter] = if (queue.nonEmpty) Some(queue.dequeue()) else None
  def size: Int = queue.size
}