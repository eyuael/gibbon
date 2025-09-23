import Dependencies._

ThisBuild / scalaVersion     := "2.13.16"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val core = (project in file("gibbon-core"))
  .settings(
    name := "gibbon-core",
    libraryDependencies ++= Seq(
      // JSON
      circeCore,
      circeGeneric,
      circeParser,
      
      // Logging
      logback,
      scalaLogging,
      
      // Testing
      munit % Test
    )
  )

lazy val akka = (project in file("gibbon-akka"))
  .dependsOn(core)
  .settings(
    name := "gibbon-akka",
    libraryDependencies ++= Seq(
      akkaStreams,
      akkaHttp,
      akkaHttpSpray,
      
      // Kafka (optional - uncomment when needed)
      "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.2",
      "org.apache.kafka" % "kafka-clients" % "3.5.1",

      //redis
      "com.github.Ma27" %% "rediscala" % "1.9.1",
      
      // Testing
      munit % Test
    )
  )

lazy val pekko = (project in file("gibbon-pekko"))
  .dependsOn(core)
  .settings(
    name := "gibbon-pekko",
    libraryDependencies ++= Seq(
      pekkoStreams,
      pekkoHttp,
      pekkoHttpSpray,
      
      // Kafka (optional - uncomment when needed)
      "org.apache.pekko" %% "pekko-connectors-kafka" % "1.0.0",
      "org.apache.kafka" % "kafka-clients" % "3.5.1",

      //redis
      "com.github.Ma27" %% "rediscala" % "1.9.1",
      
      // Testing
      munit % Test
    )
  )

lazy val root = (project in file("."))
  .aggregate(core, akka, pekko)
  .dependsOn(core, akka % "compile->compile;test->test")
  .settings(
    name := "gibbon"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
