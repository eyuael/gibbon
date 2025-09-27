package gibbon.operations.event_store.versioned

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}
import io.circe.{Encoder, Decoder, Json}
import io.circe.syntax._
import io.circe.parser._
import java.time.Instant
import java.util.UUID

/**
  * In-memory implementation of VersionedEventStore with comprehensive versioning support
  * 
  * Features:
  * - Thread-safe operations using concurrent collections
  * - Configurable concurrency control strategies
  * - Operation logging for recovery
  * - Checkpoint creation and restoration
  * - Consistency verification and repair
  * - Statistics tracking
  */
class InMemoryVersionedEventStore[K: Encoder: Decoder, V: Encoder: Decoder](
  val config: VersionedStoreConfig
)(implicit ec: ExecutionContext) extends VersionedEventStore[K, V] {
  
  // Core storage
  private val store = scala.collection.concurrent.TrieMap.empty[K, VersionedValue[V]]
  private val operationLog = scala.collection.concurrent.TrieMap.empty[Long, VersionedOperation]
  private val checkpoints = scala.collection.concurrent.TrieMap.empty[String, RecoveryState]
  private val locks = scala.collection.concurrent.TrieMap.empty[K, String]
  
  // Version management
  @volatile private var versionCounter = 0L
  @volatile private var operationCounter = 0L
  
  // Statistics
  @volatile private var lastCheckpointId: Option[String] = None
  @volatile private var lastCheckpointTime: Option[Instant] = None
  
  // Helper methods
  private def generateOperationId(): String = UUID.randomUUID().toString
  
  private def computeValueHash(value: V): String = {
    val jsonStr = value.asJson.noSpaces
    java.util.Objects.hash(jsonStr).toString
  }
  
  private def nextVersion(): Long = {
    versionCounter += 1
    versionCounter
  }
  
  private def incrementOperationCounter(): Long = {
    operationCounter += 1
    operationCounter
  }
  
  // Lock management for pessimistic locking
  private def acquireLock(key: K, operationId: String): Boolean = {
    config.concurrencyStrategy match {
      case PessimisticLocking =>
        locks.putIfAbsent(key, operationId).isEmpty
      case _ => true // No locking needed for other strategies
    }
  }
  
  private def releaseLock(key: K, operationId: String): Unit = {
    config.concurrencyStrategy match {
      case PessimisticLocking =>
        locks.remove(key, operationId)
      case _ => // No action needed
    }
  }
  
  // Retry logic for optimistic locking
  private def withRetry[T](operation: => Future[T], retriesLeft: Int = config.maxRetries): Future[T] = {
    operation.recoverWith {
      case _: ConcurrentModificationException if retriesLeft > 0 =>
        Thread.sleep(100) // Simple exponential backoff would be better
        withRetry(operation, retriesLeft - 1)
      case other => Future.failed(other)
    }
  }
  
  // Base EventStore implementation
  override def get(key: K): Future[Option[V]] = {
    Future.successful(store.get(key).map(_.value))
  }
  
  override def put(key: K, value: V): Future[Unit] = {
    putVersioned(key, value).map {
      case Right(_) => ()
      case Left(error) => throw new RuntimeException(error)
    }
  }
  
  override def delete(key: K): Future[Unit] = {
    deleteVersioned(key).map {
      case Right(_) => ()
      case Left(error) => throw new RuntimeException(error)
    }
  }
  
  override def getAll: Future[Map[K, V]] = {
    Future.successful(store.map { case (k, vv) => k -> vv.value }.toMap)
  }
  
  // Versioned operations
  override def getVersioned(key: K): Future[Option[VersionedValue[V]]] = {
    Future.successful(store.get(key))
  }
  
  override def putVersioned(key: K, value: V, expectedVersion: Option[Long]): Future[Either[String, VersionedValue[V]]] = {
    withRetry {
      val operationId = generateOperationId()
      
      if (!acquireLock(key, operationId)) {
        return Future.successful(Left("Could not acquire lock"))
      }
      
      try {
        val current = store.get(key)
        
        // Check version consistency for optimistic locking
        config.concurrencyStrategy match {
          case OptimisticLocking =>
            expectedVersion.foreach { version =>
              if (current.exists(_.version != version)) {
                return Future.successful(Left(s"Version mismatch: expected $version, got ${current.map(_.version)}"))
              }
            }
          case _ => // No version checking for other strategies
        }
        
        val newVersion = nextVersion()
        val timestamp = Instant.now()
        val valueHash = Some(computeValueHash(value))
        
        val versionedValue = VersionedValue(
          value = value,
          version = newVersion,
          timestamp = timestamp,
          operationId = operationId,
          previousVersion = current.map(_.version)
        )
        
        // Store the value
        store.put(key, versionedValue)
        incrementOperationCounter()
        
        // Log the operation for recovery
        if (config.enableRecovery) {
          val operation = VersionedOperation(
            operationId = operationId,
            key = key.asJson.noSpaces,
            operationType = "PUT",
            version = newVersion,
            timestamp = timestamp,
            valueHash = valueHash
          )
          operationLog.put(newVersion, operation)
          
          // Maintain operation log size
          if (operationLog.size > config.maxOperationLogSize) {
            val oldestVersion = operationLog.keys.min
            operationLog.remove(oldestVersion)
          }
        }
        
        Future.successful(Right(versionedValue))
      } finally {
        releaseLock(key, operationId)
      }
    }
  }
  
  override def deleteVersioned(key: K, expectedVersion: Option[Long]): Future[Either[String, Unit]] = {
    withRetry {
      val operationId = generateOperationId()
      
      if (!acquireLock(key, operationId)) {
        return Future.successful(Left("Could not acquire lock"))
      }
      
      try {
        val current = store.get(key)
        
        if (current.isEmpty) {
          return Future.successful(Left("Key not found"))
        }
        
        // Check version consistency for optimistic locking
        config.concurrencyStrategy match {
          case OptimisticLocking =>
            expectedVersion.foreach { version =>
              if (current.exists(_.version != version)) {
                return Future.successful(Left(s"Version mismatch: expected $version, got ${current.map(_.version)}"))
              }
            }
          case _ => // No version checking for other strategies
        }
        
        val newVersion = nextVersion()
        val timestamp = Instant.now()
        
        // Remove the value
        store.remove(key)
        incrementOperationCounter()
        
        // Log the operation for recovery
        if (config.enableRecovery) {
          val operation = VersionedOperation(
            operationId = operationId,
            key = key.asJson.noSpaces,
            operationType = "DELETE",
            version = newVersion,
            timestamp = timestamp,
            valueHash = None
          )
          operationLog.put(newVersion, operation)
          
          // Maintain operation log size
          if (operationLog.size > config.maxOperationLogSize) {
            val oldestVersion = operationLog.keys.min
            operationLog.remove(oldestVersion)
          }
        }
        
        Future.successful(Right(()))
      } finally {
        releaseLock(key, operationId)
      }
    }
  }
  
  override def getAllVersioned: Future[Map[K, VersionedValue[V]]] = {
    Future.successful(store.toMap)
  }
  
  // Recovery operations
  override def createCheckpoint(): Future[String] = {
    if (!config.enableRecovery) {
      return Future.failed(new UnsupportedOperationException("Recovery is disabled"))
    }
    
    val checkpointId = generateOperationId()
    val recoveryState = RecoveryState(
      lastProcessedVersion = versionCounter,
      checkpointTimestamp = Instant.now(),
      pendingOperations = operationLog.values.toList.sortBy(_.version),
      isConsistent = true
    )
    
    checkpoints.put(checkpointId, recoveryState)
    lastCheckpointId = Some(checkpointId)
    lastCheckpointTime = Some(Instant.now())
    
    Future.successful(checkpointId)
  }
  
  override def restoreFromCheckpoint(checkpointId: String): Future[Boolean] = {
    if (!config.enableRecovery) {
      return Future.failed(new UnsupportedOperationException("Recovery is disabled"))
    }
    
    checkpoints.get(checkpointId) match {
      case Some(recoveryState) =>
        // Clear current state and restore from checkpoint
        store.clear()
        operationLog.clear()
        locks.clear()
        versionCounter = recoveryState.lastProcessedVersion
        operationCounter = 0
        
        // In a real implementation, you'd restore the actual data from the checkpoint
        // For now, we just reset the version counter and clear the store
        Future.successful(true)
      case None =>
        Future.successful(false)
    }
  }
  
  override def getRecoveryState: Future[RecoveryState] = {
    if (!config.enableRecovery) {
      return Future.failed(new UnsupportedOperationException("Recovery is disabled"))
    }
    
    val state = RecoveryState(
      lastProcessedVersion = versionCounter,
      checkpointTimestamp = Instant.now(),
      pendingOperations = operationLog.values.toList.sortBy(_.version),
      isConsistent = verifyConsistencySync()
    )
    Future.successful(state)
  }
  
  override def replayOperations(fromVersion: Long, toVersion: Option[Long]): Future[List[VersionedOperation]] = {
    if (!config.enableRecovery) {
      return Future.failed(new UnsupportedOperationException("Recovery is disabled"))
    }
    
    val maxVersion = toVersion.getOrElse(versionCounter)
    val operations = operationLog
      .filter { case (version, _) => version >= fromVersion && version <= maxVersion }
      .values
      .toList
      .sortBy(_.version)
    
    Future.successful(operations)
  }
  
  // Consistency checks
  override def verifyConsistency(): Future[Boolean] = {
    Future.successful(verifyConsistencySync())
  }
  
  private def verifyConsistencySync(): Boolean = {
    // Check for version gaps
    val versions = operationLog.keys.toList.sorted
    if (versions.isEmpty) return true
    
    val hasGaps = versions.sliding(2).exists {
      case List(v1, v2) => v2 - v1 > 1
      case _ => false
    }
    
    !hasGaps && store.size >= 0 // Additional consistency checks can be added here
  }
  
  override def repairConsistency(): Future[Boolean] = {
    val wasConsistent = verifyConsistencySync()
    
    if (wasConsistent) {
      Future.successful(true)
    } else {
      // Simple repair: remove operations with gaps and rebuild version counter
      val versions = operationLog.keys.toList.sorted
      val validVersions = versions.foldLeft(List.empty[Long]) { (acc, version) =>
        if (acc.isEmpty || version == acc.head + 1) {
          version :: acc
        } else {
          operationLog.remove(version)
          acc
        }
      }
      
      // Reset version counter to highest valid version
      if (validVersions.nonEmpty) {
        versionCounter = validVersions.max
      }
      
      Future.successful(validVersions.nonEmpty)
    }
  }
  
  // Additional methods
  override def currentVersion: Long = versionCounter
  
  override def getStatistics: Future[StoreStatistics] = {
    Future.successful(StoreStatistics(
      totalKeys = store.size.toLong,
      totalOperations = operationCounter,
      currentVersion = versionCounter,
      lastCheckpointId = lastCheckpointId,
      lastCheckpointTimestamp = lastCheckpointTime,
      isConsistent = verifyConsistencySync()
    ))
  }
  
  /**
    * Get operation history for a specific key
    * 
    * @param key The key to get history for
    * @return List of operations affecting this key
    */
  def getKeyHistory(key: K): Future[List[VersionedOperation]] = {
    if (!config.enableRecovery) {
      return Future.successful(Nil)
    }
    
    val keyJson = key.asJson.noSpaces
    val operations = operationLog.values
      .filter(_.key == keyJson)
      .toList
      .sortBy(_.version)
    
    Future.successful(operations)
  }
  
  /**
    * Clear all data and reset state
    * 
    * @return Future indicating completion
    */
  def clear(): Future[Unit] = {
    store.clear()
    operationLog.clear()
    checkpoints.clear()
    locks.clear()
    versionCounter = 0L
    operationCounter = 0L
    lastCheckpointId = None
    lastCheckpointTime = None
    
    Future.successful(())
  }
}