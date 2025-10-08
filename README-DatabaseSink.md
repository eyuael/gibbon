# DatabaseSink Setup and Testing Guide

This guide will help you set up and test the Gibbon DatabaseSink with PostgreSQL.

## Prerequisites

1. **PostgreSQL Server**: You need a PostgreSQL server running locally
2. **Database**: A database named `Sink` (or any name you prefer)
3. **Scala/SBT**: Ensure you have SBT installed for building and running tests

## Quick Setup

### 1. PostgreSQL Installation

**macOS (using Homebrew):**
```bash
brew install postgresql
brew services start postgresql
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

**Windows:**
Download and install from [PostgreSQL official website](https://www.postgresql.org/download/windows/)

### 2. Database Setup

Connect to PostgreSQL and create the database:

```bash
# Connect to PostgreSQL (default user is usually 'postgres')
psql -U postgres

# Create the database
CREATE DATABASE "Sink";

# Connect to the new database
\c Sink

# Exit psql
\q
```

### 3. Run Database Schema Setup

Execute the provided SQL script to create the necessary tables:

```bash
psql -U postgres -d Sink -f database-setup.sql
```

Or manually run the SQL commands from `database-setup.sql` in your PostgreSQL client.

## Configuration

### Database Connection Parameters

Update these parameters in your code based on your PostgreSQL setup:

```scala
val dbHost = "localhost"        // Your PostgreSQL host
val dbPort = 5432              // Your PostgreSQL port (default: 5432)
val dbName = "Sink"            // Your database name
val dbUsername = "postgres"     // Your PostgreSQL username
val dbPassword = "password"     // Your PostgreSQL password
```

### Common PostgreSQL Default Credentials

- **Username**: `postgres`
- **Password**: Often empty or set during installation
- **Host**: `localhost`
- **Port**: `5432`

## Usage Examples

### Basic Usage

```scala
import gibbon.sinks.DatabaseSink
import gibbon.core.Event

implicit val ec = scala.concurrent.ExecutionContext.global

// Create DatabaseSink instance
val sink = DatabaseSink.postgres[String, String](
  database = "Sink",
  username = "postgres",
  password = "your_password"
)

// Create and write an event
val event = Event("user-123", "login", System.currentTimeMillis())
sink.write(event)
sink.flush() // Ensure immediate write
```

### Advanced Configuration

```scala
val sink = DatabaseSink.withSettings[String, String](
  jdbcUrl = "jdbc:postgresql://localhost:5432/Sink",
  username = "postgres",
  password = "your_password",
  tableName = "custom_events",
  batchSize = 50,              // Events per batch
  retryAttempts = 3,           // Retry attempts on failure
  createTableIfNotExists = true
)
```

### Streaming Integration

```scala
import gibbon.runtime.TestStreamingRuntime
import gibbon.runtime.TestSource

implicit val runtime = new TestStreamingRuntime()

val events = List(
  Event("key1", "value1", System.currentTimeMillis()),
  Event("key2", "value2", System.currentTimeMillis())
)

val source = TestSource(events: _*)
val runtimeSink = sink.toRuntimeSink()

source.runWith(runtimeSink)
```

## Testing

### Running the Test Suite

```bash
# Run all tests
sbt test

# Run only DatabaseSink tests
sbt "testOnly *DatabaseSinkTest"
```

### Test Configuration

The tests are marked as `.flaky` because they require a live database connection. Update the test configuration in `DatabaseSinkTest.scala`:

```scala
val testUsername = "postgres"     // Your PostgreSQL username
val testPassword = "password"     // Your PostgreSQL password
```

### Running the Example Application

```bash
# Compile the project
sbt compile

# Run the example (update credentials first)
sbt "runMain examples.DatabaseSinkExample"
```

## Database Schema

The DatabaseSink creates the following table structure:

```sql
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    event_key TEXT,
    event_value TEXT,
    event_time BIGINT,
    timestamp_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Table Fields

- `id`: Auto-incrementing primary key
- `event_key`: The event key (converted to string)
- `event_value`: The event value (converted to string)
- `event_time`: Event time from the Event object
- `timestamp_ms`: Timestamp from the Event object
- `created_at`: Database insertion timestamp

## Performance Features

### Batch Processing
- Events are batched for efficient database writes
- Default batch size: 100 events
- Configurable batch size for different use cases

### Connection Management
- Automatic connection initialization
- Connection reuse for multiple operations
- Proper connection cleanup on close

### Error Handling
- Configurable retry attempts
- Exponential backoff on failures
- Transaction rollback on batch failures

### Monitoring
- Built-in row count queries for testing
- Table clearing utilities for test cleanup

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Ensure PostgreSQL is running: `brew services list | grep postgresql`
   - Check if the port is correct (default: 5432)

2. **Authentication Failed**
   - Verify username and password
   - Check PostgreSQL authentication configuration (`pg_hba.conf`)

3. **Database Does Not Exist**
   - Create the database: `CREATE DATABASE "Sink";`
   - Ensure you're connecting to the correct database name

4. **Table Does Not Exist**
   - Run the `database-setup.sql` script
   - Or set `createTableIfNotExists = true` in the sink configuration

5. **Permission Denied**
   - Ensure your user has necessary privileges on the database
   - Grant permissions: `GRANT ALL PRIVILEGES ON DATABASE "Sink" TO your_username;`

### Verification Queries

```sql
-- Check if table exists
\dt

-- Count total events
SELECT COUNT(*) FROM events;

-- View recent events
SELECT * FROM events ORDER BY created_at DESC LIMIT 10;

-- Check table structure
\d events
```

## Performance Tips

1. **Batch Size**: Adjust based on your throughput requirements
   - Higher batch size = better throughput, higher latency
   - Lower batch size = lower latency, more database connections

2. **Connection Pooling**: For production use, consider implementing connection pooling

3. **Indexing**: The setup script creates indexes on timestamp fields for better query performance

4. **Monitoring**: Use `getTableRowCount()` to monitor sink effectiveness

## Integration with Akka/Pekko

The DatabaseSink works seamlessly with both Akka Streams and Pekko Streams through the runtime abstraction:

```scala
// With Akka
import gibbon.runtime.AkkaStreamingRuntime
implicit val akkaRuntime = new AkkaStreamingRuntime()

// With Pekko  
import gibbon.runtime.PekkoStreamingRuntime
implicit val pekkoRuntime = new PekkoStreamingRuntime()

val sink = DatabaseSink.postgres[String, String](...)
val runtimeSink = sink.toRuntimeSink()
```

This allows you to use the same DatabaseSink code with either streaming framework.