
### 1. Introduction to Event-Based Communication (EBC)

Apache Kafka is middleware designed for **asynchronous event-based communication**. EBC is a widely adopted architectural style for building large-scale distributed systems.

In EBC, system components interact exclusively by exchanging messages or events.

#### 1.1 Key Roles

Components in an EBC system assume one of two primary roles:

1.  **Producers (Publishers):** Components that publish notifications whenever events occur.
    *   *Example:* A security camera producing an event every time a person enters a room.
2.  **Consumers (Subscribers):** Components that subscribe to events they are interested in.
    *   *Example:* A security dashboard subscribing to all security-related events, including those from cameras.

Crucially, producers and consumers **need not know each other**. Communication is always mediated by an external service, such as Kafka.

#### 1.2 Benefits of EBC

The popularity of EBC stems from several decoupling properties it offers:

*   **Space Decoupling:** Producers and consumers are unaware of each other's existence. This allows new components (e.g., new sensors or publishers) to be dynamically added without needing to restart or reconfigure the entire system or any existing components.
*   **Synchronization Decoupling:** Producers are not blocked while generating events, and consumers are notified asynchronously. Producers do not wait for consumers to finish consuming messages, which promotes **scalability** by removing explicit dependencies.
*   **Time Decoupling:** If the middleware service (like Kafka) can **persist events**, producers and consumers do not need to be connected at the same time. If a consumer goes down, events are persisted; when it comes back online, it contacts Kafka and reads the messages it missed.

#### 1.3 EBC in Microservices

EBC is particularly advantageous when building applications using a **microservices architecture**, where applications are split into independent services, each managing its own local state (e.g., a local database).

**Transforming Commands (Write Path):**
Traditionally, a service (e.g., Orders) might invoke a command (a state-altering operation) directly on another service (e.g., Shipping). Using EBC, this synchronous command is transformed into an event (e.g., a new order event) registered on a Kafka topic (e.g., `order topic`). This decouples the Orders service, which no longer needs to know which services are processing the orders.

**Reversing Queries (Read Path):**
Synchronous request/response interactions (queries) between services (e.g., Shipping querying Customer for an address) can negatively affect latency. EBC solves this by reversing the interaction: the Shipping service maintains a local replica of the customer address data it needs. When the Customer service changes details, it places a notification (an event) into a `customer updates topic`. The Shipping service consumes this topic and updates its local view.

This approach promotes independence but trades consistency (there may be temporary inconsistency between databases) for performance and decoupling.

**Associated Design Principles:**

1.  **Event Sourcing (ES):** This principle dictates that **events become the core elements, the source of truth**. Instead of relying solely on the current state, the state of any component can be reconstructed by replaying the sequence of past events from the log.
2.  **Command Query Responsibility Segregation (CQRS):** This principle separates the **write path (commands)** from the **read path (queries)**, connecting them using an asynchronous communication channel like an event log.

---

### 2. Apache Kafka Framework Overview

Kafka is the standard platform for event-based communication today. Over 80% of Fortune 100 companies trust and use Kafka, including LinkedIn, Netflix, and The New York Times.

#### 2.1 History and Architecture

Kafka was originally developed at LinkedIn to efficiently handle logs of their distributed applications. It was open-sourced in 2011 and became an Apache project in 2012.

A Kafka system consists of a cluster of multiple servers, also called **brokers**, which collectively offer storage and messaging capabilities.

#### 2.2 Topics and Partitions

Kafka uses **topic-based communication**. Messages (or records, or events) are grouped by topics, which represent a stream of messages.

*   **Messages:** The basic unit of data is a message, defined as a key-value pair. All data is stored as **byte arrays**, and client producers provide the code (serializers) to convert data structures into these byte arrays.
*   **Topics are Partitioned:** A single topic is split into multiple **partitions**. Each partition contains a subset of the topic's messages.
*   **Partitions as Logs:** Each partition is an **ordered, immutable log of messages**. Messages are identified by a monotonically increasing integer value called the **offset**. The offset reflects the First-In, First-Out (FIFO) order in which messages were added by a producer to that partition.
*   **Distribution and Scalability:** Partitions are distributed across brokers. This allows Kafka to scale beyond the storage capacity of a single node.
*   **Partitioning Strategy:** The message **key** is used to determine which partition a message is assigned to. Messages with the same key are guaranteed to be stored in the same partition, ensuring that these related messages maintain their order. Producers can specify a custom partitioner class for **semantic partitioning** or rely on the default strategy, which uses a hash of the key to achieve load balancing.

#### 2.3 Replication and Fault Tolerance

Partitions can be replicated across brokers to provide **fault tolerance**.

*   Kafka automatically handles replication.
*   For each partition, there is one **leader replica** and zero or more **followers**.
*   All read and write requests go exclusively to the **leader replica**, which then propagates changes to the followers.
*   A topic replicated across $N$ brokers (replication factor $N$) can tolerate the failure of $N-1$ brokers without losing information.

