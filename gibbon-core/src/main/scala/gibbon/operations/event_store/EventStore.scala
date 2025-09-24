package gibbon.operations.event_store

import scala.concurrent.Future
import scala.collection.concurrent.TrieMap

trait EventStore[K, V] {
  def get(key: K): Future[Option[V]]
  def put(key: K, value: V): Future[Unit]
  def delete(key: K): Future[Unit]
  def getAll: Future[Map[K, V]]
}

class InMemoryEventStore[K, V] extends EventStore[K, V] {
  private val store = TrieMap.empty[K, V]

  override def get(key: K): Future[Option[V]] = Future.successful(store.get(key))
  override def put(key: K, value: V): Future[Unit] = Future.successful(store.put(key, value))
  override def delete(key: K): Future[Unit] = Future.successful(store.remove(key))
  override def getAll: Future[Map[K, V]] = Future.successful(store.toMap)
}
