# Comprehensive Guide to The Actor Model and Akka

This README provides a comprehensive overview of the Actor Model, its fundamental concepts, and its practical implementation using the Akka framework on the JVM.

## 1. The Actor Model: Fundamentals

The Actor Model is a mathematical model of concurrent computation that treats **Actors as the universal primitives** of concurrent computation. The philosophy behind it suggests that since the world is parallel, programs designed to model real-world objects should have a concurrent structure.

This model presents a **different approach to concurrency** compared to traditional lock-based primitives. A core advantage is that it effectively **blurs the distinction between local and remote processing**.

### 1.1 Actor Properties and Actions

An actor is defined by several key properties:

| Property | Description |
| :--- | :--- |
| **Stateful** | Actors possess an internal state that they can reason upon. |
| **Asynchronous** | Actors run concurrently. |
| **Persistent** | The internal state can be saved to persistent storage, allowing the state to survive periods when the actor is not executing. |

In response to a message, an actor can perform the following actions:
1.  **Make Local Decisions:** Change their internal private state or change their behaviour.
2.  **Create More Actors:** An actor can spawn new actors.
3.  **Send More Messages:** An actor can send 0, 1, or more messages to the sender or other actors it knows of.

Crucially, actors may modify their own private state but **can only affect each other through messages, avoiding the need for any locks**. This adheres to the basic communication idea: "Do not communicate by sharing memory; instead, share memory by communicating".

### 1.2 Atomic Message Processing

Actors communicate through asynchronous message passing. When an actor receives a message, it is processed **atomically**. This means that the entire processing of a message cannot be interrupted by any other event, including the arrival of another message. Messages received while one is being processed are queued up in the actor's mailbox. This design **serializes all operations** manipulating the actor's state, preventing concurrent access issues.

## 2. The Akka Framework

**Akka** is an implementation of the Actor Model specifically designed for the Java Virtual Machine (JVM), offering APIs for both Java and Scala. Akka provides the basic actor model abstractions, often called the "classical actor API," along with higher-level abstractions like Akka clusters, Akka streams, and Akka HTTP. Akka is used in practice by major industry players such as BMW, VW Group, Bosch, and Siemens.

### 2.1 Core Actor Primitives

#### A. Creating Actors

To define a new actor type, you must inherit from the `AbstractActor` class. The minimum requirement is defining the `createReceive()` method, which dictates how incoming messages are handled.

To instantiate an actor, you first need an `ActorSystem`, which acts as the underlying runtime support:

```java
// Instantiating the ActorSystem
ActorSystem sys = ActorSystem.create(“sys name”);
```

Next, you define the actor's class using a `Props` object and use the `actorOf` method of the `ActorSystem` to create the actor. The `actorOf` method returns an `ActorRef`—a valid reference throughout the actor's lifecycle, even if the actor stops and restarts.

```java
// Defining the actor class property
Props p = Props.create(MyActor.class); 

// Creating the actor instance
ActorRef myActor = sys.actorOf(p, “actor name”); 
```

#### B. Sending Messages

Messages are sent to an actor using the `tell()` method on its `ActorRef`. The `tell` operation is asynchronous; the sender sends the message and does not wait for a reply.

Akka recommends that **messages must be immutable** to avoid unexpected behaviour, although this is not enforced by the framework.

```java
// Sending a SimpleMessage to myActor
myActor.tell(new SimpleMessage(), ActorRef.noSender()); 
```

#### C. Handling Messages and Mailbox Semantics

An actor's reception behavior is defined by `createReceive()`. When a message arrives, the actor checks all `match` clauses in the order they are defined and executes the method associated with the first match. If no clause matches, the message is discarded.

The default mailbox policy is **FIFO** (First In, First Out). Messages from a single sender are processed in FIFO order; however, the order of messages arriving from *different* senders is non-deterministic.

##### Example: Basic Message Handling

The following Java-like example illustrates a `CounterActor` that increments an internal counter upon receiving a `SimpleMessage`:

