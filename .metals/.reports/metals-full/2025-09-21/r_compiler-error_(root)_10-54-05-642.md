error id: 89767767FBA02F0265455AAA78B61001
file://<WORKSPACE>/src/main/scala/gibbon/sinks/ConsoleSink.scala
### scala.reflect.internal.FatalError: no context found for source-file://<WORKSPACE>/src/main/scala/gibbon/sinks/ConsoleSink.scala,line-1,offset=21

occurred in the presentation compiler.



action parameters:
offset: 21
uri: file://<WORKSPACE>/src/main/scala/gibbon/sinks/ConsoleSink.scala
text:
```scala
//ConsoleSink for Deb@@

package gibbon.sinks

import gibbon.core.{Event, Sink}
import akka.stream.scaladsl.{Sink => AkkaSink}
import scala.concurrent.{Future, ExecutionContext}

class ConsoleSink[K, V](
  prefix: String = "Event",
  formatter: Option[Event[K, V] => String] = None
)(implicit ec: ExecutionContext) extends Sink[Event[K, V]] {

  private val formatFunc = formatter.getOrElse { event: Event[K, V] =>
    s"$prefix: key=${event.key}, value=${event.value}, eventTime=${event.eventTime}, timestamp=${event.timestamp}"
  }

  def toAkkaSink(): AkkaSink[Event[K, V], Future[Unit]] = 
    AkkaSink.foreachAsync[Event[K, V]](1) { event =>
      Future {
        println(formatFunc(event))
      }
    }.mapMaterializedValue(_.map(_ => ()))

  def close(): Future[Unit] = Future.successful(())
}

object ConsoleSink {
  def apply[K, V]()(implicit ec: ExecutionContext): ConsoleSink[K, V] = 
    new ConsoleSink[K, V]()
    
  def apply[K, V](prefix: String)(implicit ec: ExecutionContext): ConsoleSink[K, V] = 
    new ConsoleSink[K, V](prefix)
    
  def apply[K, V](prefix: String, formatter: Event[K, V] => String)(implicit ec: ExecutionContext): ConsoleSink[K, V] = 
    new ConsoleSink[K, V](prefix, Some(formatter))
}
```


presentation compiler configuration:
Scala version: 2.13.16
Classpath:
<WORKSPACE>/.bloop/root/bloop-bsp-clients-classes/classes-Metals-Yp_fLE6qQHCkZ_aNc2N9eQ== [exists ], <HOME>/Library/Caches/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.0/semanticdb-javac-0.11.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.16/scala-library-2.13.16.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-stream_2.13/2.8.5/akka-stream_2.13-2.8.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-http_2.13/10.5.3/akka-http_2.13-10.5.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-http-spray-json_2.13/10.5.3/akka-http-spray-json_2.13-10.5.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-core_2.13/0.14.7/circe-core_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-generic_2.13/0.14.7/circe-generic_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-parser_2.13/0.14.7/circe-parser_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/ch/qos/logback/logback-classic/1.5.6/logback-classic-1.5.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/scala-logging/scala-logging_2.13/3.9.5/scala-logging_2.13-3.9.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-stream-kafka_2.13/4.0.2/akka-stream-kafka_2.13-4.0.2.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/kafka/kafka-clients/3.5.1/kafka-clients-3.5.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-actor_2.13/2.8.5/akka-actor_2.13-2.8.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-protobuf-v3_2.13/2.8.5/akka-protobuf-v3_2.13-2.8.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/reactivestreams/reactive-streams/1.0.4/reactive-streams-1.0.4.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/ssl-config-core_2.13/0.6.1/ssl-config-core_2.13-0.6.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-http-core_2.13/10.5.3/akka-http-core_2.13-10.5.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/spray/spray-json_2.13/1.3.6/spray-json_2.13-1.3.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-numbers_2.13/0.14.7/circe-numbers_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/typelevel/cats-core_2.13/2.10.0/cats-core_2.13-2.10.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.10/shapeless_2.13-2.3.10.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-jawn_2.13/0.14.7/circe-jawn_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/ch/qos/logback/logback-core/1.5.6/logback-core-1.5.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.16/scala-reflect-2.13.16.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/github/luben/zstd-jni/1.5.5-1/zstd-jni-1.5.5-1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/lz4/lz4-java/1.8.0/lz4-java-1.8.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/xerial/snappy/snappy-java/1.1.10.1/snappy-java-1.1.10.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/config/1.4.2/config-1.4.2.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-java8-compat_2.13/1.0.0/scala-java8-compat_2.13-1.0.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-parsing_2.13/10.5.3/akka-parsing_2.13-10.5.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/typelevel/cats-kernel_2.13/2.10.0/cats-kernel_2.13-2.10.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/typelevel/jawn-parser_2.13/1.5.1/jawn-parser_2.13-1.5.1.jar [exists ]
Options:
-Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.tools.nsc.interactive.CompilerControl.$anonfun$doLocateContext$1(CompilerControl.scala:100)
	scala.tools.nsc.interactive.CompilerControl.doLocateContext(CompilerControl.scala:100)
	scala.tools.nsc.interactive.CompilerControl.doLocateContext$(CompilerControl.scala:99)
	scala.tools.nsc.interactive.Global.doLocateContext(Global.scala:115)
	scala.meta.internal.pc.PcDefinitionProvider.definitionTypedTreeAt(PcDefinitionProvider.scala:181)
	scala.meta.internal.pc.PcDefinitionProvider.definition(PcDefinitionProvider.scala:69)
	scala.meta.internal.pc.PcDefinitionProvider.definition(PcDefinitionProvider.scala:17)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$definition$1(ScalaPresentationCompiler.scala:495)
	scala.meta.internal.pc.CompilerAccess.retryWithCleanCompiler(CompilerAccess.scala:182)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withSharedCompiler$1(CompilerAccess.scala:155)
	scala.Option.map(Option.scala:242)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:154)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withNonInterruptableCompiler$1(CompilerAccess.scala:132)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1095)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:619)
	java.base/java.lang.Thread.run(Thread.java:1447)
```
#### Short summary: 

scala.reflect.internal.FatalError: no context found for source-file://<WORKSPACE>/src/main/scala/gibbon/sinks/ConsoleSink.scala,line-1,offset=21