error id: 834CD881EDFE86C819FE70D3261DF82B
file://<WORKSPACE>/build.sbt
### java.lang.AssertionError: assertion failed: (List(),0)

occurred in the presentation compiler.



action parameters:
offset: 1224
uri: file://<WORKSPACE>/build.sbt
text:
```scala
import _root_.scala.xml.{TopScope=>$scope}
import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbt.nio.Keys._
import _root_.sbt.ScriptedPlugin.autoImport._, _root_.sbt.plugins.JUnitXmlReportPlugin.autoImport._, _root_.sbt.plugins.MiniDependencyTreePlugin.autoImport._, _root_.bloop.integrations.sbt.BloopPlugin.autoImport._
import _root_.sbt.plugins.IvyPlugin, _root_.sbt.plugins.JvmPlugin, _root_.sbt.plugins.CorePlugin, _root_.sbt.ScriptedPlugin, _root_.sbt.plugins.SbtPlugin, _root_.sbt.plugins.SemanticdbPlugin, _root_.sbt.plugins.JUnitXmlReportPlugin, _root_.sbt.plugins.Giter8TemplatePlugin, _root_.sbt.plugins.MiniDependencyTreePlugin, _root_.bloop.integrations.sbt.BloopPlugin
import Dependencies._

ThisBuild / scalaVersion     := "2.13.16"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "gibbon",
    libraryDependencies ++= Seq(
      // Akka
      akkaStreams,
      akkaHttp,
      akkaHttpSpray,
      
      // JSON
      circeCore,
      circeGeneric,
      circeParser,
      
      // Logging
      logback,
      scalaLogging,
      
      // @@Kafka (optional - uncomment when needed)
      // "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.2",
      // "org.apache.kafka" % "kafka-clients" % "3.5.1",
      
      // Testing
      munit % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

```