#### 2.4 Consumer Offset Management and Groups

Consumers **pull** messages from brokers when they are ready, contrasting with older push-based systems.

*   **Offset Tracking:** The **consumer offset** keeps track of the latest message read for a given partition.
*   **Consumer Responsibility:** A key design choice is that the consumer is responsible for storing and managing its own offset, not the brokers. This design enables brokers to avoid storing state that grows with the number of consumers, contributing to scalability.
*   **Consumer Groups:** Multiple physical consumers can be combined into a **consumer group**, which conceptually represents a single logical application.
    *   **Scaling:** Consumers within a group are assigned a subset of the topic's partitions, allowing them to read and process messages in **parallel**.
    *   **Delivery:** Each message is delivered to *each* interested consumer group. Within that group, the message is delivered to *only one* consumer.
    *   *Limitation:* The number of useful consumers in a group is capped by the number of partitions in the topic.

#### 2.5 Cluster Management

Kafka traditionally relies on **Apache Zookeeper** to manage the distributed broker cluster. Zookeeper provides necessary protocols for cluster membership, failure detection, topic configuration agreement, and leader election. Recent versions of Kafka are transitioning to an internal alternative known as **KRaft** to offer these same functionalities.

---

### 3. Kafka Producer and Consumer Implementation (Code Examples)

The following examples outline the core configurations and logic required to implement basic Kafka producers and consumers using the Java API, as detailed in the source materials [77–93].

#### 3.1 Basic Producer Implementation

A producer must be configured with connection details (`bootstrap.servers`) and serializers for keys and values.

| Concept | Explanation 
| :--- | :--- |
| **KafkaProducer** | The class used to instantiate the producer, generic over Key and Value types | |
| **bootstrap.servers** | Defines the list of brokers the producer connects to (e.g., `localhost:9092`) | |
| **Serializers** | Code provided by the client to convert data structures (keys/values) into byte arrays (e.g., `StringSerializer`) | |
| **producer.send()** | Method used to publish a `ProducerRecord` (containing key, value, and target topic) to Kafka | |

**Code Example: Basic Producer Configuration and Send**

```java
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

// 1. Define configuration properties
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// 2. Instantiate the producer
KafkaProducer<String, String> producer = new KafkaProducer<>(props);

String topicName = "TopicA";
String key = "Key-X";
String value = "Sequential-Value-1";

// 3. Create and send the record
ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, value);

// The send method returns a Future containing metadata (partition, offset)
producer.send(record); 

// Close producer when done
producer.close();

// Note: New topics are created automatically when a producer first publishes to them
```

#### 3.2 Basic Consumer Implementation

A consumer must define connection details, deserializers, and must belong to a consumer group.

| Concept | Explanation | 
| :--- | :--- |
| **KafkaConsumer** | The class used to instantiate the consumer, generic over Key and Value types | |
| **Group ID (`group.id`)** | A unique identifier specifying the consumer group the consumer belongs to. Consumers with the same ID belong to the same logical consumer. | |
| **Deserializers** | Used to convert byte arrays received from Kafka back into structured data (e.g., `StringDeserializer`) | |
| **consumer.subscribe()** | Expresses interest in a list of topics. | |
| **consumer.poll()** | A blocking method used to pull new messages. It returns records or times out. | |
| **`enable.auto.commit`** | If set to `true` (default), offsets are updated periodically, rather than manually per message. | |
| **`auto.offset.reset`** | Determines where a *new* consumer group starts reading. Options are `latest` (only new messages after the first poll) or `earliest` (all messages stored on Kafka). | |

**Code Example: Basic Consumer Configuration and Poll Loop**

```java
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

// 1. Define configuration properties
Properties props = new Properties();
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ConsumerConfig.GROUP_ID_CONFIG, "ConsumerGroupA");
props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

// Optional: Configure offset reset behavior for new consumers
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
// Optional: Enable or disable automatic offset committing
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

// 2. Instantiate and subscribe
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Collections.singletonList("TopicA"));

// 3. Start the poll loop
while (true) {
    // Poll for records, blocking for a maximum duration (e.g., 5 minutes)
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMinutes(5));

    for (ConsumerRecord<String, String> record : records) {
        // Process the record (key, value, partition, offset are available)
        System.out.printf("Offset = %d, Key = %s, Value = %s%n", 
                          record.offset(), record.key(), record.value());
    }
    
    // If auto commit is false, manual commit would happen here
    // consumer.commitSync();
}
```

---

### 4. Advanced Guarantees and Exactly Once Semantics (EOS)

Kafka can be configured to offer strong guarantees regarding message delivery, specifically **Exactly Once Semantics (EOS)**.

#### 4.1 Communication Semantics

The theoretical guarantees for message delivery are:

