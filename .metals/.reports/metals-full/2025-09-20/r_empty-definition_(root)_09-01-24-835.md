error id: file://<WORKSPACE>/src/main/scala/gibbon/EventStore.scala:gibbon/EventStore#
file://<WORKSPACE>/src/main/scala/gibbon/EventStore.scala
empty definition using pc, found symbol in pc: 
found definition using semanticdb; symbol gibbon/EventStore#
empty definition using fallback
non-local guesses:

offset: 107
uri: file://<WORKSPACE>/src/main/scala/gibbon/EventStore.scala
text:
```scala
package gibbon

import scala.concurrent.Future
import scala.collection.concurrent.TrieMap

trait EventStore@@ {
  def get(key: String): Future[Option[Event]]
  def put(key: String, value: Event): Future[Unit]
  
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 