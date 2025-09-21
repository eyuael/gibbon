error id: file://<WORKSPACE>/build.sbt:`<error>`#`<error>`.
file://<WORKSPACE>/build.sbt
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -Dependencies.r.
	 -Dependencies.r#
	 -Dependencies.r().
	 -r.
	 -r#
	 -r().
	 -scala/Predef.r.
	 -scala/Predef.r#
	 -scala/Predef.r().
offset: 760
uri: file://<WORKSPACE>/build.sbt
text:
```scala
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
      
      // Kafka (optional - uncomment when needed)
      "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.2",
      "org.apache.kafka" % "kafka-clients" % "3.5.1",

      //redis
      "com.github.etaty" %% "rediscala" % "1.9.0",
      r@@
      
      // Testing
      munit % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

```


#### Short summary: 

empty definition using pc, found symbol in pc: 