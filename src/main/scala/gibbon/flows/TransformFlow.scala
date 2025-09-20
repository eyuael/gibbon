package gibbon.flows

import gibbon.core.Flow
import akka.stream.scaladsl.{Flow => AkkaFlow}

/**
 * TransformFlow - transforms event structure from type I to type O
 * 
 * Example usage:
 * ```scala
 * val transformFlow = TransformFlow[Event[String, String], Event[String, Int]](event => 
 *   event.copy(value = event.value.toInt)
 * )
 * ```
 */
class TransformFlow[I, O](transformer: I => O) extends Flow[I, O] {
  
  override def toAkkaFlow(): AkkaFlow[I, O, Any] = {
    AkkaFlow[I].map(transformer)
  }
}

/**
 * TransformFlow companion object with factory methods
 */
object TransformFlow {
  
  /**
   * Create a simple transform flow with a transformer function
   */
  def apply[I, O](transformer: I => O): TransformFlow[I, O] = {
    new TransformFlow[I, O](transformer)
  }
  
  /**
   * Create a transform flow that extracts values from Options, filtering out None
   */
  def flattenOption[T]: TransformFlow[Option[T], T] = {
    new TransformFlow[Option[T], T]({
      case Some(value) => value
      case None => throw new NoSuchElementException("Cannot flatten None value")
    })
  }
  
  /**
   * Create a transform flow that safely extracts values from Options, replacing None with a default
   */
  def flattenOptionWithDefault[T](default: T): TransformFlow[Option[T], T] = {
    new TransformFlow[Option[T], T](_.getOrElse(default))
  }
  
  /**
   * Create a transform flow that converts between collection types
   */
  def flattenIterable[T]: TransformFlow[Iterable[T], T] = {
    new TransformFlow[Iterable[T], T](_.iterator.next())
  }
  
  /**
   * Create a transform flow that applies multiple transformations in sequence
   */
  def chain[I, M, O](first: I => M, second: M => O): TransformFlow[I, O] = {
    new TransformFlow[I, O](first.andThen(second))
  }
}