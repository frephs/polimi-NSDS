
# Node-RED Exercises Repository

This repository contains exercises and examples utilizing **Node-RED**, a visual tool designed to build applications across the computing continuum. Node-RED was originally developed by IBM to act as a "glue" connecting the Internet of Things (IoT) to other systems, and is now an open-source tool with a significant user base.

## Programming Paradigm

Node-RED adopts a **"flow-based" programming model**, often considered yet another variation of **data flow programming**.

### Core Principles
1.  **Directed Graph Structure:** Node-RED programs are encoded as a **directed graph of operators** (nodes) with **data flowing in between** them.
2.  **Visual and Component-Based:** The application is built visually using a **Node palette**, a **Flow area**, and nodes that can be deployed.
3.  **Event-Driven and Asynchronous:** Node-RED is built upon Node.js, utilizing a **non-blocking, event-driven model**. Flows execute **asynchronously**. The execution of individual nodes is asynchronous and **scheduled non-deterministically**, meaning if multiple nodes receive messages simultaneously, it is generally impossible to predict which one will execute first.
4.  **Shared Characteristics:** The model shares features with **functional programming** (but includes the ability to reason on local and global context) and the **actor model**, where nodes function as **reactive actors** and data flows as messages.

## Key Features

### 1. Flow Structure and Messages

Node-RED flows are composed of **Input nodes** (typically starting a flow), **Output nodes** (typically ending a flow), and **Processing nodes**. Flows can be saved and loaded as **JSON**.

**Messages** are the fundamental unit of data transfer between nodes and are specified as **JavaScript objects**. They contain three fundamental properties:
*   ***payload***: The actual message content, which can be any JavaScript data type, including objects.
*   ***topic***: A description of the channel the message belongs to, characterizing the message content.
*   ***messageId***: A unique identifier within a running Node-RED instance.

### 2. Extensibility and Node Types

Node-RED provides a set of **built-in nodes**. The architecture is **highly extensible**, and the functionality can be extended by adding new nodes, often provided by the community through a vast catalogue.

### 3. Function Nodes (Custom Logic)

For custom logic implementation, **Function nodes** are provided. These are generic containers for **Plain JavaScript code**.
*   **Input/Output:** Function nodes receive a message object as input and generate one or more message objects as output. The default action is to return the received message.
*   **Patterns:** They allow users to override the incoming message payload or **slice the payload** (e.g., extracting a specific key like `temperature` from an incoming object payload).
*   **Flow Control:** Function nodes can determine where messages flow by using JavaScript arrays in their return statement, corresponding to the number of configured outputs (a `null` value generates no message on that output).
*   **Multi-Message Output:** They can output multiple messages sequentially by returning a list (JavaScript array) of message objects.
*   **Lifecycle Hooks:** Code can be specified to execute when the flow is deployed, every time a message is received, or when the flow stops.

### 4. Data Sharing and Context Management

Node-RED applications often require data to be shared across different flow executions, different blocks, or entirely different flows.

*   **Files:** External storage via files (or databases) is one way to achieve data persistence, especially if state needs to be retained across deployment actions.
*   **Node Context:** A special context object exists for Function nodes (`context`) to **retain state across invocations**. This context applies only to the individual node. *Important:* The node context is **re-initialized** when the flow is deployed.
*   **Flow and Global Context:** Context modules also exist for sharing data across an entire flow (`flow`) or globally across all flows in the same Node-RED instance (`global`). These contexts are accessed in JavaScript code using the respective keyword. These also suffer from the same initialization limitations upon deployment as node context.

### 5. Networking Capabilities

Node-RED offers regular networking abstractions, allowing applications to interact with other Node-RED installations or different platforms.

Networking functionalities available include:
*   UDP/TPC sockets (mainly for pair-wise interactions).
*   HTTP/WebSockets (for application-level interactions).
*   MQTT (mainly for many-to-many interactions).

#### Data Serialization
Because the network is a pipe that transfers serial data, complex JavaScript data structures used in Node-RED must be serialized when traversing a network. **JSON** (JavaScript Object Notation) is a widely used open data interchange format for this purpose. Node-RED includes a built-in node to convert data to/from JavaScript objects and JSON representations.

