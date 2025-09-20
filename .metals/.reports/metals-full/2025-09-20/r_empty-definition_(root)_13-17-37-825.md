error id: file://<WORKSPACE>/src/main/scala/gibbon/strategy/DeadLetterQueue.scala:
file://<WORKSPACE>/src/main/scala/gibbon/strategy/DeadLetterQueue.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -i.
	 -i#
	 -i().
	 -scala/Predef.i.
	 -scala/Predef.i#
	 -scala/Predef.i().
offset: 101
uri: file://<WORKSPACE>/src/main/scala/gibbon/strategy/DeadLetterQueue.scala
text:
```scala
package gibbon.strategy

import akka.stream.scaladsl.{Sink, Source}
import scala.collection.mutable
i@@

final case class DeadLetter(
  originalElement: Any,
  error: StreamError,
  timestamp: java.time.Instant
)

trait DeadLetterQueue {
  
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 