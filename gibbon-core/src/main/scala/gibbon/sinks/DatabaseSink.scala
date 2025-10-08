package gibbon.sinks

import gibbon.core.{Event, Sink}
import gibbon.runtime.StreamingRuntime
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Try, Success, Failure}
import java.sql.{Connection, DriverManager, PreparedStatement, SQLException}
import java.util.Properties

class DatabaseSink[K, V](
  jdbcUrl: String,
  username: String,
  password: String,
  tableName: String = "events",
  batchSize: Int = 100,
  connectionPoolSize: Int = 5,
  retryAttempts: Int = 3,
  createTableIfNotExists: Boolean = true
)(implicit ec: ExecutionContext) extends Sink[Event[K, V]] {

  @volatile private var connection: Option[Connection] = None
  @volatile private var eventBuffer: List[Event[K, V]] = List.empty
  
  // Default table schema for events
  private val defaultCreateTableSQL = s"""
    CREATE TABLE IF NOT EXISTS $tableName (
      id SERIAL PRIMARY KEY,
      event_key TEXT,
      event_value TEXT,
      event_time BIGINT,
      timestamp_ms BIGINT,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  """

  private val insertSQL = s"""
    INSERT INTO $tableName (event_key, event_value, event_time, timestamp_ms) 
    VALUES (?, ?, ?, ?)
  """

  // Initialize connection
  private def initConnection(): Try[Connection] = Try {
    Class.forName("org.postgresql.Driver")
    val props = new Properties()
    props.setProperty("user", username)
    props.setProperty("password", password)
    props.setProperty("ssl", "false")
    props.setProperty("connectTimeout", "10")
    
    val conn = DriverManager.getConnection(jdbcUrl, props)
    conn.setAutoCommit(false) // Use transactions for batch inserts
    
    if (createTableIfNotExists) {
      val stmt = conn.createStatement()
      try {
        stmt.execute(defaultCreateTableSQL)
        conn.commit()
      } finally {
        stmt.close()
      }
    }
    
    conn
  }

  // Get or create connection
  private def getConnection(): Future[Connection] = Future {
    connection match {
      case Some(conn) if !conn.isClosed => conn
      case _ =>
        initConnection() match {
          case Success(conn) =>
            connection = Some(conn)
            conn
          case Failure(ex) =>
            throw new RuntimeException(s"Failed to initialize database connection: ${ex.getMessage}", ex)
        }
    }
  }

  override def toRuntimeSink[R <: StreamingRuntime]()(implicit runtime: R): runtime.Sink[Event[K, V], Future[Unit]] = {
    runtime.foreachSink[Event[K, V]] { event =>
      write(event)
      () // Convert Future[Unit] to Unit for the sink
    }
  }

  def write(event: Event[K, V]): Future[Unit] = {
    synchronized {
      eventBuffer = event :: eventBuffer
      if (eventBuffer.length >= batchSize) {
        val batch = eventBuffer.reverse
        eventBuffer = List.empty
        writeBatch(batch)
      } else {
        Future.successful(())
      }
    }
  }

  def writeBatch(events: Seq[Event[K, V]]): Future[Unit] = {
    if (events.isEmpty) return Future.successful(())
    
    def attemptWrite(attempt: Int): Future[Unit] = {
      getConnection().flatMap { conn =>
        Future {
          var stmt: PreparedStatement = null
          try {
            stmt = conn.prepareStatement(insertSQL)
            
            events.foreach { event =>
              stmt.setString(1, event.key.toString)
              stmt.setString(2, event.value.toString)
              stmt.setLong(3, event.eventTime)
              stmt.setLong(4, event.timestamp)
              stmt.addBatch()
            }
            
            stmt.executeBatch()
            conn.commit()
            
          } catch {
            case ex: SQLException =>
              conn.rollback()
              throw ex
          } finally {
            if (stmt != null) stmt.close()
          }
        }.recoverWith {
          case ex if attempt < retryAttempts =>
            println(s"Database write failed (attempt $attempt/$retryAttempts): ${ex.getMessage}")
            Thread.sleep(1000 * attempt) // Exponential backoff
            attemptWrite(attempt + 1)
          case ex =>
            Future.failed(new RuntimeException(s"Failed to write batch after $retryAttempts attempts", ex))
        }
      }
    }
    
    attemptWrite(1)
  }

  def flush(): Future[Unit] = {
    synchronized {
      if (eventBuffer.nonEmpty) {
        val batch = eventBuffer.reverse
        eventBuffer = List.empty
        writeBatch(batch)
      } else {
        Future.successful(())
      }
    }
  }

  def close(): Future[Unit] = {
    flush().flatMap { _ =>
      Future {
        connection.foreach { conn =>
          Try {
            if (!conn.isClosed) {
              conn.close()
            }
          }
        }
        connection = None
      }
    }
  }

  // Utility methods for testing
  def getTableRowCount(): Future[Long] = {
    getConnection().flatMap { conn =>
      Future {
        val stmt = conn.createStatement()
        try {
          val rs = stmt.executeQuery(s"SELECT COUNT(*) FROM $tableName")
          if (rs.next()) rs.getLong(1) else 0L
        } finally {
          stmt.close()
        }
      }
    }
  }

  def clearTable(): Future[Unit] = {
    getConnection().flatMap { conn =>
      Future {
        val stmt = conn.createStatement()
        try {
          stmt.execute(s"DELETE FROM $tableName")
          conn.commit()
        } finally {
          stmt.close()
        }
      }
    }
  }
}

object DatabaseSink {
  def apply[K, V](
    jdbcUrl: String,
    username: String,
    password: String
  )(implicit ec: ExecutionContext): DatabaseSink[K, V] = 
    new DatabaseSink[K, V](jdbcUrl, username, password)
    
  def apply[K, V](
    jdbcUrl: String,
    username: String,
    password: String,
    tableName: String
  )(implicit ec: ExecutionContext): DatabaseSink[K, V] = 
    new DatabaseSink[K, V](jdbcUrl, username, password, tableName)
    
  def withSettings[K, V](
    jdbcUrl: String,
    username: String,
    password: String,
    tableName: String = "events",
    batchSize: Int = 100,
    connectionPoolSize: Int = 5,
    retryAttempts: Int = 3,
    createTableIfNotExists: Boolean = true
  )(implicit ec: ExecutionContext): DatabaseSink[K, V] = 
    new DatabaseSink[K, V](
      jdbcUrl, username, password, tableName, 
      batchSize, connectionPoolSize, retryAttempts, createTableIfNotExists
    )

  // Convenience method for PostgreSQL
  def postgres[K, V](
    host: String = "localhost",
    port: Int = 5432,
    database: String,
    username: String,
    password: String,
    tableName: String = "events"
  )(implicit ec: ExecutionContext): DatabaseSink[K, V] = {
    val jdbcUrl = s"jdbc:postgresql://$host:$port/$database"
    new DatabaseSink[K, V](jdbcUrl, username, password, tableName)
  }
}