# Gibbon

Gibbon is a lightweight Scala library for building production-ready, backpressure-aware, stateful, event-time-correct streaming pipelines that can run locally for development or across a cluster. Built on top of Akka Streams, Gibbon provides higher-level abstractions that make stream processing more accessible and maintainable.

##  Features

### Core Components
- **Events**: Structured data with key-value pairs, event time, and processing timestamps
- **Sources**: Generate or consume events from various inputs (files, Kafka, HTTP, generators)
- **Flows**: Transform, filter, enrich, and process events with built-in operations
- **Sinks**: Output processed events to various destinations (console, files, Kafka, HTTP)
- **Pipelines**: Chain sources, flows, and sinks together into runnable graphs

### Built-in Sources
- **GeneratorSource**: Generate test events (strings, numbers, JSON) for development
- **FileSource**: Read events from files with custom parsers
- **KafkaSource**: Consume events from Kafka topics
- **HttpSource**: Receive events via HTTP endpoints

### Built-in Flows
- **FilterFlow**: Filter events based on predicates
- **TransformFlow**: Transform event structure and types
- **EnrichFlow**: Add additional data to events
- **WindowFlow**: Windowing operations (tumbling windows)
- **CheckpointFlow**: Automatic checkpointing for fault tolerance

### Built-in Sinks
- **ConsoleSink**: Output events to console for debugging
- **FileSink**: Write events to files with custom formatting
- **KafkaSink**: Publish events to Kafka topics
- **HttpSink**: Send events via HTTP requests

### Advanced Features
- **Fault Tolerance**: Automatic checkpointing and recovery from failures
- **State Management**: Built-in event store with Redis backend
- **Monitoring**: Metrics collection and monitoring capabilities
- **Error Handling**: Comprehensive error handling framework
- **Backpressure**: Built on Akka Streams for automatic backpressure handling

##  Installation

Add Gibbon to your `build.sbt`:

```scala
libraryDependencies += "com.example" %% "gibbon" % "0.1.0-SNAPSHOT"
```

Required dependencies:
```scala
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.8.5",
  "com.typesafe.akka" %% "akka-http" % "10.5.3",
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6"
)
```

## 🏃 Quick Start

### Basic Pipeline

```scala
import akka.actor.ActorSystem
import gibbon.core.{Event, Pipeline}
import gibbon.sources.generator.GeneratorSource
import gibbon.flows.{FilterFlow, TransformFlow}
import gibbon.sinks.ConsoleSink

implicit val system: ActorSystem = ActorSystem("MyGibbonApp")

// Create a source that generates numeric events
val source = GeneratorSource.numericEvents(
  startKey = 1,
  startValue = 0,
  eventsPerSecond = 5,
  maxEvents = Some(20)
)

// Create flows to process the events
val filterFlow = FilterFlow[Event[Int, Int]](event => event.value > 10)
val transformFlow = TransformFlow[Event[Int, Int], Event[Int, String]](event => 
  event.copy(value = s"Processed: ${event.value}")
)

// Create a sink to output results
val sink = new ConsoleSink[Int, String]()

// Build and run the pipeline
val pipeline = Pipeline(source, filterFlow, sink)
pipeline.toRunnableGraph().run()
```

### File Processing Pipeline

```scala
import gibbon.sources.file.FileSource
import gibbon.sinks.FileSink

// Read from CSV file
val fileSource = new FileSource[String, String](
  filePath = "input.csv",
  parser = line => {
    val parts = line.split(",")
    Event(parts(0), parts(1), System.currentTimeMillis())
  }
)

// Transform the data
val transformFlow = TransformFlow[Event[String, String], Event[String, String]](
  event => event.copy(value = event.value.toUpperCase)
)

// Write to output file
val fileSink = new FileSink[String, String](
  filePath = "output.csv",
  formatter = Some(event => s"${event.key},${event.value}\n")
)

val pipeline = Pipeline(fileSource, transformFlow, fileSink)
pipeline.toRunnableGraph().run()
```

### Fault-Tolerant Pipeline with Checkpointing

```scala
import gibbon.core.RecoverablePipeline
import gibbon.checkpoint.CheckpointManager

// Set up checkpoint manager (implement your own or use built-in)
val checkpointManager = new MyCheckpointManager()

// Create a recoverable pipeline
val recoverablePipeline = new RecoverablePipeline(
  pipelineId = "my-pipeline",
  source = mySource,
  flow = myFlow,
  sink = mySink,
  checkpointManager = checkpointManager,
  extractOffset = (event: MyEvent) => event.id
)

// Run with automatic recovery
recoverablePipeline.runWithRecovery()
```

## 🔧 Examples

Check out the examples in the codebase:
- `GeneratorExample.scala` - Basic event generation
- `FlowExamples.scala` - Comprehensive flow operations
- Integration tests for complete pipeline examples

## 📊 Current Status

**Version**: 0.1.0-SNAPSHOT (Active Development)

### ✅ Implemented Features
- Core abstractions (Event, Source, Flow, Sink, Pipeline)
- Built-in sources (Generator, File, Kafka, HTTP)
- Built-in flows (Filter, Transform, Enrich, Window, Checkpoint)
- Built-in sinks (Console, File, Kafka, HTTP)
- Checkpointing and recovery mechanisms
- Event store with Redis backend
- Basic monitoring and metrics
- Comprehensive test suite

### 🚧 In Development
- Advanced windowing operations
- Complex event processing features
- Enhanced monitoring and observability
- Performance optimizations

### 📋 Planned Features
- Distributed state management
- Machine learning integration
- Advanced CEP operations
- Kubernetes deployment support
- Web UI for pipeline monitoring

## 🤝 Contributing

Gibbon is actively being developed. Contributions are welcome! Please check the `ROADMAP.md` for planned features and development priorities.

## 📄 License

MIT License

## 🔗 Links

- [Roadmap](ROADMAP.md) - Development roadmap and planned features
- [Akka Streams Documentation](https://doc.akka.io/docs/akka/current/stream/index.html) - Underlying streaming framework