1.  **At Most Once:** Messages are never duplicated, but they may be lost.
2.  **At Least Once:** Messages are never lost, but they may be delivered more than once (duplicates).
3.  **Exactly Once Semantics (EOS):** The system behaves as if each message was delivered once and only once.

#### 4.2 Implementing EOS in Kafka

Achieving EOS in Kafka involves ensuring transactional integrity across two steps: Producer-to-Broker, and Broker-to-Consumer.

##### A. EOS: Producer to Broker (Idempotence)

To prevent message duplication during transport, the producer can be configured to be **idempotent**. An idempotent operation can be performed multiple times without causing a different effect than performing it once.

The implementation relies on the producer attaching a unique sequence number to each message. If the producer fails to receive an acknowledgement (ACK) and resends the message, the broker uses the sequence number to identify and **discard duplicates**.

**Code Example: Idempotent Producer Configuration**

```java
import org.apache.kafka.clients.producer.ProducerConfig;

// Configuration to enable idempotence
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"); 

// Instantiate producer as before
KafkaProducer<String, String> idempotentProducer = new KafkaProducer<>(props);
```

##### B. EOS: Consumer Processing (Transactional Writes)

EOS on the consumer side requires that the side effects (results) of processing a message are produced once and only once. This is achieved when all results are written back into Kafka topics, which are reliable through replication.

Kafka enables this using **transactional writes**, guaranteeing that multiple writes are atomic (either all or none are written).

**End-to-End EOS Implementation:**
The consumer (or processing component, like a forwarder) must perform two critical actions within a single transaction:

1.  Write the processing results to the output topic(s).
2.  Advance the consumer offsets for the input topic(s).

This ensures that if the service fails, either both the results are written and the offsets updated, or neither occurs, preventing lost or duplicated processing.

**Code Example: Atomic Forwarder Logic**

The forwarder acts as both a consumer (reading from Input Topic A) and a producer (writing to Output Topic B). It requires a unique `transactional.id`.

```java
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;

// 1. Setup Producer with Transactional ID
Properties producerProps = new Properties();
producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-transactional-id");

KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

// Setup Consumer with read_committed isolation and auto-commit disabled
Properties consumerProps = new Properties();
consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "forwarder-group");
consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
consumer.subscribe(Collections.singletonList("TopicA"));

// 2. Main processing loop
producer.initTransactions(); 

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

    if (!records.isEmpty()) {
        try {
            producer.beginTransaction();

            for (ConsumerRecord<String, String> record : records) {
                // Step A: Process the message and write to output topic (Topic B)
                String processedValue = record.value().toUpperCase(); // Example processing
                ProducerRecord<String, String> outputRecord = new ProducerRecord<>("TopicB", record.key(), processedValue);
                producer.send(outputRecord);
            }

            // Step B: Manually commit the read offsets within the transaction
            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();
            for (ConsumerRecord<String, String> record : records) {
                offsetsToCommit.put(
                    new TopicPartition(record.topic(), record.partition()),
                    new OffsetAndMetadata(record.offset() + 1)
                );
            }

            // Send offsets to the transaction coordinator
            producer.sendOffsetsToTransaction(offsetsToCommit, consumer.groupMetadata());

            producer.commitTransaction();
        } catch (Exception e) {
            producer.abortTransaction();
        }
    }
}
```

##### C. Isolation Levels

When using transactions, consumers can choose their level of isolation:

*   **Read Committed:** Consumers read only those writes that have been successfully committed as part of an atomic transaction. **This is necessary to maintain EOS.**
*   **Read Uncommitted:** Consumers read all messages immediately, even if they have not yet been committed by a transaction coordinator. This sacrifices EOS for improved performance/latency.

**Code Example: Consumer Isolation Level Configuration**

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;

// To ensure EOS (default configuration):
props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); 

// To read all messages immediately, including uncommitted ones:
props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_uncommitted");
```

#### 4.3 Log Compaction

To support services that store their state or side effects within Kafka logs, Kafka offers **log compaction**.

Log compaction is a special **retention policy** configurable per topic, designed for scenarios where new events have **update semantics** (i.e., they overwrite previous state associated with a key).

Instead of storing all historical messages for a configurable time (the default behavior), log compaction allows Kafka to **only keep the last value for each key**. Older messages with the same key are deleted, reducing storage usage while preserving the order and offset of the remaining messages. This procedure runs periodically and typically operates on the older "tail" of the log to minimize performance impact on active producers and consumers.

---
*Analogy:* Think of Kafka's log compaction like a notebook where you only care about the final, authoritative version of each entry. If you write "Key 1 = 5," then later write "Key 1 = 10," and finally "Key 1 = 15," the log compaction process removes the first two entries, leaving only "Key 1 = 15." This keeps the record history lean while ensuring you always have the most recent truth associated with that key.
