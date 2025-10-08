import Dependencies._

ThisBuild / scalaVersion     := "2.13.16"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "io.github.eyuael"
ThisBuild / organizationName := "Eyuael Berhe"

// Maven Central publishing configuration
ThisBuild / homepage := Some(url("https://github.com/eyuaelberhe/gibbon"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/eyuaelberhe/gibbon"),
    "scm:git@github.com:eyuaelberhe/gibbon.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "eyuaelberhe",
    name  = "Eyuael Berhe",
    email = "eyuael.berhe@gmail.com", // Replace with your actual email
    url   = url("https://github.com/eyuaelberhe")
  )
)

ThisBuild / description := "A Scala library for event sourcing and CQRS patterns"
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / versionScheme := Some("early-semver")

// Publishing settings - publishTo is configured in sonatype.sbt
ThisBuild / publishMavenStyle := true
ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

// Don't publish the root project
publish / skip := true

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
      
      // Database
      postgresql,
      
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
  .dependsOn(core, akka % "compile->compile;test->test", pekko % "compile->compile;test->test")
  .settings(
    name := "gibbon"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
