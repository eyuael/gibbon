package gibbon.operations.event_store

import scala.concurrent.{Future, ExecutionContext}
import io.circe.{Encoder, Decoder, Json}
import io.circe.syntax._
import java.time.Instant
import gibbon.operations.event_store.versioned.codecs._

/**
  * Package object for versioned event store functionality.
  * 
  * Provides convenient type aliases and utilities for working with
  * versioned event stores and their implementations.
  */
package object versioned {
  
  /**
    * Type alias for a versioned key-value pair
    */
  type VersionedEntry[K, V] = (K, VersionedValue[V])
  
  /**
    * Type alias for a versioned operation result
    */
  type VersionedResult[V] = Either[String, VersionedValue[V]]
  
  /**
    * Type alias for a delete operation result
    */
  type DeleteResult = Either[String, Unit]
  
  /**
    * Default configuration for versioned stores
    */
  val DefaultConfig: VersionedStoreConfig = VersionedStoreConfig(
    enableRecovery = true,
    maxOperationLogSize = 1000,
    concurrencyStrategy = OptimisticLocking
  )
  
  /**
    * Configuration for high-performance scenarios (no recovery)
    */
  val HighPerformanceConfig: VersionedStoreConfig = VersionedStoreConfig(
    enableRecovery = false,
    maxOperationLogSize = 0,
    concurrencyStrategy = NoConcurrencyControl
  )
  
  /**
    * Configuration for safe, consistent operations
    */
  val SafeConfig: VersionedStoreConfig = VersionedStoreConfig(
    enableRecovery = true,
    maxOperationLogSize = 5000,
    concurrencyStrategy = PessimisticLocking
  )
  
  /**
    * Implicits for working with versioned stores
    */
  object implicits {
    
    /**
      * Extension methods for VersionedEventStore
      */
    implicit class VersionedEventStoreOps[K: Encoder: Decoder, V: Encoder: Decoder](val store: VersionedEventStore[K, V]) {
      
      /**
        * Get a value with its version information
        */
      def getWithVersion(key: K)(implicit ec: ExecutionContext): Future[Option[VersionedValue[V]]] = 
        store.getVersioned(key)
      
      /**
        * Put a value and return the new version
        */
      def putAndGetVersion(key: K, value: V)(implicit ec: ExecutionContext): Future[Either[String, Long]] = 
        store.putVersioned(key, value).map(_.map(_.version))
      
      /**
        * Put a value with version checking
        */
      def putIfVersion(key: K, value: V, expectedVersion: Long)(implicit ec: ExecutionContext): Future[Boolean] = 
        store.putVersioned(key, value, Some(expectedVersion)).map(_.isRight)
      
      /**
        * Delete a value with version checking
        */
      def deleteIfVersion(key: K, expectedVersion: Long)(implicit ec: ExecutionContext): Future[Boolean] = 
        store.deleteVersioned(key, Some(expectedVersion)).map(_.isRight)
      
      /**
        * Get all entries with their versions
        */
      def getAllWithVersions(implicit ec: ExecutionContext): Future[Map[K, VersionedValue[V]]] = 
        store.getAllVersioned
      
      /**
        * Get the current version of a key
        */
      def getVersion(key: K)(implicit ec: ExecutionContext): Future[Option[Long]] = 
        store.getVersioned(key).map(_.map(_.version))
      
      /**
        * Check if a key exists at a specific version
        */
      def existsAtVersion(key: K, version: Long)(implicit ec: ExecutionContext): Future[Boolean] = 
        store.getVersioned(key).map(_.exists(_.version == version))
      
      /**
        * Get the history of a key (requires recovery to be enabled)
        */
      def getHistory(key: K)(implicit ec: ExecutionContext): Future[List[VersionedOperation]] = {
        if (!store.config.enableRecovery) {
          Future.successful(Nil)
        } else {
          store.replayOperations(0L, None).map { ops =>
            ops.filter(_.key == key.asJson.noSpaces)
          }
        }
      }
    }
    
    /**
      * Extension methods for VersionedValue
      */
    implicit class VersionedValueOps[V](val value: VersionedValue[V]) {
      
      /**
        * Check if this version is newer than another
        */
      def isNewerThan(other: VersionedValue[V]): Boolean = 
        value.version > other.version
      
      /**
        * Check if this version is older than another
        */
      def isOlderThan(other: VersionedValue[V]): Boolean = 
        value.version < other.version
      
      /**
        * Get the age of this version in milliseconds
        */
      def ageMillis: Long = 
        java.time.Duration.between(value.timestamp, Instant.now()).toMillis
      
      /**
        * Convert to a human-readable string
        */
      def toHumanString: String = 
        s"Version ${value.version} (${value.timestamp})"
    }
  }
  
  /**
    * Utilities for working with versioned stores
    */
  object utils {
    
    /**
      * Create a versioned store from an existing event store
      * Note: This creates a new versioned store, it doesn't wrap an existing non-versioned store
      */
    def versionedFrom[K: Encoder: Decoder, V: Encoder: Decoder](
      baseStore: gibbon.operations.event_store.EventStore[K, V],
      config: VersionedStoreConfig = DefaultConfig
    )(implicit ec: ExecutionContext): VersionedEventStore[K, V] = {
      // For now, create a new in-memory versioned store
      // In a real implementation, this would need to migrate data from the base store
      new InMemoryVersionedEventStore[K, V](config)
    }
    
    /**
      * Create a new in-memory versioned store
      */
    def inMemory[K: Encoder: Decoder, V: Encoder: Decoder](
      config: VersionedStoreConfig = DefaultConfig
    )(implicit ec: ExecutionContext): VersionedEventStore[K, V] = {
      new InMemoryVersionedEventStore[K, V](config)
    }
    
    /**
      * Create a new Redis-backed versioned store
      */
    def redis[K: Encoder: Decoder, V: Encoder: Decoder](
      connectionString: String,
      config: VersionedStoreConfig = DefaultConfig
    )(implicit ec: ExecutionContext): VersionedEventStore[K, V] = {
      // Create a new Redis-backed versioned store
      // The VersionedRedisEventStore creates its own internal RedisEventStore[String, String]
      new VersionedRedisEventStore[K, V](
        new gibbon.operations.event_store.RedisEventStore[String, String](connectionString),
        config
      )
    }
    
    /**
      * Create a new PostgreSQL-backed versioned store
      */
    def postgres[K: Encoder: Decoder, V: Encoder: Decoder](
      jdbcUrl: String,
      username: String,
      password: String,
      tableName: String = "versioned_events",
      config: VersionedStoreConfig = DefaultConfig
    )(implicit ec: ExecutionContext): VersionedEventStore[K, V] = {
      // Create a new PostgreSQL-backed versioned store
      // The VersionedPostgresEventStore creates its own internal PostgresEventStore[String, String]
      new VersionedPostgresEventStore[K, V](
        new gibbon.operations.event_store.PostgresEventStore[String, String](jdbcUrl, username, password, tableName),
        config
      )
    }
    
    /**
      * Compare two versioned values
      */
    def compareVersions[V](v1: VersionedValue[V], v2: VersionedValue[V]): Int = 
      java.lang.Long.compare(v1.version, v2.version)
    
    /**
      * Find the latest version among a list of versioned values
      */
    def latestVersion[V](values: List[VersionedValue[V]]): Option[VersionedValue[V]] = 
      values.reduceOption((a, b) => if (a.version > b.version) a else b)
    
    /**
      * Filter values by version range
      */
    def filterByVersionRange[V](
      values: List[VersionedValue[V]], 
      fromVersion: Long, 
      toVersion: Option[Long] = None
    ): List[VersionedValue[V]] = {
      val maxVersion = toVersion.getOrElse(Long.MaxValue)
      values.filter(v => v.version >= fromVersion && v.version <= maxVersion)
    }
  }
}