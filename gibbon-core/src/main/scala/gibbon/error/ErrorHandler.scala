package gibbon.error

sealed trait ErrorHandlingStrategy
case object Stop extends ErrorHandlingStrategy
case object Resume extends ErrorHandlingStrategy
case class Restart(maxRetries: Int) extends ErrorHandlingStrategy

trait ErrorHandler {
  def handleError(error: StreamError): ErrorHandlingStrategy
  def onError(error: StreamError): Unit
} 
