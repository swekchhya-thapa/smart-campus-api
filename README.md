# Smart Campus Sensor & Room Management API

A JAX-RS RESTful API for managing campus rooms, sensors, and sensor readings.  
Built with **Jersey 3.1** on an embedded **Grizzly HTTP server** — no external containers required.

---

## API Overview

The API exposes three primary resource collections:

| Resource | Base Path | Description |
|---|---|---|
| Discovery | `GET /api/v1` | API metadata and resource links |
| Rooms | `/api/v1/rooms` | Manage campus rooms |
| Sensors | `/api/v1/sensors` | Manage sensors and filter by type |
| Readings | `/api/v1/sensors/{id}/readings` | Historical readings per sensor |

### Data Models

- **Room** — has an ID, name, capacity, and a list of sensor IDs
- **Sensor** — has an ID, type, status (`ACTIVE` / `MAINTENANCE` / `OFFLINE`), current value, and a room reference
- **SensorReading** — has a UUID, epoch timestamp, and recorded value

All data is stored in-memory using `ConcurrentHashMap` and `ArrayList`. No database is used.

---

## Build & Run

### Prerequisites

- Java 11+
- Maven 3.6+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api

# 2. Build the fat jar
mvn clean package

# 3. Run the server
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

The server starts on **http://localhost:8080**  
Base API path: **http://localhost:8080/api/v1**

---

## Sample curl Commands

### 1. Discover the API
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. List all rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 3. Create a new room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-002","name":"Main Hall","capacity":200}'
```

### 4. Get a specific room
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Try to delete a room that has sensors (expect 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 6. Create a new sensor (valid roomId)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"LAB-101"}'
```

### 7. Try to create a sensor with an invalid roomId (expect 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

### 8. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 9. Post a reading to an active sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'
```

### 10. Post a reading to a MAINTENANCE sensor (expect 403)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5.0}'
```