presentation compiler configuration:
Scala version: 2.12.20
Classpath:
<WORKSPACE>/project/.bloop/gibbon-build/bloop-bsp-clients-classes/classes-Metals-jJ0NoIWQQkSq10iObyIIdA== [exists ], <HOME>/Library/Caches/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.0/semanticdb-javac-0.11.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/sbt/1.11.6/sbt-1.11.6.jar [exists ], <HOME>/.sbt/boot/scala-2.12.20/lib/scala-library.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/ch/epfl/scala/sbt-bloop_2.12_1.0/2.0.13/sbt-bloop_2.12_1.0-2.0.13.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/main_2.12/1.11.6/main_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/io_2.12/1.10.5/io_2.12-1.10.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/ch/epfl/scala/bloop-config_2.12/2.3.2/bloop-config_2.12-2.3.2.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/logic_2.12/1.11.6/logic_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/actions_2.12/1.11.6/actions_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/main-settings_2.12/1.11.6/main-settings_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/run_2.12/1.11.6/run_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/command_2.12/1.11.6/command_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/collections_2.12/1.11.6/collections_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/scripted-plugin_2.12/1.11.6/scripted-plugin_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-lm-integration_2.12/1.11.6/zinc-lm-integration_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/util-logging_2.12/1.11.6/util-logging_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.12/2.3.0/scala-xml_2.12-2.3.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/launcher-interface/1.5.0/launcher-interface-1.5.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/2.8.5/caffeine-2.8.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/lm-coursier-shaded_2.12/2.1.10/lm-coursier-shaded_2.12-2.1.10.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.17.1/log4j-api-2.17.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.17.1/log4j-core-2.17.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/apache/logging/log4j/log4j-slf4j-impl/2.17.1/log4j-slf4j-impl-2.17.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/librarymanagement-core_2.12/1.11.5/librarymanagement-core_2.12-1.11.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/librarymanagement-ivy_2.12/1.11.5/librarymanagement-ivy_2.12-1.11.5.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/compiler-interface/1.10.8/compiler-interface-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-compile_2.12/1.10.8/zinc-compile_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/swoval/file-tree-views/2.1.12/file-tree-views-2.1.12.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala/jsoniter-scala-core_2.12/2.13.5.2/jsoniter-scala-core_2.12-2.13.5.2.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/unroll-annotation_2.12/0.1.12/unroll-annotation_2.12-0.1.12.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/util-relation_2.12/1.11.6/util-relation_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/completion_2.12/1.11.6/completion_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/task-system_2.12/1.11.6/task-system_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/tasks_2.12/1.11.6/tasks_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/testing_2.12/1.11.6/testing_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/util-tracking_2.12/1.11.6/util-tracking_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/eed3si9n/sjson-new-core_2.12/0.10.1/sjson-new-core_2.12-0.10.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/eed3si9n/sjson-new-scalajson_2.12/0.10.1/sjson-new-scalajson_2.12-0.10.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/eed3si9n/gigahorse-apache-http_2.12/0.9.3/gigahorse-apache-http_2.12-0.9.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-terminal/3.27.1/jline-terminal-3.27.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-classpath_2.12/1.10.8/zinc-classpath_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-apiinfo_2.12/1.10.8/zinc-apiinfo_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc_2.12/1.10.8/zinc_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/core-macros_2.12/1.11.6/core-macros_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/util-cache_2.12/1.11.6/util-cache_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/util-control_2.12/1.11.6/util-control_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/protocol_2.12/1.11.6/protocol_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/template-resolver/0.1/template-resolver-0.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/util-position_2.12/1.11.6/util-position_2.12-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-compile-core_2.12/1.10.8/zinc-compile-core_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/util-interface/1.11.6/util-interface-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/jline/jline/2.14.7-sbt-9a88bc413e2b34a4580c001c654d1a7f4f65bf18/jline-2.14.7-sbt-9a88bc413e2b34a4580c001c654d1a7f4f65bf18.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-terminal-jni/3.27.1/jline-terminal-jni-3.27.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-native/3.27.1/jline-native-3.27.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/lmax/disruptor/3.4.2/disruptor-3.4.2.jar [exists ], <HOME>/.sbt/boot/scala-2.12.20/lib/scala-reflect.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/checkerframework/checker-qual/3.4.1/checker-qual-3.4.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.4.0/error_prone_annotations-2.4.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.12/2.13.0/scala-collection-compat_2.12-2.13.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar [exists ], <HOME>/.sbt/boot/scala-2.12.20/lib/scala-compiler.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/github/mwiede/jsch/0.2.23/jsch-0.2.23.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/ivy/ivy/2.3.0-sbt-77cc781d727b367d3761f097d89f5a4762771d41/ivy-2.3.0-sbt-77cc781d727b367d3761f097d89f5a4762771d41.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-reader/3.27.1/jline-reader-3.27.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-builtins/3.27.1/jline-builtins-3.27.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/test-agent/1.11.6/test-agent-1.11.6.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/test-interface/1.0/test-interface-1.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/eed3si9n/shaded-scalajson_2.12/1.0.0-M4/shaded-scalajson_2.12-1.0.0-M4.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/eed3si9n/shaded-jawn-parser_2.12/1.3.2/shaded-jawn-parser_2.12-1.3.2.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/eed3si9n/gigahorse-core_2.12/0.9.3/gigahorse-core_2.12-0.9.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/eed3si9n/shaded-apache-httpclient5/0.9.3/shaded-apache-httpclient5-0.9.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/compiler-bridge_2.12/1.10.8/compiler-bridge_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-classfile_2.12/1.10.8/zinc-classfile_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-core_2.12/1.10.8/zinc-core_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-persist_2.12/1.10.8/zinc-persist_2.12-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/eed3si9n/sjson-new-murmurhash_2.12/0.10.1/sjson-new-murmurhash_2.12-0.10.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/ipcsocket/ipcsocket/1.6.3/ipcsocket-1.6.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-parser-combinators_2.12/1.1.2/scala-parser-combinators_2.12-1.1.2.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/net/openhft/zero-allocation-hashing/0.16/zero-allocation-hashing-0.16.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/fusesource/jansi/jansi/2.4.1/jansi-2.4.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-style/3.27.1/jline-style-3.27.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/ssl-config-core_2.12/0.6.1/ssl-config-core_2.12-0.6.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/reactivestreams/reactive-streams/1.0.3/reactive-streams-1.0.3.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/zinc-persist-core-assembly/1.10.8/zinc-persist-core-assembly-1.10.8.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/sbinary_2.12/0.5.1/sbinary_2.12-0.5.1.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna/5.12.0/jna-5.12.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.12.0/jna-platform-5.12.0.jar [exists ], <HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/typesafe/config/1.4.2/config-1.4.2.jar [exists ]
Options:
-deprecation -Wconf:cat=unused-nowarn:s -Wconf:cat=unused-nowarn:s -Xsource:3 -Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.tools.nsc.classpath.FileBasedCache.getOrCreate(ZipAndJarFileLookupFactory.scala:284)
	scala.tools.nsc.plugins.Plugins.findPluginClassLoader(Plugins.scala:109)
	scala.tools.nsc.plugins.Plugins.findPluginClassLoader$(Plugins.scala:72)
	scala.tools.nsc.Global.findPluginClassLoader(Global.scala:44)
	scala.tools.nsc.plugins.Plugins.$anonfun$loadRoughPluginsList$6(Plugins.scala:47)
	scala.tools.nsc.plugins.Plugin$.$anonfun$loadAllFrom$1(Plugin.scala:148)
	scala.collection.immutable.List.map(List.scala:293)
	scala.tools.nsc.plugins.Plugins.loadRoughPluginsList(Plugins.scala:47)
	scala.tools.nsc.plugins.Plugins.loadRoughPluginsList$(Plugins.scala:40)
	scala.tools.nsc.Global.loadRoughPluginsList(Global.scala:44)
	scala.tools.nsc.plugins.Plugins.roughPluginsList(Plugins.scala:113)
	scala.tools.nsc.plugins.Plugins.roughPluginsList$(Plugins.scala:113)
	scala.tools.nsc.Global.roughPluginsList$lzycompute(Global.scala:44)
	scala.tools.nsc.Global.roughPluginsList(Global.scala:44)
	scala.tools.nsc.plugins.Plugins.loadPlugins(Plugins.scala:149)
	scala.tools.nsc.plugins.Plugins.loadPlugins$(Plugins.scala:119)
	scala.tools.nsc.Global.loadPlugins(Global.scala:44)
	scala.tools.nsc.plugins.Plugins.plugins(Plugins.scala:165)
	scala.tools.nsc.plugins.Plugins.plugins$(Plugins.scala:165)
	scala.tools.nsc.Global.plugins$lzycompute(Global.scala:44)
	scala.tools.nsc.Global.plugins(Global.scala:44)
	scala.tools.nsc.plugins.Plugins.computePluginPhases(Plugins.scala:176)
	scala.tools.nsc.plugins.Plugins.computePluginPhases$(Plugins.scala:175)
	scala.tools.nsc.Global.computePluginPhases(Global.scala:44)
	scala.tools.nsc.Global.computePhaseDescriptors(Global.scala:733)
	scala.tools.nsc.Global.phaseDescriptors$lzycompute(Global.scala:738)
	scala.tools.nsc.Global.phaseDescriptors(Global.scala:738)
	scala.tools.nsc.Global$Run.<init>(Global.scala:1247)
	scala.tools.nsc.interactive.Global$TyperRun.<init>(Global.scala:1323)
	scala.tools.nsc.interactive.Global.newTyperRun(Global.scala:1346)
	scala.tools.nsc.interactive.Global.<init>(Global.scala:294)
	scala.meta.internal.pc.MetalsGlobal.<init>(MetalsGlobal.scala:49)
	scala.meta.internal.pc.ScalaPresentationCompiler.newCompiler(ScalaPresentationCompiler.scala:627)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$compilerAccess$1(ScalaPresentationCompiler.scala:147)
	scala.meta.internal.pc.CompilerAccess.loadCompiler(CompilerAccess.scala:40)
	scala.meta.internal.pc.CompilerAccess.retryWithCleanCompiler(CompilerAccess.scala:182)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withSharedCompiler$1(CompilerAccess.scala:155)
	scala.Option.map(Option.scala:230)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:154)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withNonInterruptableCompiler$1(CompilerAccess.scala:132)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1095)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:619)
	java.base/java.lang.Thread.run(Thread.java:1447)
```
#### Short summary: 

java.lang.AssertionError: assertion failed: (List(),0)