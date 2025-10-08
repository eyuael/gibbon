package examples

import gibbon.core.Event
import gibbon.sinks.DatabaseSink
import gibbon.runtime.TestStreamingRuntime
import gibbon.runtime.TestSource
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure, Random}

/**
 * Example application demonstrating DatabaseSink usage with PostgreSQL
 * 
 * Prerequisites:
 * 1. PostgreSQL server running on localhost:5432
 * 2. Database named 'Sink' created
 * 3. Run the database-setup.sql script to create the events table
 * 
 * To run this example:
 * 1. Update the database credentials below
 * 2. Compile: sbt compile
 * 3. Run: sbt "runMain examples.DatabaseSinkExample"
 */
object DatabaseSinkExample extends App {
  
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val runtime = new TestStreamingRuntime()
  
  // Database configuration - update these for your setup
  val dbHost = "localhost"
  val dbPort = 5432
  val dbName = "Sink"
  val dbUsername = "postgres" // Change to your PostgreSQL username
  val dbPassword = "password" // Change to your PostgreSQL password
  val tableName = "events"
  
  println("=== Gibbon DatabaseSink Example ===")
  println(s"Connecting to PostgreSQL at $dbHost:$dbPort/$dbName")
  
  // Create DatabaseSink instance
  val databaseSink = DatabaseSink.postgres[String, String](
    host = dbHost,
    port = dbPort,
    database = dbName,
    username = dbUsername,
    password = dbPassword,
    tableName = tableName
  )
  
  // Test database connection
  def testConnection(): Boolean = {
    Try {
      val count = Await.result(databaseSink.getTableRowCount(), 5.seconds)
      println(s"✓ Database connection successful. Current row count: $count")
      true
    } match {
      case Success(_) => true
      case Failure(ex) =>
        println(s"✗ Database connection failed: ${ex.getMessage}")
        println("Please ensure:")
        println("1. PostgreSQL is running on localhost:5432")
        println("2. Database 'Sink' exists")
        println("3. Username and password are correct")
        println("4. The events table exists (run database-setup.sql)")
        false
    }
  }
  
  // Generate sample events
  def generateSampleEvents(count: Int): List[Event[String, String]] = {
    val random = new Random()
    val eventTypes = List("user_login", "page_view", "purchase", "logout", "error")
    val users = List("user1", "user2", "user3", "user4", "user5")
    
    (1 to count).map { i =>
      val eventType = eventTypes(random.nextInt(eventTypes.length))
      val user = users(random.nextInt(users.length))
      val value = s"""{"user":"$user","action":"$eventType","session_id":"${random.nextInt(1000)}"}"""
      
      Event(
        key = s"$eventType-$i",
        value = value,
        eventTime = System.currentTimeMillis() - random.nextInt(3600000) // Random time within last hour
      )
    }.toList
  }
  
  // Example 1: Basic usage with single event
  def basicUsageExample(): Future[Unit] = {
    println("\n--- Example 1: Basic Usage ---")
    
    val event = Event(
      key = "welcome-event",
      value = """{"message":"Welcome to Gibbon DatabaseSink!","timestamp":"${System.currentTimeMillis()}"}""",
      eventTime = System.currentTimeMillis()
    )
    
    println(s"Writing single event: ${event.key}")
    
    for {
      _ <- databaseSink.write(event)
      _ <- databaseSink.flush() // Ensure event is written immediately
      count <- databaseSink.getTableRowCount()
    } yield {
      println(s"✓ Event written successfully. Total events in database: $count")
    }
  }
  
  // Example 2: Batch processing with multiple events
  def batchProcessingExample(): Future[Unit] = {
    println("\n--- Example 2: Batch Processing ---")
    
    val events = generateSampleEvents(25)
    println(s"Generated ${events.length} sample events")
    
    val source = TestSource(events: _*)
    val runtimeSink = databaseSink.toRuntimeSink()
    
    for {
      _ <- source.runWith(runtimeSink)
      _ <- databaseSink.flush()
      count <- databaseSink.getTableRowCount()
    } yield {
      println(s"✓ Batch processing completed. Total events in database: $count")
    }
  }
  
  // Example 3: High-throughput streaming
  def highThroughputExample(): Future[Unit] = {
    println("\n--- Example 3: High-Throughput Streaming ---")
    
    val eventCount = 1000
    val events = generateSampleEvents(eventCount)
    println(s"Streaming $eventCount events...")
    
    val startTime = System.currentTimeMillis()
    val source = TestSource(events: _*)
    val runtimeSink = databaseSink.toRuntimeSink()
    
    for {
      _ <- source.runWith(runtimeSink)
      _ <- databaseSink.flush()
      count <- databaseSink.getTableRowCount()
      endTime = System.currentTimeMillis()
      duration = endTime - startTime
    } yield {
      println(s"✓ Streamed $eventCount events in ${duration}ms")
      println(s"✓ Throughput: ${eventCount * 1000 / duration} events/second")
      println(s"✓ Total events in database: $count")
    }
  }
  
  // Example 4: Custom batch size configuration
  def customConfigExample(): Future[Unit] = {
    println("\n--- Example 4: Custom Configuration ---")
    
    val customSink = DatabaseSink.withSettings[String, String](
      jdbcUrl = s"jdbc:postgresql://$dbHost:$dbPort/$dbName",
      username = dbUsername,
      password = dbPassword,
      tableName = tableName,
      batchSize = 10, // Smaller batch size for demonstration
      retryAttempts = 5
    )
    
    val events = generateSampleEvents(35) // More than 3 batches
    println(s"Using custom sink with batch size 10 for ${events.length} events")
    
    val source = TestSource(events: _*)
    val runtimeSink = customSink.toRuntimeSink()
    
    for {
      _ <- source.runWith(runtimeSink)
      _ <- customSink.flush()
      count <- customSink.getTableRowCount()
      _ <- customSink.close()
    } yield {
      println(s"✓ Custom configuration example completed. Total events: $count")
    }
  }
  
  // Run all examples
  def runExamples(): Future[Unit] = {
    for {
      _ <- basicUsageExample()
      _ <- batchProcessingExample()
      _ <- highThroughputExample()
      _ <- customConfigExample()
    } yield {
      println("\n=== All Examples Completed Successfully! ===")
      println("\nTo verify the results, you can run these SQL queries:")
      println(s"SELECT COUNT(*) FROM $tableName;")
      println(s"SELECT * FROM $tableName ORDER BY created_at DESC LIMIT 10;")
    }
  }
  
  // Main execution
  if (testConnection()) {
    try {
      Await.result(runExamples(), 30.seconds)
    } catch {
      case ex: Exception =>
        println(s"Example execution failed: ${ex.getMessage}")
        ex.printStackTrace()
    } finally {
      // Clean up
      Try {
        Await.result(databaseSink.close(), 5.seconds)
        println("\n✓ Database connection closed")
      }
    }
  } else {
    println("Skipping examples due to database connection issues")
    System.exit(1)
  }
}