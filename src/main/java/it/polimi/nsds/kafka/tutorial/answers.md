

### Exercise 1: Basic Consumers

This exercise sets up two independent consumers, C1 and C2, on the same topic. Assuming they are in *different* consumer groups (which is required for both to get all the messages), here's what happens.

* **What happens if one consumer fails?** 
    * If C1 fails, it simply stops printing records to the standard output.
    * If C2 fails, it stops processing messages and writing them to the new topic.
    * Because they are in separate groups (or are standalone consumers), **the failure of one has no effect on the other**. C1 will continue printing, or C2 will continue processing, unaware of the other's failure.

* **What happens if you restart it?** 
    * The prompt states this is "Without delivery guarantees", which implies you are likely using the default configuration (`enable.auto.commit=true`).
    * With auto-commit, the consumer's position (offset) is periodically committed in the background. When you restart the failed consumer, it will resume reading from the **last committed offset**.
    * This can lead to two scenarios:
        1.  **Message Loss (At-Most-Once):** The consumer processed a batch, auto-committed the offset, but crashed *before* it could print (C1) or write to the new topic (C2). Upon restart, it starts *after* that batch, and those messages are lost.
        2.  **Duplicate Processing (At-Least-Once):** The consumer processed a batch, printed/wrote the results, but crashed *before* the auto-commit could run. Upon restart, it reads from the *previous* offset and processes the same batch of messages again.

---

### Exercise 2: Scaling a Slow Consumer

Here, C2 is too slow, and you need to improve its performance.

* **How can you improve the performance of the system?** 
    * The standard Kafka way to parallelize processing is to **run multiple instances of the consumer (C2) within the same consumer group**.
    * For this to work, the input topic must have **multiple partitions**.
    * If the topic has, for example, 4 partitions, you can run up to 4 consumer instances in the `C2-group`. Kafka will automatically assign one partition to each consumer, allowing them to process the topic's data in parallel, thus increasing overall throughput.

* **What happens if one consumer fails?** 
    * When one consumer instance in the group fails, the Kafka broker detects it (via a missed heartbeat) and triggers a **consumer group rebalance**.
    * During a rebalance, Kafka revokes the partitions from the failed consumer and **re-assigns them to the remaining healthy consumers** in the group.
    * There will be a short pause in processing for those partitions, but the system automatically recovers. Processing continues, though the remaining consumers will now have a heavier load.

* **What happens if you start multiple consumers?** 
    * When you start a new consumer instance and it joins an existing group, this *also* triggers a **rebalance**.
    * Kafka will revoke some partitions from the *existing* consumers and assign them to the *new* consumer.
    * This is the desired behavior: the processing load is redistributed (or "balanced") among all available consumer instances, improving performance.

---

### Exercise 3: Exactly-Once Semantics

The goal is to make C2's "read-process-write" operation atomic, ensuring each input message results in an output message *once and only once*[cite: 23, 25]. This requires **Kafka Transactions**.

* **What happens if one consumer fails?** 
    * Using transactions, the consumer (C2) will atomically commit the *input offsets* and the *produced output messages* together.
    * If the consumer fails *before* committing the transaction:
        1.  The transaction times out and is **aborted**.
        2.  Any messages produced to the output topic as part of that transaction are marked as "aborted" and will *not* be visible to downstream consumers (assuming they are configured with `isolation.level=read_committed`).
        3.  The input offsets are *not* committed.
    * When the consumer (or a new one, after a rebalance) restarts, it will resume reading from the *last successfully committed offset*, effectively re-processing the batch that was part of the failed transaction. This ensures data is not lost.

* **What happens if you start multiple consumers?** 
    * The system scales just like in Exercise 2 (a rebalance occurs, partitions are distributed).
    * Kafka's transactional guarantees are designed to be safe even during rebalances. This prevents "zombie" consumers (a consumer that lost its partitions but hasn't realized it yet) from committing transactions. Each producer instance gets an "epoch," and the broker will reject commits from an instance with an older epoch, ensuring only the currently-assigned consumer can write data for a partition.

---

### Exercise 4: Stateful Processing

This is the most complex scenario. C2 must now *store* a running count for each key, which is a **stateful** operation.

* **Consider a single instance of C2:** 
    * **What happens in the case of failure?** 
        If you store the counts in the consumer's memory (e.g., in a `HashMap`), that **state is lost on failure**. When the consumer restarts, the map is empty. It will restart counting from zero (or 1), and the "overall number" will be incorrect.
    * **Does your implementation guarantee exactly-once semantics?** 
        **No.** Even if you use Kafka Transactions (Exercise 3), you are only guaranteeing the *Kafka* parts (input offset + output message). You are *not* atomically updating your local `HashMap` state as part of that transaction.
        * **Failure Scenario:** Your consumer reads a message ("Key A"), updates its in-memory map to `{'A': 10}`, produces the `('A', 10)` message, and then crashes *before* committing the transaction.
        * **On Restart:** The transaction is aborted. The consumer restarts, reads the *same* message for "Key A" again. But its in-memory map is *empty* (it was lost in the crash). It will create a new entry `{'A': 1}` and produce `('A', 1)`, which is completely wrong.

* **How do your answers change in the case of multiple instances?** 
    * The problems become **significantly worse**.
    * 1. Partitioned State:** Unless you ensure all messages for the *same key* go to the *same partition*, they will be processed by different consumer instances. Each instance will have its *own* in-memory `HashMap` with partial counts (e.g., C2-instance-1 has `{'A': 5}` and C2-instance-2 has `{'A': 7}`). Neither has the true "overall number".
    * 2. State is Lost on Rebalance:** When a consumer fails, its partitions are given to another consumer. That new consumer has *no access* to the in-memory state from the failed instance. It will start counting from scratch for all the keys in the partitions it just received.

> **Conclusion:** This exercise demonstrates the limits of basic consumers. This kind of stateful aggregation is precisely the problem that frameworks like **Kafka Streams** are built to solve, as they manage state, partition-awareness, and fault tolerance for you.