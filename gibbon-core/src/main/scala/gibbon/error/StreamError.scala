package gibbon.error

// 1. Use a sealed trait for exhaustivity checking
sealed trait StreamError {
  def message: String
  def cause: Option[Throwable]
}

// 2. The case classes are now simpler. No need for 'override'.
final case class SourceError(message: String, cause: Option[Throwable] = None) extends StreamError
final case class ProcessingError(message: String, cause: Option[Throwable] = None) extends StreamError
final case class SinkError(message: String, cause: Option[Throwable] = None) extends StreamError
final case class CircuitBreakerError(message: String, cause: Option[Throwable] = None) extends StreamError
final case class CircuitBreakerOpenError(message: String, cause: Option[Throwable] = None) extends StreamError