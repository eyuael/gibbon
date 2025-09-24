error id: E5ACB55CFB9363F32AADA3B546BC9CC3
file://<WORKSPACE>/gibbon-core/src/main/scala/gibbon/error/ErrorHandler.scala
### java.lang.AssertionError: assertion failed: file://<WORKSPACE>/gibbon-core/src/main/scala/gibbon/error/ErrorHandler.scala: 345 >= 345

occurred in the presentation compiler.



action parameters:
offset: 345
uri: file://<WORKSPACE>/gibbon-core/src/main/scala/gibbon/error/ErrorHandler.scala
text:
```scala
package gibbon.error

sealed trait ErrorHandlingStrategy
case object Stop extends ErrorHandlingStrategy
case object Resume extends ErrorHandlingStrategy
case class Restart(maxRetries: Int) extends ErrorHandlingStrategy

trait ErrorHandler {
  def handleError(error: StreamError): ErrorHandlingStrategy
  def onError(error: StreamError): Unit
} 
@@
```


presentation compiler configuration:
Scala version: 2.13.16
Classpath:
<HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.16/scala-library-2.13.16.jar [exists ]
Options:





#### Error stacktrace:

```
scala.reflect.internal.util.SourceFile.position(SourceFile.scala:34)
	scala.tools.nsc.CompilationUnits$CompilationUnit.position(CompilationUnits.scala:136)
	scala.meta.internal.pc.PcDefinitionProvider.definition(PcDefinitionProvider.scala:68)
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

java.lang.AssertionError: assertion failed: file://<WORKSPACE>/gibbon-core/src/main/scala/gibbon/error/ErrorHandler.scala: 345 >= 345