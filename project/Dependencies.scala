import sbt._

object Dependencies {
  // Testing
  lazy val munit = "org.scalameta" %% "munit" % "0.7.29"
  
  // Akka
  lazy val akkaVersion = "2.8.5"
  lazy val akkaStreams = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.5.3"
  lazy val akkaHttpSpray = "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.3"
  
  // JSON
  lazy val circeVersion = "0.14.7"
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  
  // Logging
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.5.6"
  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"

  // Redis - using the newer maintained fork
  lazy val rediscala = "io.github.rediscala" %% "rediscala" % "1.13.0"

  // Kafka (optional dependencies)
  lazy val akkaStreamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.2"
  lazy val kafkaClients = "org.apache.kafka" % "kafka-clients" % "3.5.1"
}
