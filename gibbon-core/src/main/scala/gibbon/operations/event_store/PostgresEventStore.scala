package gibbon.operations.event_store

import scala.concurrent.{Future, ExecutionContext}
import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet}
import io.circe.{Encoder, Decoder}
import io.circe.syntax._
import io.circe.parser._

class PostgresEventStore[K: Encoder: Decoder, V: Encoder: Decoder](
  jdbcUrl: String,
  username: String,
  password: String,
  tableName: String = "gibbon_event_store"
)(implicit ec: ExecutionContext) extends EventStore[K, V] {
  
  private def initTable(): Future[Unit] = Future {
    val conn = DriverManager.getConnection(jdbcUrl, username, password)
    try {
      val stmt = conn.createStatement()
      stmt.execute(s"""
        CREATE TABLE IF NOT EXISTS $tableName (
          key_hash VARCHAR(255) PRIMARY KEY,
          key_json TEXT NOT NULL,
          value_json TEXT NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
      """)
    } finally {
      conn.close()
    }
  }
  
  initTable()
  
  override def get(key: K): Future[Option[V]] = Future {
    val conn = DriverManager.getConnection(jdbcUrl, username, password)
    try {
      val keyJson = key.asJson.noSpaces
      val keyHash = keyJson.hashCode.toString
      
      val stmt = conn.prepareStatement(s"SELECT value_json FROM $tableName WHERE key_hash = ?")
      stmt.setString(1, keyHash)
      
      val rs = stmt.executeQuery()
      if (rs.next()) {
        decode[V](rs.getString("value_json")).toOption
      } else {
        None
      }
    } finally {
      conn.close()
    }
  }
  
  override def put(key: K, value: V): Future[Unit] = Future {
    val conn = DriverManager.getConnection(jdbcUrl, username, password)
    try {
      val keyJson = key.asJson.noSpaces
      val valueJson = value.asJson.noSpaces
      val keyHash = keyJson.hashCode.toString
      
      val stmt = conn.prepareStatement(s"""
        INSERT INTO $tableName (key_hash, key_json, value_json, updated_at) 
        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT (key_hash) DO UPDATE SET 
          value_json = EXCLUDED.value_json,
          updated_at = CURRENT_TIMESTAMP
      """)
      
      stmt.setString(1, keyHash)
      stmt.setString(2, keyJson)
      stmt.setString(3, valueJson)
      stmt.executeUpdate()
    } finally {
      conn.close()
    }
  }
  
  override def delete(key: K): Future[Unit] = Future {
    val conn = DriverManager.getConnection(jdbcUrl, username, password)
    try {
      val keyJson = key.asJson.noSpaces
      val keyHash = keyJson.hashCode.toString
      
      val stmt = conn.prepareStatement(s"DELETE FROM $tableName WHERE key_hash = ?")
      stmt.setString(1, keyHash)
      stmt.executeUpdate()
    } finally {
      conn.close()
    }
  }
  
  override def getAll: Future[Map[K, V]] = Future {
    val conn = DriverManager.getConnection(jdbcUrl, username, password)
    try {
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery(s"SELECT key_json, value_json FROM $tableName")
      
      val results = scala.collection.mutable.Map[K, V]()
      while (rs.next()) {
        for {
          k <- decode[K](rs.getString("key_json")).toOption
          v <- decode[V](rs.getString("value_json")).toOption
        } {
          results += k -> v
        }
      }
      results.toMap
    } finally {
      conn.close()
    }
  }
  
  /**
   * Get all keys in the store
   * 
   * @return List of all keys
   */
  def getAllKeys: Future[List[K]] = Future {
    val conn = DriverManager.getConnection(jdbcUrl, username, password)
    try {
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery(s"SELECT key_json FROM $tableName")
      
      val results = scala.collection.mutable.ListBuffer[K]()
      while (rs.next()) {
        decode[K](rs.getString("key_json")).toOption.foreach { key =>
          results += key
        }
      }
      results.toList
    } finally {
      conn.close()
    }
  }
  
  /**
   * Clear all data from the store
   * 
   * @return Future indicating completion
   */
  def clear(): Future[Unit] = Future {
    val conn = DriverManager.getConnection(jdbcUrl, username, password)
    try {
      val stmt = conn.createStatement()
      stmt.executeUpdate(s"DELETE FROM $tableName")
    } finally {
      conn.close()
    }
  }
}