```java
public class CounterActor extends AbstractActor {
    private int counter = 0; // Internal state

    // Redefining the createReceive method to define behavior
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                // Match clause for SimpleMessage
                .match(SimpleMessage.class, this::onMessage)
                .build();
    }

    private void onMessage(SimpleMessage msg) {
        // Local decision: change internal state
        counter++;
        System.out.println("Counter increased to " + counter); 
    }

    // Static props method for instantiation (often used practice)
    static Props props() { 
        return Props.create(CounterActor.class);
    }
}
```

##### Dynamic Behavior Change

An actor can change its behaviour dynamically by changing the set of match clauses. This is achieved using the `getContext().become()` method, which takes a new `Receive` object as input.

##### Example: Multiple Match Clauses

An actor can handle different message types by concatenating multiple match clauses. Alternatively, an actor can use a single message type containing an attribute (e.g., a `code`) to decide which action to take:

```java
public Receive createReceive() {
    return receiveBuilder()
            // Clause 1: handles SimpleMessage
            .match(SimpleMessage.class, this::onSimpleMessage)
            // Clause 2: handles OtherMessage
            .match(OtherMessage.class, this::onOtherMessage)
            .build();
}
// ...
private void onOtherMessage(OtherMessage msg) {
    System.out.println("Received other type message"); 
}
```

#### D. Replying to Messages

When processing a message, an actor can obtain a reference to the sender using the `sender()` method and a reference to itself using `self()`. To reply, the actor invokes `tell()` on the sender reference:

```java
// Reply to the actor that sent the message
sender().tell(replyMsg, self());
```

### 2.2 Advanced Communication Patterns

#### A. The Ask Pattern

Since `tell` is asynchronous, waiting for a reply requires complex workarounds (like changing behavior and stashing messages). The **Ask Pattern** provides an alternative by sending a message and returning a **Future**, which will contain the reply when it becomes available.

The pattern is implemented via `Patterns.ask(receiver, msg, timeout)`. The sender can then block on the future using `Await.result(future, timeout)` to achieve a synchronous waiting behaviour.

#### B. Stashing Messages

It may be necessary to set aside messages that cannot be processed in the actor's current state (behaviour). This is achieved through **stashing**.

To use stashing, the actor must inherit from `AbstractActorWithStash`.
*   The `stash()` method saves the message for later processing.
*   The `unstashAll()` method extracts all stashed messages in the order they were added.

Stashing is useful, for example, if a message attempts to decrement a counter when its state is zero; the message can be stashed and processed later after an increment operation occurs.

### 2.3 Distribution

One of Akka's advantages is that actors can be **transparently distributed** across multiple machines. Communication remains the same as if the actors were local, relying only on the remote actor's address.

For a client/server setup, the actor system must be configured to listen on a TCP port and specify remote transport. Crucially, physical configuration details (hostname, port) are kept separate from the program logic, typically in a configuration file.

The client retrieves the remote server actor using its address string via `getContext().actorSelection()`:

```java
// Configuration (e.g., in a conf file)
// netty.tcp { hostname = "127.0.0.1" port = 6123 }

// Client side actor retrieves remote reference
String serverAddr = "akka.tcp://Server@127.0.0.1:6123/user/serverActor";
ActorSelection server = getContext().actorSelection(serverAddr);

// Communication continues as if local
server.tell(myMessage, self()); 
```

## 3. Fault Tolerance and Supervision

Faults and exceptional behaviours are managed through the concept of **supervision**.

### 3.1 Supervision Hierarchy

In Akka, supervision is hierarchical. Each actor has a supervisor (which is itself another actor) that monitors its execution state. Applications are typically organized into **supervision trees**. An actor automatically becomes the supervisor for all actors it creates using `getContext().actorOf()`.

If a supervisor cannot handle a problem locally, it propagates the fault to the upper layer.

### 3.2 Supervision Strategies and Directives

A supervisor can decide the appropriate action for a faulty supervised actor. This strategy is customized by overriding the `supervisorStrategy()` method.

