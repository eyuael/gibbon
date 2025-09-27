package gibbon.operations.event_store.versioned

import gibbon.operations.event_store.versioned.VersionedEventStore
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Test specification for InMemoryVersionedEventStore
  */
class InMemoryVersionedEventStoreSpec extends VersionedEventStoreSpec {
  
  override def createStore(): VersionedEventStore[String, String] = {
    VersionedEventStore.inMemory[String, String]()
  }
  
  test("should be an in-memory implementation") {
    val store = createStore()
    assert(store.getClass.getSimpleName.contains("InMemory"))
  }
  
  test("should have fast in-memory performance") {
    val store = createStore()
    
    // Test with many operations to verify in-memory performance
    val futures = (1 to 100).map { i =>
      store.putVersioned(s"key$i", s"value$i")
    }
    
    import scala.concurrent.{Await, Future}
    import scala.concurrent.duration._
    
    val results = Await.result(Future.sequence(futures), 5.seconds)
    
    assert(results.forall(_.isRight))
    
    val stats = Await.result(store.getStatistics, 3.seconds)
    assert(stats.totalOperations >= 100)
  }
  
  test("should maintain consistency after operations") {
    val store = createStore()
    
    val result = for {
      _ <- store.putVersioned("key1", "value1")
      _ <- store.putVersioned("key2", "value2")
      isConsistent <- store.verifyConsistency()
    } yield isConsistent
    
    val isConsistent = Await.result(result, 3.seconds)
    assert(isConsistent == true)
  }
}