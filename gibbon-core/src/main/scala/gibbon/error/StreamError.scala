package gibbon.error

abstract class StreamError(
  val message: String,
  val cause: Option[Throwable] = None
) extends Exception(message, cause.orNull) {
  override def getMessage: String = message
}

final case class SourceError(
    override val message: String,
    override val cause: Option[Throwable] = None
) extends StreamError(message, cause)

final case class ProcessingError(
    override val message: String,
    override val cause: Option[Throwable] = None
) extends StreamError(message, cause)

final case class SinkError(
    override val message: String,
    override val cause: Option[Throwable] = None
) extends StreamError(message, cause)

final case class CircuitBreakerError(
    override val message: String,
    override val cause: Option[Throwable] = None
) extends StreamError(message, cause)

final case class CircuitBreakerOpenError(
    override val message: String,
    override val cause: Option[Throwable] = None
) extends StreamError(message, cause)