#### UDP Sockets
The **User Datagram Protocol (UDP)** is a lightweight protocol for sending small messages.
*   It requires no connection setup but provides **no guarantees** (messages may be lost or arrive out of order).
*   UDP messages are sent or received by specifying a port number (and a destination IP address when sending).
*   Node-RED provides specific nodes for sending and receiving UDP messages, where the content is derived from the `msg.payload`.

#### MQTT (Message Queuing Telemetry Transport)
MQTT is an extremely lightweight, topic-based, **Publish/Subscribe (Pub/Sub)** messaging layer.
*   **Model:** Pub/Sub decouples data producers (publishers) and consumers (subscribers); communication is **data-centric** rather than address-centric. Subscriptions are often topic-based (hierarchically arranged channels, e.g., `iot/building21/temperature`).
*   **Nodes:** Nodes are provided to subscribe to topics (input) or publish messages (output).
*   **Serialization in MQTT:** MQTT handles messages as Strings. When publishing, JavaScript objects are automatically converted to JSON, eliminating the need for an explicit JSON conversion node. When receiving, the output can often be converted directly back to a JavaScript object.
*   **Quality of Service (QoS):** MQTT supports per-message guarantees on delivery: QoS0 (at most once), QoS1 (at least once), and QoS2 (at most once).
*   **Integration:** MQTT is frequently used as a basis for integrating diverse data sources.

***

**Analogy for Node-RED's Paradigm:**

The flow-based programming model of Node-RED is like a complex assembly line in a factory. Each *node* is a specialized machine (operator) waiting for material (the *message*) to arrive. When the material arrives, the machine processes it (using custom *JavaScript* or a built-in function) and sends the processed material along a specific conveyor belt (the *flow* connection) to the next machine. Since the whole factory runs asynchronously, you can't always predict which machine will finish its work first if multiple inputs arrive simultaneously, but the structure ensures the data moves logically from input to output.

---

## 6. Practical Examples and Code Patterns

### 6.1 Function Node Patterns

#### Basic Message Transformation
```javascript
// Access incoming message
let temperature = msg.payload.temperature;
let humidity = msg.payload.humidity;

// Create new payload
msg.payload = {
    temp_f: temperature * 9/5 + 32,  // Convert to Fahrenheit
    humidity: humidity,
    timestamp: new Date().toISOString()
};

return msg;
```

#### Filtering Messages
```javascript
// Only pass messages that meet criteria
if (msg.payload.temperature > 25) {
    return msg;  // Pass message through
} else {
    return null; // Discard message
}
```

#### Splitting to Multiple Outputs
```javascript
// Configure node with 3 outputs
// Return array with message for each output (null = no output)

let temp = msg.payload.temperature;

if (temp > 30) {
    return [msg, null, null];  // Send to output 1 (hot)
} else if (temp > 20) {
    return [null, msg, null];  // Send to output 2 (warm)
} else {
    return [null, null, msg];  // Send to output 3 (cold)
}
```

#### Sending Multiple Messages
```javascript
// Send multiple messages from single input
let messages = [];

for (let i = 0; i < 5; i++) {
    messages.push({
        payload: {
            index: i,
            value: msg.payload.value * i
        }
    });
}

return [messages];  // Array of messages to output 1
```

#### Using Context to Store State
```javascript
// Store value in node context
let count = context.get('count') || 0;
count++;
context.set('count', count);

msg.payload = {
    count: count,
    original: msg.payload
};

return msg;
```

```javascript
// Use flow context (shared across flow)
flow.set('lastTemperature', msg.payload.temperature);
let previousTemp = flow.get('lastTemperature');

// Use global context (shared across all flows)
global.set('systemStatus', 'running');
let status = global.get('systemStatus');
```

### 6.2 Working with JSON

#### Parse JSON String to Object
```javascript
// If msg.payload is a JSON string
try {
    msg.payload = JSON.parse(msg.payload);
    return msg;
} catch (e) {
    msg.payload = { error: "Invalid JSON" };
    return msg;
}
```

