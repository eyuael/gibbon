/*
 * A token bucket rate limiter.
 *
 * The limiter allows a fixed number of tokens to be consumed per interval,
 * refilling the bucket at a specified rate. If the bucket is empty, requests
 * are queued and completed when tokens become available.
 */

package gibbon.ratelimiter

import java.time.{Instant, Duration}
import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.collection.mutable

case class RateLimiterConfig(
  maxTokens: Int,
  refillEvery: Duration,
  refillAmount: Int
)

class RateLimiter(val name: String, cfg: RateLimiterConfig)(
    implicit ec: ExecutionContext
) {

  private var available = cfg.maxTokens
  private var lastRefill = Instant.now()

  private val queue = mutable.Queue[Promise[Unit]]()

  private def refill(): Unit = {
    val now = Instant.now()
    val elapsed = Duration.between(lastRefill, now)
    if (elapsed.compareTo(cfg.refillEvery) >= 0) {
      val cycles = elapsed.toMillis / cfg.refillEvery.toMillis
      val refillTokens = (cycles * cfg.refillAmount).toInt
      available = (available + refillTokens).min(cfg.maxTokens)
      lastRefill = now
    }
  }

  /** Obtain a permit; completes when the caller may proceed */
  def acquire(): Future[Unit] = synchronized {
    refill()
    if (available > 0) {
      available -= 1
      Future.successful(())
    } else {
      val p = Promise[Unit]()
      queue.enqueue(p)
      p.future
    }
  }

  /** Call when a permit has been used to make space for the next waiter */
  private def release(): Unit = synchronized {
    refill()
    if (queue.nonEmpty && available > 0) {
      available -= 1
      queue.dequeue().success(())
    }
  }

  /** Convenience for wrapping an arbitrary operation */
  def execute[T](op: => Future[T]): Future[T] =
    acquire().flatMap(_ => op).andThen { case _ => release() }
}