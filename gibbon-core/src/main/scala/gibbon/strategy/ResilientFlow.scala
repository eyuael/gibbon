package gibbon.strategy

import gibbon.runtime.StreamingRuntime
import scala.util.{Try, Success, Failure}
import gibbon.error.{ProcessingError, ErrorHandler, Stop, Resume, Restart}

object ResilientFlow {
  def apply[I, O, R <: StreamingRuntime](
    flow: R#Flow[I, O, Any],
    errorHandler: ErrorHandler
  )(implicit runtime: R): R#Flow[I, O, Any] = {
    // Simplified implementation - error handling and supervision would be implemented in runtime-specific modules
    runtime.mapFlow[I, O] { element =>
      Try(element) match {
        case Success(value) => value.asInstanceOf[O]
        case Failure(ex) =>
          val error = ProcessingError(
            s"Failed to process element $element",
            Some(ex)
          )
          errorHandler.onError(error)
          throw ex
      }
    }
  }
}