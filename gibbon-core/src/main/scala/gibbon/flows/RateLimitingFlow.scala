package gibbon.flows

import gibbon.core.Flow
import gibbon.runtime.StreamingRuntime
import gibbon.ratelimiter.{RateLimiter, RateLimiterConfig}
import scala.concurrent.{Future, ExecutionContext}

class RateLimitingFlow[I, O] private (
  limiter: RateLimiter,
  transform: I => O
)(implicit ec: ExecutionContext) extends Flow[I, O] {

  override def toRuntimeFlow[R <: StreamingRuntime]()
                      (implicit rt: R): rt.Flow[I, O, rt.NotUsed] = {

    // you may choose parallelism >1 if you still want some concurrency
    val parallelism = 1

    rt.mapAsyncFlow[I, O](parallelism) { elem =>
      limiter.execute(Future { transform(elem) })
    }
  }
}

object RateLimitingFlow {
  def apply[I, O](
    name: String,
    transform: I => O,
    cfg: RateLimiterConfig
  )(implicit ec: ExecutionContext): RateLimitingFlow[I, O] =
    new RateLimitingFlow[I, O](new RateLimiter(name, cfg), transform)

  /* pass-through variant */
  def passThrough[I](
    name: String,
    cfg: RateLimiterConfig
  )(implicit ec: ExecutionContext): RateLimitingFlow[I, I] =
    apply(name, identity[I], cfg)
}