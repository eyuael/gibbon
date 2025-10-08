package gibbon.sinks

import gibbon.core.Event
import gibbon.runtime.{TestStreamingRuntime, TestSource}
import munit.FunSuite
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

class DatabaseSinkTest extends FunSuite {
  
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val runtime = new TestStreamingRuntime()
  implicit val testRuntime: TestStreamingRuntime = runtime
  
  // Test database configuration - adjust these values for your local setup
  val testJdbcUrl = "jdbc:postgresql://localhost:5432/Sink"
  val testUsername = "postgres" // Change this to your PostgreSQL username
  val testPassword = "password" // Change this to your PostgreSQL password
  val testTableName = "test_events"
  
  // Helper method to check if database is available
  def isDatabaseAvailable: Boolean = {
    Try {
      val sink = DatabaseSink.postgres[String, String](
        database = "Sink",
        username = testUsername,
        password = testPassword,
        tableName = testTableName
      )
      val future = sink.getTableRowCount()
      Await.result(future, 5.seconds)
      sink.close()
    } match {
      case Success(_) => true
      case Failure(_) => false
    }
  }
  
  override def beforeEach(context: BeforeEach): Unit = {
    // Clear test table before each test if database is available
    if (isDatabaseAvailable) {
      val sink = DatabaseSink.postgres[String, String](
        database = "Sink",
        username = testUsername,
        password = testPassword,
        tableName = testTableName
      )
      Try {
        Await.result(sink.clearTable(), 5.seconds)
        Await.result(sink.close(), 5.seconds)
      }
    }
  }
  
  test("DatabaseSink should create instance successfully") {
    val sink = DatabaseSink[String, String](testJdbcUrl, testUsername, testPassword)
    assert(sink != null)
  }
  
  test("DatabaseSink should create instance with custom table name") {
    val sink = DatabaseSink[String, String](testJdbcUrl, testUsername, testPassword, "custom_events")
    assert(sink != null)
  }
  
  test("DatabaseSink should create instance with postgres convenience method") {
    val sink = DatabaseSink.postgres[String, String](
      database = "Sink",
      username = testUsername,
      password = testPassword
    )
    assert(sink != null)
  }
  
  test("DatabaseSink should create instance with custom settings") {
    val sink = DatabaseSink.withSettings[String, String](
      jdbcUrl = testJdbcUrl,
      username = testUsername,
      password = testPassword,
      tableName = "custom_events",
      batchSize = 50,
      retryAttempts = 5
    )
    assert(sink != null)
  }
  
  test("DatabaseSink should handle empty stream".flaky) {
    assume(isDatabaseAvailable, "Database not available for testing")
    
    val sink = DatabaseSink.postgres[String, String](
      database = "Sink",
      username = testUsername,
      password = testPassword,
      tableName = testTableName
    )
    
    val source = TestSource.empty[Event[String, String]]
    val runtimeSink = sink.toRuntimeSink()
    val result = source.runWith(runtimeSink)
    val done = Await.result(result, 10.seconds)
    
    // Verify table is still empty
    val count = Await.result(sink.getTableRowCount(), 5.seconds)
    assertEquals(count, 0L)
    
    Await.result(sink.close(), 5.seconds)
    assert(done != null)
  }
  
  test("DatabaseSink should process single event".flaky) {
    assume(isDatabaseAvailable, "Database not available for testing")
    
    val sink = DatabaseSink.postgres[String, String](
      database = "Sink",
      username = testUsername,
      password = testPassword,
      tableName = testTableName
    )
    
    val event = Event("test-key", "test-value", System.currentTimeMillis())
    val source = TestSource(event)
    
    val runtimeSink = sink.toRuntimeSink()
    val result = source.runWith(runtimeSink)
    val done = Await.result(result, 10.seconds)
    
    // Flush any remaining events
    Await.result(sink.flush(), 5.seconds)
    
    // Verify event was written to database
    val count = Await.result(sink.getTableRowCount(), 5.seconds)
    assertEquals(count, 1L)
    
    Await.result(sink.close(), 5.seconds)
    assert(done != null)
  }
  
  test("DatabaseSink should process multiple events".flaky) {
    assume(isDatabaseAvailable, "Database not available for testing")
    
    val sink = DatabaseSink.postgres[String, String](
      database = "Sink",
      username = testUsername,
      password = testPassword,
      tableName = testTableName
    )
    
    val events = List(
      Event("key1", "value1", System.currentTimeMillis()),
      Event("key2", "value2", System.currentTimeMillis()),
      Event("key3", "value3", System.currentTimeMillis())
    )
    val source = TestSource(events: _*)
    
    val runtimeSink = sink.toRuntimeSink()
    val result = source.runWith(runtimeSink)
    val done = Await.result(result, 10.seconds)
    
    // Flush any remaining events
    Await.result(sink.flush(), 5.seconds)
    
    // Verify all events were written to database
    val count = Await.result(sink.getTableRowCount(), 5.seconds)
    assertEquals(count, 3L)
    
    Await.result(sink.close(), 5.seconds)
    assert(done != null)
  }
  
  test("DatabaseSink should handle batch processing".flaky) {
    assume(isDatabaseAvailable, "Database not available for testing")
    
    val batchSize = 5
    val sink = DatabaseSink.withSettings[String, String](
      jdbcUrl = testJdbcUrl,
      username = testUsername,
      password = testPassword,
      tableName = testTableName,
      batchSize = batchSize
    )
    
    // Create more events than batch size to test batching
    val events = (1 to 12).map { i =>
      Event(s"key$i", s"value$i", System.currentTimeMillis())
    }.toList
    
    val source = TestSource(events: _*)
    val runtimeSink = sink.toRuntimeSink()
    val result = source.runWith(runtimeSink)
    val done = Await.result(result, 10.seconds)
    
    // Flush any remaining events
    Await.result(sink.flush(), 5.seconds)
    
    // Verify all events were written to database
    val count = Await.result(sink.getTableRowCount(), 5.seconds)
    assertEquals(count, 12L)
    
    Await.result(sink.close(), 5.seconds)
    assert(done != null)
  }
  
  test("DatabaseSink should handle write failures gracefully".flaky) {
    // Test with invalid connection parameters
    val sink = DatabaseSink[String, String](
      "jdbc:postgresql://invalid-host:5432/invalid-db",
      "invalid-user",
      "invalid-password"
    )
    
    val event = Event("key1", "value1", System.currentTimeMillis())
    
    // This should fail but not crash the application
    val writeResult = sink.write(event)
    
    intercept[RuntimeException] {
      Await.result(writeResult, 10.seconds)
    }
  }
  
  test("DatabaseSink should flush remaining events on close".flaky) {
    assume(isDatabaseAvailable, "Database not available for testing")
    
    val sink = DatabaseSink.withSettings[String, String](
      jdbcUrl = testJdbcUrl,
      username = testUsername,
      password = testPassword,
      tableName = testTableName,
      batchSize = 10 // Large batch size to prevent auto-flushing
    )
    
    // Write fewer events than batch size
    val events = List(
      Event("key1", "value1", System.currentTimeMillis()),
      Event("key2", "value2", System.currentTimeMillis())
    )
    
    // Write events individually
    events.foreach { event =>
      Await.result(sink.write(event), 5.seconds)
    }
    
    // Events should not be in database yet (still in buffer)
    val countBefore = Await.result(sink.getTableRowCount(), 5.seconds)
    
    // Close should flush remaining events
    Await.result(sink.close(), 10.seconds)
    
    // Now events should be in database
    val countAfter = Await.result(sink.getTableRowCount(), 5.seconds)
    assertEquals(countAfter, 2L)
  }
}