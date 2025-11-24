
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