Common strategies include:
*   **One-for-one:** Applies the chosen action only to the faulty supervised actor.
*   **One-for-all:** Applies the chosen action to *all* supervised actors under that supervisor, even if only one is faulty.

Common actions (directives) include:
*   `stop()`
*   `restart()`
*   `resume()`
*   `escalate()`

### 3.3 Restarting Actors

When an actor is restarted:
1.  Its **address does not change**.
2.  Its **message box is retrieved from persistent state**, meaning messages received during the restart process are preserved and not lost.

If the supervisor chooses a `restart()` strategy, the actor's internal state is typically reset, as a new actor effectively starts from scratch. If the supervisor chooses a `resume()` strategy, the operation attempts to continue, and the actor's state is preserved.

##### Example: Supervision Strategy (Restart)

This example shows a supervisor configured to restart the child actor upon receiving any generic `Exception`.

```java
// 1. Supervisor Actor Definition
public class CounterSupervisorActor extends AbstractActor {
    // Strategy: OneForOne, max 10 faults per minute, restart on Exception
    private static SupervisorStrategy strategy = new OneForOneStrategy(
        10, Duration.ofMinutes(1), 
        DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.restart()).build());

    @Override 
    public SupervisorStrategy supervisorStrategy() { 
        return strategy; 
    }
    // ... logic to create CounterActor using Ask Pattern (omitted here for brevity) ...
}

// 2. Supervised Actor Definition (CounterActor)
public class CounterActor extends AbstractActor {
    // ... state and props definition ...
    
    void onMessage(DataMessage msg) throws Exception {
        if (msg.getCode() == Counter.FAULT_OP) {
            System.out.println("I am emulating a FAULT!");
            throw new Exception("Actor fault!"); // Triggers supervision
        }
        // ... normal operations ...
    }

    @Override public void preRestart(Throwable reason, Optional<Object> message) { 
        System.out.print("Preparing to restart..."); // Overridden lifecycle method 
    }
    
    @Override public void postRestart(Throwable reason) { 
        System.out.println("...now restarted!"); // Overridden lifecycle method
    }
}
```
***

## 4. Akka Clustering

Akka clustering is a powerful feature built on top of the basic actor model, providing a decentralized membership service without a single point of failure.

### 4.1 Key Concepts

*   **Node:** A logical member of a cluster, often corresponding to a process or actor system, identified by `hostname:port:uid`.
*   **Cluster:** A set of nodes joined together via a membership service.
*   **Leader:** A single node managing cluster convergence, handling nodes joining and leaving.
*   **Seed Nodes:** Configured contact points for new nodes wishing to join the cluster.

### 4.2 Clustering Protocol

Nodes organize themselves into an **overlay network** superimposed on the physical network. They distribute information about cluster members using a **gossip protocol**, allowing nodes to propagate their current view of the membership and update their own view based on received messages. This mechanism ensures that the state of nodes eventually converges to a consistent view across the cluster.

### 4.3 Higher-Level Tools

Akka clustering supports higher-level tools for sophisticated distributed applications:
*   **Cluster Singleton:** Ensures that only a single actor of a certain type exists in the cluster.
*   **Cluster Sharding:** Distributes actors across nodes in the cluster, ensuring they can communicate without needing to know their physical location.
*   **Distributed Data:** Creates a distributed key-value store across the cluster nodes.

***
To solidify the understanding of Akka and the Actor Model, consider it like managing a massive, hyper-efficient post office: each **Actor** is a specialized clerk with their own private desk and filing cabinet (state). They only communicate by sending sealed **Messages** (immutable data) via the internal mail system (mailbox). The **Actor System** is the building itself, and the **Supervisor** is the department manager who, if a clerk falls ill (fault), can hire a new clerk immediately (restart) or simply tell them to take a break and resume work later (resume), ensuring the mail flow (communication) is never interrupted or lost, regardless of whether the clerk is local or across town (distribution).