#### Convert Object to JSON String
```javascript
// If msg.payload is an object
msg.payload = JSON.stringify(msg.payload);
return msg;
```

### 6.3 MQTT Patterns

#### Publishing with Topic
```javascript
// Set topic dynamically in function node
msg.topic = "sensors/building1/temperature";
msg.payload = {
    value: 23.5,
    unit: "celsius",
    timestamp: Date.now()
};
return msg;
// Connect to MQTT Out node
```

#### Subscribing with Wildcards
```
Topic patterns:
- sensors/+/temperature     (+ matches single level)
- sensors/#                 (# matches multiple levels)
- sensors/building1/#       (all sensors in building1)
```

#### Quality of Service (QoS) Levels
```
QoS 0: At most once (fire and forget)
QoS 1: At least once (acknowledged delivery)
QoS 2: Exactly once (guaranteed delivery)
```

### 6.4 HTTP Request/Response Patterns

#### Making HTTP Requests
```javascript
// Prepare HTTP request
msg.url = "https://api.example.com/data";
msg.method = "POST";
msg.headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer " + flow.get('apiToken')
};
msg.payload = {
    sensor: "temp1",
    value: 25.3
};
return msg;
// Connect to HTTP Request node
```

#### Handling HTTP Response
```javascript
// Process response from HTTP Request node
if (msg.statusCode === 200) {
    let data = msg.payload;
    // Process successful response
    msg.payload = {
        success: true,
        data: data
    };
} else {
    msg.payload = {
        success: false,
        error: msg.statusCode
    };
}
return msg;
```

#### Creating HTTP Endpoints
```
HTTP In node → Function (process request) → HTTP Response node

Function node example:
```
```javascript
// Access request data
let method = msg.req.method;
let params = msg.req.params;
let query = msg.req.query;
let body = msg.payload;

// Create response
msg.payload = {
    message: "Request processed",
    receivedData: body
};

msg.statusCode = 200;
return msg;
```

### 6.5 UDP Communication

#### Sending UDP Messages
```javascript
// Prepare UDP message
msg.payload = JSON.stringify({
    sensor: "temp1",
    value: 25.3,
    timestamp: Date.now()
});

msg.ip = "192.168.1.100";  // Optional: override destination
msg.port = 5000;           // Optional: override port

return msg;
// Connect to UDP Out node
```

#### Receiving UDP Messages
```
UDP In node → Function (process) → Output

Function node example:
```
```javascript
// msg.payload contains the UDP message (typically string)
try {
    let data = JSON.parse(msg.payload);
    msg.payload = {
        received: data,
        from: msg.ip + ":" + msg.port
    };
} catch (e) {
    msg.payload = {
        raw: msg.payload,
        error: "Not JSON"
    };
}
return msg;
```

### 6.6 Working with Time and Timers

#### Inject Node with Intervals
```
Configure Inject node:
- Repeat: interval (every X seconds/minutes)
- Between specific times
- At specific time of day
- On specific days
```

#### Delay and Rate Limiting
```
Delay node configurations:
- Delay each message by X seconds
- Rate limit: 1 msg per X seconds
- Delay until (specific time)
- Random delay between X and Y seconds
```

#### Timestamp Handling
```javascript
// Current timestamp
msg.payload.timestamp = Date.now();  // Unix timestamp (ms)
msg.payload.isoTime = new Date().toISOString();  // ISO 8601 format

// Parse timestamp
let date = new Date(msg.payload.timestamp);
msg.payload.hour = date.getHours();
msg.payload.day = date.getDay();
msg.payload.formatted = date.toLocaleString();
```

### 6.7 Data Aggregation and Buffering

#### Buffering Messages
```javascript
// Store messages in context until threshold
let buffer = context.get('buffer') || [];
buffer.push(msg.payload);
context.set('buffer', buffer);

// Send batch when size reached
if (buffer.length >= 10) {
    msg.payload = buffer;
    context.set('buffer', []);  // Reset buffer
    return msg;
} else {
    return null;  // Don't send yet
}
```

