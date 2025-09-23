error id: B0EEC37EAB19794F9D2ADAC90155F1F5
file://<WORKSPACE>/src/main/scala/gibbon/runtime/PekkoStreamingRuntime.scala
### java.util.NoSuchElementException: head of empty list

occurred in the presentation compiler.



action parameters:
uri: file://<WORKSPACE>/src/main/scala/gibbon/runtime/PekkoStreamingRuntime.scala
text:
```scala
package gibbon.runtime

import org.apache.pekko.{actor => pekko}
import org.apache.pekko.stream.scaladsl.{Source => PekkoSource, Flow => PekkoFlow, Sink => PekkoSink, RunnableGraph => PekkoRunnableGraph}
import org.apache.pekko.{NotUsed => PekkoNotUsed}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class PekkoStreamingRuntime extends StreamingRuntime {
  type ActorSystem = pekko.ActorSystem
  type Source[+Out, +Mat] = PekkoSource[Out, Mat]
  type Flow[-In, +Out, +Mat] = PekkoFlow[In, Out, Mat]
  type Sink[-In, +Mat] = PekkoSink[In, Mat]
  type RunnableGraph[+Mat] = PekkoRunnableGraph[Mat]
  type NotUsed = PekkoNotUsed
  
  def createActorSystem(name: String): ActorSystem = 
    pekko.ActorSystem(name)
  
  def terminateActorSystem(system: ActorSystem): Future[Unit] = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    system.terminate().map(_ => ())
  }

  def emptySource[T]: Source[T, NotUsed] = 
    PekkoSource.empty[T]
  
  def fromIterator[T](iterator: () => Iterator[T]): Source[T, NotUsed] = 
    PekkoSource.fromIterator(iterator)
  
  def tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): Source[T, Any] = 
    PekkoSource.tick(initialDelay, interval, tick)

  def mapFlow[In, Out](f: In => Out): Flow[In, Out, NotUsed] = 
    PekkoFlow[In].map(f)
    
  def filterFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed] = 
    PekkoFlow[T].filter(predicate)
    
  def dropWhileFlow[T](predicate: T => Boolean): Flow[T, T, NotUsed] = 
    PekkoFlow[T].dropWhile(predicate)

  def foreachSink[T](f: T => Unit): Sink[T, Future[Unit]] = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    PekkoSink.foreach(f).mapMaterializedValue(_.map(_ => ()))
  }
    
  def seqSink[T]: Sink[T, Future[Seq[T]]] = 
    PekkoSink.seq[T]

  def runGraph[Mat](source: Source[Any, Any], sink: Sink[Any, Mat])(implicit system: ActorSystem): Mat = 
    source.runWith(sink)
    
  // Composition methods
  def via[In, Out, Mat1, Mat2](source: Source[In, Mat1], flow: Flow[In, Out, Mat2]): Source[Out, Mat1] = 
    source.via(flow)
    
  def to[In, Mat1, Mat2](source: Source[In, Mat1], sink: Sink[In, Mat2]): RunnableGraph[Mat1] = 
    source.to(sink)
    
  def runWith[In, Mat1, Mat2](source: Source[In, Mat1], sink: Sink[In, Mat2])(implicit system: ActorSystem): Mat2 = 
    source.runWith(sink)
    
  def dropWhile[T, Mat](source: Source[T, Mat], predicate: T => Boolean): Source[T, Mat] = 
    source.dropWhile(predicate)
}
```


