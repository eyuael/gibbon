package gibbon.sinks

import gibbon.core.Event
import akka.stream.scaladsl.Source
import java.io.File
import java.nio.file.{Files, Paths}
import scala.io.{Source => ScalaSource}
import scala.concurrent.Future

class FileSinkTest extends SinkTestSuite {
  
  private def readFileContent(filePath: String): String = {
    val source = ScalaSource.fromFile(filePath)
    try {
      source.mkString
    } finally {
      source.close()
    }
  }
  
  private def cleanupFile(filePath: String): Unit = {
    val file = new File(filePath)
    if (file.exists()) {
      file.delete()
    }
  }
  
  test("FileSink should write events to file with default CSV format") {
    val filePath = SinkTestUtils.createTempFilePath("test-csv", ".csv")
    
    try {
      val sink = FileSink[String, String](filePath)
      val event = SinkTestUtils.createEvent("test-key", "test-value")
      val source = Source.single(event)
      
      val future = source.runWith(sink.toAkkaSink())
      SinkTestUtils.awaitResult(future)
      SinkTestUtils.awaitResult(sink.close())
      
      val content = readFileContent(filePath)
      assert(content.contains("test-key"))
      assert(content.contains("test-value"))
      assert(content.contains(",")) // CSV format
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink should append to existing file when append=true") {
    val filePath = SinkTestUtils.createTempFilePath("test-append", ".txt")
    
    try {
      // Write first event
      val sink1 = FileSink[String, String](filePath, append = true)
      val event1 = SinkTestUtils.createEvent("key1", "value1")
      val source1 = Source.single(event1)
      
      val future1 = source1.runWith(sink1.toAkkaSink())
      SinkTestUtils.awaitResult(future1)
      SinkTestUtils.awaitResult(sink1.close())
      
      // Write second event
      val sink2 = FileSink[String, String](filePath, append = true)
      val event2 = SinkTestUtils.createEvent("key2", "value2")
      val source2 = Source.single(event2)
      
      val future2 = source2.runWith(sink2.toAkkaSink())
      SinkTestUtils.awaitResult(future2)
      SinkTestUtils.awaitResult(sink2.close())
      
      val content = readFileContent(filePath)
      assert(content.contains("key1"))
      assert(content.contains("key2"))
      assert(content.contains("value1"))
      assert(content.contains("value2"))
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink should overwrite file when append=false") {
    val filePath = SinkTestUtils.createTempFilePath("test-overwrite", ".txt")
    
    try {
      // Write first event
      val sink1 = FileSink[String, String](filePath, append = false)
      val event1 = SinkTestUtils.createEvent("key1", "value1")
      val source1 = Source.single(event1)
      
      val future1 = source1.runWith(sink1.toAkkaSink())
      SinkTestUtils.awaitResult(future1)
      SinkTestUtils.awaitResult(sink1.close())
      
      // Write second event (should overwrite)
      val sink2 = FileSink[String, String](filePath, append = false)
      val event2 = SinkTestUtils.createEvent("key2", "value2")
      val source2 = Source.single(event2)
      
      val future2 = source2.runWith(sink2.toAkkaSink())
      SinkTestUtils.awaitResult(future2)
      SinkTestUtils.awaitResult(sink2.close())
      
      val content = readFileContent(filePath)
      assert(!content.contains("key1"))
      assert(content.contains("key2"))
      assert(!content.contains("value1"))
      assert(content.contains("value2"))
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink should use custom formatter") {
    val filePath = SinkTestUtils.createTempFilePath("test-custom", ".txt")
    
    try {
      val customFormatter = (event: Event[String, String]) => 
        s"CUSTOM: ${event.key} -> ${event.value}\n"
      
      val sink = FileSink[String, String](filePath, true, customFormatter)
      val event = SinkTestUtils.createEvent("mykey", "myvalue")
      val source = Source.single(event)
      
      val future = source.runWith(sink.toAkkaSink())
      SinkTestUtils.awaitResult(future)
      SinkTestUtils.awaitResult(sink.close())
      
      val content = readFileContent(filePath)
      assert(content.contains("CUSTOM: mykey -> myvalue"))
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink.csv should format events as CSV") {
    val filePath = SinkTestUtils.createTempFilePath("test-csv-format", ".csv")
    
    try {
      val sink = FileSink.csv[String, String](filePath)
      val events = SinkTestUtils.createEvents(
        ("key1", "value1"),
        ("key2", "value2")
      )
      val source = Source(events)
      
      val future = source.runWith(sink.toAkkaSink())
      SinkTestUtils.awaitResult(future)
      SinkTestUtils.awaitResult(sink.close())
      
      val content = readFileContent(filePath)
      val lines = content.split("\n").filter(_.nonEmpty)
      
      assert(lines.length == 2)
      lines.foreach { line =>
        val parts = line.split(",")
        assert(parts.length == 4) // timestamp, key, value, eventTime
      }
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink.json should format events as JSON") {
    val filePath = SinkTestUtils.createTempFilePath("test-json-format", ".json")
    
    try {
      val sink = FileSink.json[String, String](filePath)
      val event = SinkTestUtils.createEvent("test-key", "test-value")
      val source = Source.single(event)
      
      val future = source.runWith(sink.toAkkaSink())
      SinkTestUtils.awaitResult(future)
      SinkTestUtils.awaitResult(sink.close())
      
      val content = readFileContent(filePath)
      assert(content.contains("\"key\":\"test-key\""))
      assert(content.contains("\"value\":\"test-value\""))
      assert(content.contains("\"timestamp\":"))
      assert(content.contains("\"eventTime\":"))
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink should handle batch writing") {
    val filePath = SinkTestUtils.createTempFilePath("test-batch", ".txt")
    
    try {
      val sink = FileSink[String, String](filePath)
      val events = SinkTestUtils.createEvents(
        ("key1", "value1"),
        ("key2", "value2"),
        ("key3", "value3")
      )
      
      val future = sink.writeBatch(events)
      SinkTestUtils.awaitResult(future)
      SinkTestUtils.awaitResult(sink.close())
      
      val content = readFileContent(filePath)
      assert(content.contains("key1"))
      assert(content.contains("key2"))
      assert(content.contains("key3"))
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink should create parent directories if they don't exist") {
    val tempDir = System.getProperty("java.io.tmpdir")
    val nestedPath = s"$tempDir/test-nested-${SinkTestUtils.generateTestId()}/subdir/test.txt"
    
    try {
      val sink = FileSink[String, String](nestedPath)
      val event = SinkTestUtils.createEvent("test-key", "test-value")
      val source = Source.single(event)
      
      val future = source.runWith(sink.toAkkaSink())
      SinkTestUtils.awaitResult(future)
      SinkTestUtils.awaitResult(sink.close())
      
      assert(Files.exists(Paths.get(nestedPath)))
      val content = readFileContent(nestedPath)
      assert(content.contains("test-key"))
    } finally {
      // Clean up nested directories
      val file = new File(nestedPath)
      if (file.exists()) {
        file.delete()
        file.getParentFile.delete()
        file.getParentFile.getParentFile.delete()
      }
    }
  }
  
  test("FileSink should handle empty stream") {
    val filePath = SinkTestUtils.createTempFilePath("empty", ".txt")
    
    try {
      val sink = FileSink[String, String](filePath)
      val source = Source.empty[Event[String, String]]
      
      val future = source.runWith(sink.toAkkaSink())
      SinkTestUtils.awaitResult(future)
      SinkTestUtils.awaitResult(sink.close())
      
      // File may or may not exist for empty streams, but if it exists it should be empty
      if (Files.exists(Paths.get(filePath))) {
        val content = readFileContent(filePath)
        assert(content.isEmpty)
      }
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink should handle different data types") {
    val filePath = SinkTestUtils.createTempFilePath("test-types", ".txt")
    
    try {
      val sink = FileSink[Int, Double](filePath)
      val event = SinkTestUtils.createEvent(42, 3.14)
      val source = Source.single(event)
      
      val future = source.runWith(sink.toAkkaSink())
      SinkTestUtils.awaitResult(future)
      SinkTestUtils.awaitResult(sink.close())
      
      val content = readFileContent(filePath)
      assert(content.contains("42"))
      assert(content.contains("3.14"))
    } finally {
      cleanupFile(filePath)
    }
  }
  
  test("FileSink close should complete successfully") {
    val filePath = SinkTestUtils.createTempFilePath("test-close", ".txt")
    
    try {
      val sink = FileSink[String, String](filePath)
      val future = sink.close()
      SinkTestUtils.awaitResult(future)
      // Should complete without throwing
    } finally {
      cleanupFile(filePath)
    }
  }
}