### 11. Get reading history for a sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 12. Delete a sensor then delete its now-empty room
```bash
curl -X DELETE http://localhost:8080/api/v1/sensors/TEMP-001
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---

## Report: Answers to Coursework Questions

---

### Part 1.1 — JAX-RS Resource Lifecycle & Thread Safety

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request scope). This means instance fields are never shared between requests and are inherently thread-safe in isolation. However, it also means that any data stored as an instance field would be lost the moment the request finishes.

This architectural decision has a direct impact on how shared state must be managed. Because each request gets its own resource object, shared in-memory data — such as the list of rooms or sensors — **cannot be stored as instance fields**. If it were, every request would start with an empty dataset.

The solution used in this project is to store all data in **static fields on a dedicated `DataStore` class** (`ConcurrentHashMap`). Since static fields belong to the class, not to any instance, they persist across all requests. `ConcurrentHashMap` is used instead of a plain `HashMap` because multiple requests may read and write concurrently (e.g., one request creating a sensor while another is reading the list). `ConcurrentHashMap` provides thread-safe atomic operations without requiring explicit `synchronized` blocks, preventing data corruption and race conditions.

If a plain `HashMap` were used instead, concurrent modifications could cause a `ConcurrentModificationException` or silently corrupt the data structure.

---

### Part 1.2 — HATEOAS and Hypermedia in REST APIs

HATEOAS (Hypermedia as the Engine of Application State) is the principle that API responses should include **navigable links to related resources and available actions**, rather than forcing clients to rely on static documentation to construct URLs.

For example, a response to `GET /api/v1/rooms/LIB-301` might include:
```json
{
  "id": "LIB-301",
  "links": {
    "self": "/api/v1/rooms/LIB-301",
    "sensors": "/api/v1/rooms/LIB-301/sensors",
    "delete": "/api/v1/rooms/LIB-301"
  }
}
```

This is considered a hallmark of advanced REST design for several reasons. First, it makes the API **self-documenting** — a client can explore the API by following links, just like a browser navigates the web. Second, it **decouples the client from hardcoded URL structures**; if the server changes a path, clients that follow links rather than constructing URLs continue to work without modification. Third, it **communicates available actions contextually** — for instance, the absence of a `delete` link signals the client that deletion is not currently permitted (e.g., the room has sensors), without requiring the client to attempt the call first.

Compared to static documentation, HATEOAS ensures clients always have access to the current valid state of the API, rather than potentially outdated documentation.

---

### Part 2.1 — Returning IDs vs Full Objects in List Responses

When returning a list of rooms, there is a trade-off between two approaches:

**Returning IDs only** (e.g., `["LIB-301", "LAB-101"]`) minimises the response payload, which reduces network bandwidth. However, it forces the client to make **N additional requests** — one per room — to retrieve any useful data. This is known as the N+1 problem and significantly increases latency and server load, especially when the client ultimately needs data for all rooms.

**Returning full objects** in a single response requires more bandwidth per call, but eliminates all follow-up requests. The client receives everything it needs in one round trip, which reduces total latency and simplifies client-side code.

In this implementation, `GET /api/v1/rooms` returns full room objects with a `count` field. This is the preferred approach for list endpoints where clients are likely to need the full data. For very large datasets, pagination should be added (e.g., `?page=1&size=20`) to control payload size without reverting to ID-only responses.

---

### Part 2.2 — Is DELETE Idempotent?

In the **theoretical RFC definition**, an idempotent operation produces the same server state regardless of how many times it is called. By this definition, DELETE is idempotent: whether you delete a room once or ten times, the end state is the same — the room does not exist.

In **this specific implementation**, the first DELETE returns `200 OK`, while subsequent calls to the same endpoint return `404 Not Found`. The HTTP status code differs between calls, but the **underlying resource state does not** — the room is absent in both cases.

This is a common and accepted implementation. RFC 7231 explicitly states that idempotency refers to the effect on the server state, not the response code. Therefore, DELETE is considered idempotent in this implementation. The varying status code (200 vs 404) is informational — it tells the client whether the deletion was performed or was already in the desired state — but does not violate the idempotency principle.

---

### Part 3.1 — Effect of @Consumes(APPLICATION_JSON) Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells the JAX-RS runtime that the method **only accepts requests with a `Content-Type: application/json` header**.

If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS will attempt to find a resource method that can consume that media type. If no matching method is found, it **automatically returns HTTP 415 Unsupported Media Type** — the annotated method is never invoked. The developer does not need to write any validation logic for this case.

This is a key advantage of declarative content negotiation in JAX-RS: the framework handles format mismatch at the dispatch layer, before business logic is reached, keeping resource methods clean and focused.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

The query parameter approach (`GET /api/v1/sensors?type=CO2`) is generally preferred over a path-based approach (`GET /api/v1/sensors/type/CO2`) for the following reasons:

**Semantic clarity**: A URL path segment implies a unique, addressable resource. `/sensors/type/CO2` could be misread as a specific resource with the ID `CO2` under a sub-collection called `type`. Query parameters, by convention, communicate optional constraints or filters on a collection — their intent is immediately understood.

**Optionality**: Query parameters are naturally optional. A client can call `GET /api/v1/sensors` without a filter, or append `?type=CO2` to narrow results. With path segments, the same behaviour requires two separate endpoint definitions.

**Multiple filters**: Query parameters compose easily — `?type=CO2&status=ACTIVE` adds a second filter with no change to the URL structure. Achieving the same with path segments would require a combinatorial explosion of routes.

**REST conventions**: Filtering, sorting, and pagination are universally implemented via query parameters in REST API design guidelines (including Google, GitHub, and Stripe APIs). Following this convention makes the API immediately familiar to developers.

---

### Part 4.1 — Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern allows a resource method (without an HTTP verb annotation) to **delegate further path matching to a separate class**. In this project, `SensorResource` handles `/api/v1/sensors/{sensorId}` and delegates `/readings` to `SensorReadingResource`.

The architectural benefits are:

**Separation of concerns**: Each class has a single responsibility. `SensorResource` manages sensor lifecycle; `SensorReadingResource` manages reading history. Neither needs to know about the other's internal logic.

**Maintainability**: In a large API, defining every nested path in one "god class" makes the file enormous and difficult to read, test, or modify. Sub-resources keep class sizes manageable.

**Reusability**: A sub-resource class can, in principle, be reused by multiple parent resources without duplication.

**Independent testability**: Each sub-resource can be unit-tested in isolation by constructing it directly (as seen in `new SensorReadingResource(sensorId)`), without needing the full JAX-RS runtime.

**Contextual injection**: The locator passes the `sensorId` to the sub-resource constructor at the point of delegation. The sub-resource always has the correct context and never needs to re-parse the path itself.

---

### Part 5.2 — Why HTTP 422 is More Accurate Than 404 for a Bad roomId Reference

HTTP **404 Not Found** communicates that the **requested URL endpoint could not be found**. When a client POSTs to `/api/v1/sensors`, that URL is valid and the endpoint exists — a 404 would be misleading and incorrect.

HTTP **422 Unprocessable Entity** communicates that the **request was syntactically correct and the endpoint was found**, but the server cannot process it because the **semantic content of the body is invalid**. In this case, the request body is valid JSON with the correct fields, but the `roomId` value references a resource that does not exist. The error is a business logic / referential integrity problem inside the payload, not a routing problem.

Using 422 gives the client precise, actionable information: "your URL was right, your JSON was well-formed, but a field value inside it is logically invalid." A client receiving 404 might incorrectly assume it has the wrong URL and begin debugging the wrong thing.

---

### Part 5.4 — Security Risks of Exposing Java Stack Traces

Exposing raw Java stack traces in API responses is a significant security vulnerability for several reasons:

**Package and class structure disclosure**: Stack traces reveal the internal package hierarchy (e.g., `com.smartcampus.storage.DataStore`), giving attackers a map of the application's architecture that can guide targeted exploits.

**Library and version fingerprinting**: Stack traces include third-party library names and versions (e.g., `org.glassfish.jersey 3.1.3`). Attackers can cross-reference these against public CVE databases to find known vulnerabilities in the exact version deployed.

**Logic flow revelation**: The call stack shows the exact sequence of method calls that led to the error. This can reveal business logic, conditional branches, and data access patterns that help an attacker craft inputs to trigger specific error conditions or bypass validation.

**File system path exposure**: Stack traces often include absolute file paths to `.java` or `.class` files on the server, which can reveal the deployment structure and aid directory traversal attacks.

**Database schema leakage**: In applications using ORM or JDBC, stack traces frequently include SQL queries, table names, and column names — directly exposing the database schema.

The correct approach (implemented in this project's `GlobalExceptionMapper`) is to log the full stack trace server-side for developer diagnostics, while returning only a generic, non-revealing error message to the client.

---

### Part 5.5 — Why Filters Are Better Than Manual Logger Statements

Using a JAX-RS `ContainerRequestFilter` / `ContainerResponseFilter` for logging is superior to inserting `Logger.info()` calls into every resource method for the following reasons:

**Single point of change**: If the log format needs updating (e.g., adding a correlation ID or timing information), the change is made once in the filter class and immediately applies to every endpoint in the API.

**Guaranteed coverage**: Manual logging requires developers to remember to add it to every new method. A filter is applied automatically to all matched requests — new endpoints are logged without any additional effort.

**Separation of concerns**: Resource methods should contain only business logic. Cross-cutting concerns like logging, authentication, and metrics belong in filters. This makes resource classes cleaner, shorter, and easier to read and test.

**Consistency**: All log entries produced by the filter share the same format and log level, making log parsing and monitoring tools more reliable.

**Easy toggling**: Logging can be disabled by removing the `@Provider` annotation or deregistering the filter, with zero changes to business code. With manual logging, every resource method would need to be modified.

This is the same principle behind middleware in frameworks like Express.js or aspect-oriented programming — apply infrastructure concerns once, universally.

---

## Project Structure

```
src/main/java/com/smartcampus/
├── Main.java                          # Starts Grizzly embedded server
├── SmartCampusApplication.java        # @ApplicationPath("/api/v1")
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── storage/
│   └── DataStore.java                 # Thread-safe static ConcurrentHashMaps
├── resource/
│   ├── DiscoveryResource.java         # GET /api/v1
│   ├── RoomResource.java              # /api/v1/rooms
│   ├── SensorResource.java            # /api/v1/sensors
│   └── SensorReadingResource.java     # sub-resource: /{sensorId}/readings
├── exception/
│   ├── RoomNotEmptyException.java
│   ├── LinkedResourceNotFoundException.java
│   ├── SensorUnavailableException.java
│   └── mapper/
│       ├── RoomNotEmptyExceptionMapper.java          # → 409
│       ├── LinkedResourceNotFoundExceptionMapper.java # → 422
│       ├── SensorUnavailableExceptionMapper.java      # → 403
│       └── GlobalExceptionMapper.java                 # → 500
└── filter/
    └── LoggingFilter.java             # Request + Response logging
```

---

## Technology Stack

- **Language**: Java 11
- **Framework**: JAX-RS via Jersey 3.1.3
- **Server**: Grizzly2 embedded HTTP server
- **JSON**: Jackson (via jersey-media-json-jackson)
- **Build**: Maven with Shade plugin (fat jar)
- **Storage**: In-memory `ConcurrentHashMap` / `ArrayList`