presentation compiler configuration:
Scala version: 2.13.16
Classpath:
<WORKSPACE>/.bloop/root/bloop-bsp-clients-classes/classes-Metals-XrWB9-DZR1iUTqmztRO4vA== [exists ], <HOME>/Library/Caches/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.0/semanticdb-javac-0.11.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.16/scala-library-2.13.16.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-stream_2.13/2.8.5/akka-stream_2.13-2.8.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-http_2.13/10.5.3/akka-http_2.13-10.5.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-http-spray-json_2.13/10.5.3/akka-http-spray-json_2.13-10.5.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-core_2.13/0.14.7/circe-core_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-generic_2.13/0.14.7/circe-generic_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-parser_2.13/0.14.7/circe-parser_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/ch/qos/logback/logback-classic/1.5.6/logback-classic-1.5.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/scala-logging/scala-logging_2.13/3.9.5/scala-logging_2.13-3.9.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-stream-kafka_2.13/4.0.2/akka-stream-kafka_2.13-4.0.2.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/kafka/kafka-clients/3.5.1/kafka-clients-3.5.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/github/Ma27/rediscala_2.13/1.9.1/rediscala_2.13-1.9.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-stream_2.13/1.0.1/pekko-stream_2.13-1.0.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-actor_2.13/2.8.5/akka-actor_2.13-2.8.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-protobuf-v3_2.13/2.8.5/akka-protobuf-v3_2.13-2.8.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/reactivestreams/reactive-streams/1.0.4/reactive-streams-1.0.4.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/ssl-config-core_2.13/0.6.1/ssl-config-core_2.13-0.6.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-http-core_2.13/10.5.3/akka-http-core_2.13-10.5.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/spray/spray-json_2.13/1.3.6/spray-json_2.13-1.3.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-numbers_2.13/0.14.7/circe-numbers_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/typelevel/cats-core_2.13/2.10.0/cats-core_2.13-2.10.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.10/shapeless_2.13-2.3.10.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/circe/circe-jawn_2.13/0.14.7/circe-jawn_2.13-0.14.7.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/ch/qos/logback/logback-core/1.5.6/logback-core-1.5.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.16/scala-reflect-2.13.16.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/github/luben/zstd-jni/1.5.5-1/zstd-jni-1.5.5-1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/lz4/lz4-java/1.8.0/lz4-java-1.8.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/xerial/snappy/snappy-java/1.1.10.1/snappy-java-1.1.10.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-stm/scala-stm_2.13/0.9.1/scala-stm_2.13-0.9.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-actor_2.13/1.0.1/pekko-actor_2.13-1.0.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-protobuf-v3_2.13/1.0.1/pekko-protobuf-v3_2.13-1.0.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/config/1.4.2/config-1.4.2.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-java8-compat_2.13/1.0.0/scala-java8-compat_2.13-1.0.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/akka/akka-parsing_2.13/10.5.3/akka-parsing_2.13-10.5.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/typelevel/cats-kernel_2.13/2.10.0/cats-kernel_2.13-2.10.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/typelevel/jawn-parser_2.13/1.5.1/jawn-parser_2.13-1.5.1.jar [exists ]
Options:
-Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.collection.immutable.Nil$.head(List.scala:663)
	scala.collection.immutable.Nil$.head(List.scala:662)
	scala.tools.nsc.typechecker.MethodSynthesis$ClassMethodSynthesis.$anonfun$forwardMethod$1(MethodSynthesis.scala:85)
	scala.tools.nsc.typechecker.MethodSynthesis$ClassMethodSynthesis.createMethod(MethodSynthesis.scala:51)
	scala.tools.nsc.typechecker.MethodSynthesis$ClassMethodSynthesis.forwardMethod(MethodSynthesis.scala:85)
	scala.tools.nsc.typechecker.SyntheticMethods.forwardToRuntime$1(SyntheticMethods.scala:97)
	scala.tools.nsc.typechecker.SyntheticMethods.chooseHashcode$1(SyntheticMethods.scala:323)
	scala.tools.nsc.typechecker.SyntheticMethods.$anonfun$addSyntheticMethods$28(SyntheticMethods.scala:332)
	scala.tools.nsc.typechecker.SyntheticMethods.$anonfun$addSyntheticMethods$38(SyntheticMethods.scala:389)
	scala.collection.Iterator$$anon$9.next(Iterator.scala:584)
	scala.collection.immutable.List.prependedAll(List.scala:156)
	scala.collection.immutable.List$.from(List.scala:685)
	scala.collection.immutable.List$.from(List.scala:682)
	scala.collection.IterableOps$WithFilter.map(Iterable.scala:900)
	scala.tools.nsc.typechecker.SyntheticMethods.impls$1(SyntheticMethods.scala:389)
	scala.tools.nsc.typechecker.SyntheticMethods.synthesize$1(SyntheticMethods.scala:405)
	scala.tools.nsc.typechecker.SyntheticMethods.$anonfun$addSyntheticMethods$49(SyntheticMethods.scala:440)
	scala.reflect.internal.Trees.deriveTemplate(Trees.scala:2039)
	scala.reflect.internal.Trees.deriveTemplate$(Trees.scala:2037)
	scala.reflect.internal.SymbolTable.deriveTemplate(SymbolTable.scala:28)
	scala.tools.nsc.typechecker.SyntheticMethods.addSyntheticMethods(SyntheticMethods.scala:443)
	scala.tools.nsc.typechecker.SyntheticMethods.addSyntheticMethods$(SyntheticMethods.scala:70)
	scala.meta.internal.pc.MetalsGlobal$MetalsInteractiveAnalyzer.addSyntheticMethods(MetalsGlobal.scala:93)
	scala.tools.nsc.typechecker.Typers$Typer.finishMethodSynthesis(Typers.scala:2036)
	scala.tools.nsc.typechecker.Typers$Typer.typedClassDef(Typers.scala:1972)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6251)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6344)
	scala.tools.nsc.typechecker.Typers$Typer.typedStat$1(Typers.scala:6422)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typedStats$10(Typers.scala:3547)
	scala.tools.nsc.typechecker.Typers$Typer.typedStats(Typers.scala:3547)
	scala.tools.nsc.typechecker.Typers$Typer.typedPackageDef$1(Typers.scala:5925)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6254)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6344)
	scala.tools.nsc.typechecker.Analyzer$typerFactory$TyperPhase.apply(Analyzer.scala:126)
	scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:483)
	scala.tools.nsc.interactive.Global$TyperRun.applyPhase(Global.scala:1370)
	scala.tools.nsc.interactive.Global$TyperRun.typeCheck(Global.scala:1363)
	scala.tools.nsc.interactive.Global.typeCheck(Global.scala:681)
	scala.meta.internal.pc.Compat.$anonfun$runOutline$1(Compat.scala:74)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterable.foreach(Iterable.scala:935)
	scala.meta.internal.pc.Compat.runOutline(Compat.scala:66)
	scala.meta.internal.pc.Compat.runOutline(Compat.scala:35)
	scala.meta.internal.pc.Compat.runOutline$(Compat.scala:33)
	scala.meta.internal.pc.MetalsGlobal.runOutline(MetalsGlobal.scala:41)
	scala.meta.internal.pc.ScalaCompilerWrapper.compiler(ScalaCompilerAccess.scala:18)
	scala.meta.internal.pc.ScalaCompilerWrapper.compiler(ScalaCompilerAccess.scala:13)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$semanticTokens$1(ScalaPresentationCompiler.scala:211)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:148)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withInterruptableCompiler$1(CompilerAccess.scala:92)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1095)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:619)
	java.base/java.lang.Thread.run(Thread.java:1447)
```
#### Short summary: 

java.util.NoSuchElementException: head of empty list