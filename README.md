# Gibbon

[![Maven Central](https://img.shields.io/maven-central/v/io.github.eyuael/gibbon-core_2.13.svg)](https://search.maven.org/search?q=g:io.github.eyuael%20AND%20a:gibbon-core_2.13)

Gibbon is a lightweight Scala library for building production-ready, backpressure-aware, stateful, event-time-correct streaming pipelines that can run locally for development or across a cluster. Built on top of **both Akka Streams and Pekko**, Gibbon provides higher-level abstractions that make stream processing more accessible and maintainable.

## 🎯 **Runtime Flexibility**

Gibbon features a **runtime abstraction system** that allows you to choose between **Akka Streams** and **Pekko** as your underlying streaming engine. This gives you the flexibility to:

- **Migrate from Akka to Pekko** without changing your application code
- **Choose the runtime** that best fits your deployment requirements
- **Future-proof** your streaming applications against framework changes

Simply change the implicit runtime to switch between streaming engines - **your pipeline code remains identical**!

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
- **Runtime Abstraction**: Choose between Akka Streams and Pekko without code changes
- **Fault Tolerance**: Automatic checkpointing and recovery from failures
- **State Management**: Built-in event store with Redis backend
- **Monitoring**: Metrics collection and monitoring capabilities
- **Error Handling**: Comprehensive error handling framework
- **Backpressure**: Built on Akka Streams/Pekko for automatic backpressure handling

##  Installation

Gibbon is available on Maven Central and organized into multiple modules to support both Akka Streams and Pekko. Choose the modules that match your streaming runtime:

### For Akka Streams

Add these dependencies to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "io.github.eyuael" %% "gibbon-core" % "0.1.0",
  "io.github.eyuael" %% "gibbon-akka" % "0.1.0"
)
```

### For Pekko

Add these dependencies to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "io.github.eyuael" %% "gibbon-core" % "0.1.0",
  "io.github.eyuael" %% "gibbon-pekko" % "0.1.0"
)
```

### For Both (Runtime Selection)

If you want the flexibility to choose at runtime:

```scala
libraryDependencies ++= Seq(
  "io.github.eyuael" %% "gibbon-core" % "0.1.0",
  "io.github.eyuael" %% "gibbon-akka" % "0.1.0",
  "io.github.eyuael" %% "gibbon-pekko" % "0.1.0"
)
```

### Maven

For Maven projects, add these dependencies to your `pom.xml`:

```xml
<dependencies>
  <!-- Core module -->
  <dependency>
    <groupId>io.github.eyuael</groupId>
    <artifactId>gibbon-core_2.13</artifactId>
    <version>0.1.0</version>
  </dependency>
  
  <!-- For Akka Streams -->
  <dependency>
    <groupId>io.github.eyuael</groupId>
    <artifactId>gibbon-akka_2.13</artifactId>
    <version>0.1.0</version>
  </dependency>
  
  <!-- For Pekko -->
  <dependency>
    <groupId>io.github.eyuael</groupId>
    <artifactId>gibbon-pekko_2.13</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

### Additional Dependencies

You may also need these common dependencies:

```scala
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6"
)
```

## 🏃 Quick Start

### Basic Pipeline with Akka Streams

```scala
import gibbon.core.{Event, Pipeline}
import gibbon.sources.generator.GeneratorSource
import gibbon.flows.{FilterFlow, TransformFlow}
import gibbon.sinks.ConsoleSink
import gibbon.runtime.AkkaStreamingRuntime

// Choose Akka Streams as your runtime
implicit val runtime = new AkkaStreamingRuntime()

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
val sink = ConsoleSink[Int, String]()

// Build and run the pipeline
val pipeline = Pipeline(source, filterFlow, sink)
val runnableGraph = pipeline.toRunnableGraph()
runnableGraph.run()
```

### Basic Pipeline with Pekko

```scala
import gibbon.core.{Event, Pipeline}
import gibbon.sources.generator.GeneratorSource
import gibbon.flows.{FilterFlow, TransformFlow}
import gibbon.sinks.ConsoleSink
import gibbon.runtime.PekkoStreamingRuntime

// Choose Pekko as your runtime - same code, different runtime!
implicit val runtime = new PekkoStreamingRuntime()

// Everything else is identical to the Akka example above
val source = GeneratorSource.numericEvents(
  startKey = 1,
  startValue = 0,
  eventsPerSecond = 5,
  maxEvents = Some(20)
)

val filterFlow = FilterFlow[Event[Int, Int]](event => event.value > 10)
val transformFlow = TransformFlow[Event[Int, Int], Event[Int, String]](event => 
  event.copy(value = s"Processed: ${event.value}")
)

val sink = ConsoleSink[Int, String]()

val pipeline = Pipeline(source, filterFlow, sink)
val runnableGraph = pipeline.toRunnableGraph()
runnableGraph.run()
```

### Runtime Selection at Application Startup

```scala
import gibbon.runtime.{AkkaStreamingRuntime, PekkoStreamingRuntime}

// Choose runtime based on configuration or environment
val useAkka = sys.env.get("STREAMING_RUNTIME").contains("akka")

implicit val runtime = if (useAkka) {
  new AkkaStreamingRuntime()
} else {
  new PekkoStreamingRuntime()
}

// Your pipeline code remains the same regardless of runtime choice
val pipeline = Pipeline(mySource, myFlow, mySink)
pipeline.toRunnableGraph().run()
```

### File Processing Pipeline

```scala
import gibbon.sources.file.FileSource
import gibbon.sinks.FileSink
import gibbon.runtime.AkkaStreamingRuntime // or PekkoStreamingRuntime

implicit val runtime = new AkkaStreamingRuntime()

// Read from CSV file
val fileSource = FileSource[String, String](
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
val fileSink = FileSink[String, String](
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
import gibbon.runtime.AkkaStreamingRuntime // or PekkoStreamingRuntime

implicit val runtime = new AkkaStreamingRuntime()

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

**Version**: 0.1.0 (Published on Maven Central)

### ✅ Implemented Features
- **Runtime Abstraction**: Choose between Akka Streams and Pekko without code changes
- Core abstractions (Event, Source, Flow, Sink, Pipeline)
- Built-in sources (Generator, File, Kafka, HTTP)
- Built-in flows (Filter, Transform, Enrich, Window, Checkpoint)
- Built-in sinks (Console, File, Kafka, HTTP)
- Checkpointing and recovery mechanisms
- Event store with Redis backend
- Basic monitoring and metrics
- Comprehensive test suite
- **Published to Maven Central** - Ready for production use!

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
- [Akka Streams Documentation](https://doc.akka.io/docs/akka/current/stream/index.html) - Akka Streams framework
- [Pekko Documentation](https://pekko.apache.org/docs/pekko/current/) - Pekko streaming framework