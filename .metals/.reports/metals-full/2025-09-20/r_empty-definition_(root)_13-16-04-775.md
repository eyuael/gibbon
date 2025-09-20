error id: file://<WORKSPACE>/src/main/scala/gibbon/strategy/ResilientFlow.scala:gibbon/error/ErrorHandler#
file://<WORKSPACE>/src/main/scala/gibbon/strategy/ResilientFlow.scala
empty definition using pc, found symbol in pc: 
found definition using semanticdb; symbol gibbon/error/ErrorHandler#
empty definition using fallback
non-local guesses:

offset: 203
uri: file://<WORKSPACE>/src/main/scala/gibbon/strategy/ResilientFlow.scala
text:
```scala
package gibbon.strategy

import akka.stream.scaladsl.Flow
import akka.stream.{Supervision, ActorAttributes}
import scala.util.{Try, Success, Failure}
import gibbon.error.{ProcessingError, ErrorHandler, S@@}

object ResilientFlow {
  def apply[I, O](
    flow: Flow[I, O, _],
    errorHandler: ErrorHandler
  ): Flow[I, O, _] = {
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
            case Restart(maxRetries) => Supervision.restart
          }
          
        })
      }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 