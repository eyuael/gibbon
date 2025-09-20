package gibbon.strategy

import akka.stream.scaladsl.Flow
import akka.stream.{Supervision, ActorAttributes}
import scala.util.{Try, Success, Failure}
import gibbon.error.{ProcessingError, ErrorHandler, Stop, Resume, Restart}

object ResilientFlow {
  def apply[I, O](
    flow: Flow[I, O, Any],
    errorHandler: ErrorHandler
  ): Flow[I, O, Any] = {
    flow
      .map{ element => 
        Try(element) match {
          case Success(value) => value
          case Failure(ex) =>
            val error = ProcessingError(
              s"Failed to process element $element",
              Some(ex)
            )
            errorHandler.onError(error)
            throw error
          }
        }
        .withAttributes(ActorAttributes.supervisionStrategy { ex =>
          val strategy = errorHandler.handleError(
            ProcessingError(ex.getMessage, Some(ex))
          )
          strategy match {
            case Stop => Supervision.stop
            case Resume => Supervision.resume
            case Restart(_) => Supervision.restart
          }
          
        })
      }
}