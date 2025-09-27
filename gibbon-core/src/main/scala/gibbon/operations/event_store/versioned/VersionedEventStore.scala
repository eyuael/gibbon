package gibbon.operations.event_store.versioned

import scala.concurrent.{Future, ExecutionContext}
import io.circe.{Encoder, Decoder}

/**
  * Versioned event store that extends the base EventStore with versioning capabilities
  * 
  * Features:
  * - Version tracking for all operations
  * - Recovery mechanisms with checkpoints and operation logs
  * - Concurrency control strategies
  * - Consistency verification and repair
  * - Statistics and monitoring
  * 
  * @tparam K Key type
  * @tparam V Value type
  */
trait VersionedEventStore[K, V] extends gibbon.operations.event_store.EventStore[K, V] {
  
  /**
    * Get a value with its version information
    * 
    * @param key The key to retrieve
    * @return The versioned value if found
    */
  def getVersioned(key: K): Future[Option[VersionedValue[V]]]
  
  /**
    * Store a value with versioning information
    * 
    * @param key The key to store
    * @param value The value to store
    * @param expectedVersion Optional expected version for optimistic locking
    * @return Either an error message or the new versioned value
    */
  def putVersioned(key: K, value: V, expectedVersion: Option[Long] = None): Future[Either[String, VersionedValue[V]]]
  
  /**
    * Delete a value with version checking
    * 
    * @param key The key to delete
    * @param expectedVersion Optional expected version for optimistic locking
    * @return Either an error message or success
    */
  def deleteVersioned(key: K, expectedVersion: Option[Long] = None): Future[Either[String, Unit]]
  
  /**
    * Get all key-value pairs with their version information
    * 
    * @return Map of all versioned entries
    */
  def getAllVersioned: Future[Map[K, VersionedValue[V]]]
  
  /**
    * Create a checkpoint for recovery purposes
    * 
    * @return ID of the created checkpoint
    */
  def createCheckpoint(): Future[String]
  
  /**
    * Restore state from a checkpoint
    * 
    * @param checkpointId The checkpoint to restore from
    * @return Whether restoration was successful
    */
  def restoreFromCheckpoint(checkpointId: String): Future[Boolean]
  
  /**
    * Get the current recovery state
    * 
    * @return Current recovery state
    */
  def getRecoveryState: Future[RecoveryState]
  
  /**
    * Replay operations from a version range
    * 
    * @param fromVersion Starting version (inclusive)
    * @param toVersion Optional ending version (inclusive), None for all
    * @return List of operations in the specified range
    */
  def replayOperations(fromVersion: Long, toVersion: Option[Long]): Future[List[VersionedOperation]]
  
  /**
    * Verify the consistency of the store
    * 
    * @return Whether the store is consistent
    */
  def verifyConsistency(): Future[Boolean]
  
  /**
    * Attempt to repair consistency issues
    * 
    * @return Whether repair was successful
    */
  def repairConsistency(): Future[Boolean]
  
  /**
    * Get the current version number
    * 
    * @return Current version
    */
  def currentVersion: Long
  
  /**
    * Get the store configuration
    * 
    * @return Store configuration
    */
  def config: VersionedStoreConfig
  
  /**
    * Get store statistics
    * 
    * @return Store statistics
    */
  def getStatistics: Future[StoreStatistics]
}

/**
  * Companion object with factory methods for creating versioned stores
  */
object VersionedEventStore {
  
  /**
    * Create an in-memory versioned event store
    * 
    * @param config Store configuration
    * @tparam K Key type
    * @tparam V Value type
    * @return New in-memory versioned event store
    */
  def inMemory[K: Encoder: Decoder, V: Encoder: Decoder](
    config: VersionedStoreConfig = VersionedStoreConfig()
  )(implicit ec: ExecutionContext): VersionedEventStore[K, V] = {
    new InMemoryVersionedEventStore[K, V](config)
  }
  
  /**
    * Create a Redis-backed versioned event store
    * 
    * @param connectionString Redis connection string
    * @param config Store configuration
    * @tparam K Key type
    * @tparam V Value type
    * @return New Redis-backed versioned event store
    */
  def redis[K: Encoder: Decoder, V: Encoder: Decoder](
    connectionString: String,
    config: VersionedStoreConfig = VersionedStoreConfig()
  )(implicit ec: ExecutionContext): VersionedEventStore[K, V] = {
    val redisStore = new gibbon.operations.event_store.RedisEventStore[String, String](connectionString)
    new VersionedRedisEventStore[K, V](redisStore, config)
  }
  
  /**
    * Create a PostgreSQL-backed versioned event store
    * 
    * @param connectionString PostgreSQL connection string
    * @param tableName Table name for storing data
    * @param config Store configuration
    * @tparam K Key type
    * @tparam V Value type
    * @return New PostgreSQL-backed versioned event store
    */
  def postgres[K: Encoder: Decoder, V: Encoder: Decoder](
    connectionString: String,
    username: String,
    password: String,
    tableName: String = "versioned_events",
    config: VersionedStoreConfig = VersionedStoreConfig()
  )(implicit ec: ExecutionContext): VersionedEventStore[K, V] = {
    val postgresStore = new gibbon.operations.event_store.PostgresEventStore[String, String](connectionString, username, password, tableName)
    new VersionedPostgresEventStore[K, V](postgresStore, config)
  }
}