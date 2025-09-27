package gibbon.operations.event_store.versioned

import io.circe.{Encoder, Decoder, Json}
import java.time.Instant

/**
  * Core data models for the versioned event store system
  */

/**
  * A value stored with versioning information
  * 
  * @param value The actual value being stored
  * @param version The version number of this value
  * @param timestamp When this version was created
  * @param operationId Unique identifier for the operation that created this version
  * @param previousVersion The version number of the previous value (if any)
  */
case class VersionedValue[V](
  value: V,
  version: Long,
  timestamp: Instant,
  operationId: String,
  previousVersion: Option[Long] = None
)

/**
  * Recovery state for the versioned store
  * 
  * @param lastProcessedVersion The highest version that has been processed
  * @param checkpointTimestamp When this recovery state was created
  * @param pendingOperations Operations that are pending or in-flight
  * @param isConsistent Whether the store is in a consistent state
  */
case class RecoveryState(
  lastProcessedVersion: Long,
  checkpointTimestamp: Instant,
  pendingOperations: List[VersionedOperation],
  isConsistent: Boolean
)

/**
  * A recorded operation for recovery purposes
  * 
  * @param operationId Unique identifier for this operation
  * @param key The key affected by this operation (JSON encoded)
  * @param operationType Type of operation (PUT, DELETE, etc.)
  * @param version The version number assigned to this operation
  * @param timestamp When this operation occurred
  * @param valueHash Hash of the value (for PUT operations) to detect corruption
  */
case class VersionedOperation(
  operationId: String,
  key: String,
  operationType: String,
  version: Long,
  timestamp: Instant,
  valueHash: Option[String] = None
)

/**
  * A checkpoint for recovery
  * 
  * @param checkpointId Unique identifier for this checkpoint
  * @param createdAt When this checkpoint was created
  * @param versionAtCheckpoint The version number at the time of checkpoint
  * @param metadata Additional metadata about the checkpoint
  */
case class Checkpoint(
  checkpointId: String,
  createdAt: Instant,
  versionAtCheckpoint: Long,
  metadata: Map[String, String] = Map.empty
)

/**
  * Configuration for versioned stores
  * 
  * @param enableRecovery Whether to enable recovery features (operation logging, checkpoints)
  * @param maxOperationLogSize Maximum number of operations to keep in the log
  * @param concurrencyStrategy How to handle concurrent operations
  */
case class VersionedStoreConfig(
  enableRecovery: Boolean = true,
  maxOperationLogSize: Int = 1000,
  concurrencyStrategy: ConcurrencyStrategy = OptimisticLocking,
  maxRetries: Int = 3
)

/**
  * Concurrency control strategies
  */
sealed trait ConcurrencyStrategy

/**
  * Optimistic locking: operations succeed if the version hasn't changed
  */
case object OptimisticLocking extends ConcurrencyStrategy

/**
  * Pessimistic locking: operations acquire locks before proceeding
  */
case object PessimisticLocking extends ConcurrencyStrategy

/**
  * No concurrency control: last write wins
  */
case object NoConcurrencyControl extends ConcurrencyStrategy

/**
  * Statistics about the versioned store
  * 
  * @param totalKeys Total number of keys in the store
  * @param totalOperations Total number of operations performed
  * @param currentVersion Current version number
  * @param lastCheckpointId ID of the last checkpoint (if any)
  * @param lastCheckpointTimestamp When the last checkpoint was created
  * @param isConsistent Whether the store is in a consistent state
  */
case class StoreStatistics(
  totalKeys: Long,
  totalOperations: Long,
  currentVersion: Long,
  lastCheckpointId: Option[String],
  lastCheckpointTimestamp: Option[Instant],
  isConsistent: Boolean
)

/**
  * Custom exceptions for versioned store operations
  */
class VersionedStoreException(message: String, cause: Throwable = null) 
  extends RuntimeException(message, cause)

/**
  * Exception thrown when concurrent modification is detected
  */
class ConcurrentModificationException(message: String) 
  extends VersionedStoreException(message)

/**
  * Exception thrown when version mismatch is detected
  */
class VersionMismatchException(message: String) 
  extends VersionedStoreException(message)

/**
  * Exception thrown when recovery fails
  */
class RecoveryException(message: String, cause: Throwable = null) 
  extends VersionedStoreException(message, cause)

/**
  * JSON codecs for versioned store models
  */
object codecs {
  import io.circe.generic.semiauto._
  import java.time.format.DateTimeFormatter
  
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString.map(Instant.parse)
  
  implicit val concurrencyStrategyEncoder: Encoder[ConcurrencyStrategy] = Encoder.encodeString.contramap {
    case OptimisticLocking => "optimistic"
    case PessimisticLocking => "pessimistic" 
    case NoConcurrencyControl => "none"
  }
  
  implicit val concurrencyStrategyDecoder: Decoder[ConcurrencyStrategy] = Decoder.decodeString.map {
    case "optimistic" => OptimisticLocking
    case "pessimistic" => PessimisticLocking
    case "none" => NoConcurrencyControl
    case other => throw new IllegalArgumentException(s"Unknown concurrency strategy: $other")
  }
  
  implicit def versionedValueEncoder[V: Encoder]: Encoder[VersionedValue[V]] = deriveEncoder[VersionedValue[V]]
  implicit def versionedValueDecoder[V: Decoder]: Decoder[VersionedValue[V]] = deriveDecoder[VersionedValue[V]]
  
  implicit val recoveryStateEncoder: Encoder[RecoveryState] = deriveEncoder[RecoveryState]
  implicit val recoveryStateDecoder: Decoder[RecoveryState] = deriveDecoder[RecoveryState]
  
  implicit val versionedOperationEncoder: Encoder[VersionedOperation] = deriveEncoder[VersionedOperation]
  implicit val versionedOperationDecoder: Decoder[VersionedOperation] = deriveDecoder[VersionedOperation]
  
  implicit val checkpointEncoder: Encoder[Checkpoint] = deriveEncoder[Checkpoint]
  implicit val checkpointDecoder: Decoder[Checkpoint] = deriveDecoder[Checkpoint]
  
  implicit val versionedStoreConfigEncoder: Encoder[VersionedStoreConfig] = deriveEncoder[VersionedStoreConfig]
  implicit val versionedStoreConfigDecoder: Decoder[VersionedStoreConfig] = deriveDecoder[VersionedStoreConfig]
  
  implicit val storeStatisticsEncoder: Encoder[StoreStatistics] = deriveEncoder[StoreStatistics]
  implicit val storeStatisticsDecoder: Decoder[StoreStatistics] = deriveDecoder[StoreStatistics]
}