#### Computing Averages
```javascript
// Maintain running average
let values = flow.get('values') || [];
values.push(msg.payload.temperature);

// Keep last 10 values
if (values.length > 10) {
    values.shift();
}
flow.set('values', values);

// Calculate average
let avg = values.reduce((a, b) => a + b, 0) / values.length;

msg.payload = {
    current: msg.payload.temperature,
    average: avg.toFixed(2),
    count: values.length
};

return msg;
```

### 6.8 Error Handling

#### Try-Catch Pattern
```javascript
try {
    // Potentially problematic operation
    let result = JSON.parse(msg.payload);
    msg.payload = result;
    return msg;
} catch (error) {
    // Send to error output (configure node with 2 outputs)
    msg.payload = {
        error: error.message,
        original: msg.payload
    };
    return [null, msg];  // Send to output 2
}
```

#### Validation Pattern
```javascript
// Validate required fields
function validate(data) {
    if (!data.temperature) return "Missing temperature";
    if (!data.sensor_id) return "Missing sensor_id";
    if (typeof data.temperature !== 'number') return "Temperature must be number";
    if (data.temperature < -50 || data.temperature > 100) return "Temperature out of range";
    return null;  // Valid
}

let error = validate(msg.payload);
if (error) {
    msg.payload = { error: error };
    return [null, msg];  // Invalid - send to output 2
} else {
    return [msg, null];  // Valid - send to output 1
}
```

---

## 7. Common Node Types Reference

### Input Nodes
- **Inject**: Manual trigger or scheduled execution
- **HTTP In**: Create HTTP endpoints
- **MQTT In**: Subscribe to MQTT topics
- **TCP In / UDP In**: Network socket input
- **File In**: Read files from filesystem
- **Serial In**: Read from serial port

### Output Nodes
- **Debug**: Display messages in debug panel
- **HTTP Response**: Send HTTP responses
- **MQTT Out**: Publish to MQTT topics
- **TCP Out / UDP Out**: Network socket output
- **File Out**: Write to filesystem
- **Email**: Send email messages

### Function Nodes
- **Function**: Custom JavaScript code
- **Switch**: Route based on conditions
- **Change**: Modify message properties
- **Template**: Generate text/HTML/JSON
- **JSON**: Parse/stringify JSON
- **CSV**: Parse/generate CSV

### Processing Nodes
- **Filter**: Remove duplicate messages
- **Delay**: Delay or rate-limit messages
- **Trigger**: Send follow-up messages
- **Join**: Combine message sequences
- **Split**: Split messages/arrays
- **Sort**: Sort message sequences
- **Batch**: Batch messages together

---

## 8. Best Practices

1. **Message Structure**: Always maintain `msg.payload` for data, use `msg.topic` for categorization
2. **Error Handling**: Use try-catch and multiple outputs for error paths
3. **Context Management**: Clear context when no longer needed to avoid memory leaks
4. **Asynchronous Operations**: Use callbacks or promises for async operations
5. **Debugging**: Use Debug nodes liberally during development
6. **Documentation**: Add Comment nodes to explain complex flows
7. **Modularity**: Break complex flows into subflows for reusability
8. **Performance**: Avoid blocking operations in Function nodes
9. **Security**: Never hardcode credentials - use environment variables or context
10. **Testing**: Test flows with various input scenarios before deployment

---

## 9. Quick Command Reference

### Starting Node-RED
```bash
node-red                    # Start Node-RED
node-red -v                 # Verbose logging
node-red -s settings.js     # Custom settings file
```

### Installing Nodes
```bash
npm install node-red-contrib-[package-name]
# Or use Manage Palette in UI
```

### Common MQTT Test Commands
```bash
# Publish to MQTT
mosquitto_pub -h localhost -t "test/topic" -m "Hello World"

# Subscribe to MQTT
mosquitto_sub -h localhost -t "test/#" -v

# Subscribe with QoS
mosquitto_sub -h localhost -t "test/topic" -q 2
```

### Testing HTTP Endpoints
```bash
# GET request
curl http://localhost:1880/endpoint

# POST request with JSON
curl -X POST http://localhost:1880/endpoint \
  -H "Content-Type: application/json" \
  -d '{"key":"value"}'
```