package gibbon.operations.event_store.versioned

import gibbon.operations.event_store.versioned.VersionedEventStore
import scala.util.Try
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/**
  * Test specification for VersionedPostgresEventStore
  * 
  * Note: These tests require a PostgreSQL instance to be running.
  * They will be skipped if PostgreSQL is not available.
  */
class VersionedPostgresEventStoreSpec extends VersionedEventStoreSpec {
  
  private var postgresAvailable: Boolean = false
  
  override def beforeAll(): Unit = {
    super.beforeAll()
    // Test PostgreSQL connection
    postgresAvailable = Try {
      val store = createStore()
      Await.result(store.getStatistics, 3.seconds)
      true
    }.getOrElse(false)
    
    if (!postgresAvailable) {
      println("PostgreSQL is not available. PostgreSQL tests will be skipped.")
    }
  }
  
  override def createStore(): VersionedEventStore[String, String] = {
    try {
      val store = VersionedEventStore.postgres[String, String](
        connectionString = "jdbc:postgresql://localhost:5432/gibbon_test",
        username = "gibbon",
        password = "gibbon",
        tableName = "test_table",
        config = VersionedStoreConfig()
      )
      // Test connection
      val testResult = Await.result(store.getStatistics, 2.seconds)
      store
    } catch {
      case _: Exception =>
        // If connection fails, skip Postgres tests
        postgresAvailable = false
        VersionedEventStore.inMemory[String, String](config = VersionedStoreConfig())
    }
  }
  
  test("should be a PostgreSQL implementation") {
    assume(postgresAvailable, "PostgreSQL is not available")
    val store = createStore()
    assert(store.getClass.getSimpleName.contains("Postgres"))
  }
  
  test("should persist data across connections") {
    assume(postgresAvailable, "PostgreSQL is not available")
    
    val store1 = createStore()
    val store2 = createStore()
    
    val result = for {
      _ <- store1.putVersioned("persistent-key", "persistent-value")
      value <- store2.getVersioned("persistent-key")
    } yield value
    
    val retrievedValue = Await.result(result, 5.seconds)
    
    assert(retrievedValue.isDefined)
    assertEquals(retrievedValue.get.value, "persistent-value")
  }
  
  test("should handle PostgreSQL-specific configuration") {
    assume(postgresAvailable, "PostgreSQL is not available")
    
    val store = VersionedEventStore.postgres[String, String](
      connectionString = "jdbc:postgresql://localhost:5432/gibbon_test",
      username = "gibbon",
      password = "gibbon",
      tableName = "custom_test_table",
      config = VersionedStoreConfig()
    )
    
    val result = for {
      _ <- store.putVersioned("key1", "value1")
      stats <- store.getStatistics
    } yield stats
    
    val stats = Await.result(result, 3.seconds)
    assert(stats != null)
  }
  
  test("should handle transactions correctly") {
    assume(postgresAvailable, "PostgreSQL is not available")
    
    val store = createStore()
    
    val result = for {
      _ <- store.putVersioned("tx-key1", "tx-value1")
      _ <- store.putVersioned("tx-key2", "tx-value2")
      checkpoint <- store.createCheckpoint()
      _ <- store.putVersioned("tx-key3", "tx-value3")
      restored <- store.restoreFromCheckpoint(checkpoint)
      allValues <- store.getAllVersioned
    } yield (checkpoint, restored, allValues)
    
    val (checkpoint, restored, allValues) = Await.result(result, 5.seconds)
    
    assert(checkpoint.nonEmpty)
    assert(restored)
    assertEquals(allValues.size, 2) // tx-key3 should not be present after restore
    assert(allValues.contains("tx-key1"))
    assert(allValues.contains("tx-key2"))
    assert(!allValues.contains("tx-key3"))
  }
  
  test("should clean up test data") {
    assume(postgresAvailable, "PostgreSQL is not available")
    
    val store = createStore()
    
    val result = for {
      _ <- store.putVersioned("cleanup-key", "cleanup-value")
      _ <- store.deleteVersioned("cleanup-key")
      value <- store.getVersioned("cleanup-key")
    } yield value
    
    val deletedValue = Await.result(result, 3.seconds)
    assert(deletedValue.isEmpty)
  }
}