# MPI Tutorial - Message Passing Interface

**Course**: NSDS (Networked Software for Distributed Systems)  
**Institution**: Politecnico di Milano  
**Professor**: Prof. Margara

---

## Table of Contents

1. [Introduction to MPI](#introduction-to-mpi)
2. [History and Motivation](#history-and-motivation)
3. [MPI vs Big Data Platforms](#mpi-vs-big-data-platforms)
4. [Programming Model](#programming-model)
5. [Point-to-Point Communication](#point-to-point-communication)
6. [Collective Communication](#collective-communication)
7. [Available Examples](#available-examples)
8. [Getting Started](#getting-started)
9. [Exercises](#exercises)

---

## Introduction to MPI

**MPI (Message Passing Interface)** is a standardized specification for high-performance computing (HPC) programs. It is designed for compute-intensive tasks where computational power is the main bottleneck, such as:

- **Molecular modeling** for drug discovery
- **Computational fluid dynamics** for climate modeling
- **Agent-based simulations** for population dynamics studies
- **Computer simulations** solving complex equations describing system evolution

### Key Characteristics

- **Standard API**: MPI is a specification, not a concrete implementation
- **Multiple Implementations**: OpenMPI, MPICH, Intel MPI, and others
- **Target Languages**: C, C++, Fortran
- **Design Goals**:
  - General and adaptable to various problems
  - Easy to use
  - Portable across platforms
  - Efficient performance

---

## History and Motivation

### Timeline

- **1980s**: Parallel and distributed computing emerged with independent solutions but no standard
- **1992**: Requirements for a message passing standard defined at the "Workshop on Standards for Message Passing in Distributed Memory Environment"
- **1994**: First version of MPI (MPI-1.0) released
- **June 2021**: Current version MPI-4.0 approved

### The Problem MPI Solves

Before MPI, several independent solutions existed with different trade-offs between:
- Portability
- Performance
- Ease of use

MPI provides a unified standard that balances all these concerns.

---

## MPI vs Big Data Platforms

MPI differs significantly from big data platforms like Apache Spark and Kafka:

| Aspect | MPI | Big Data Platforms |
|--------|-----|-------------------|
| **API Focus** | Performance | Ease of use |
| **Abstraction Level** | Low-level, general primitives (send/receive) | High-level, specialized (relational, graph data) |
| **Parallelism** | Explicit communication and synchronization | Implicit parallelism |
| **Development** | Highly optimized code, used for years/decades | Fast prototyping, frequently changing tasks |
| **Resource Usage** | Uses all available resources exclusively | Dynamic resource sharing, overcommitment |
| **Fault Tolerance** | Assumes reliable hardware (supercomputers) | Built-in fault tolerance mechanisms |
| **Hardware** | Homogeneous, highly reliable | Heterogeneous, handles stragglers |
| **Computation Model** | Bulk synchronous computations | Overlapping computation and communication |
| **Data Characteristics** | Compute-intensive (small initial data) | Data-intensive (huge volumes, streams) |

---

## Programming Model

### SPMD - Single Program Multiple Data

MPI provides a **Single Program Multiple Data (SPMD)** abstraction:
- Developer writes **one program**
- Program executes in parallel by **multiple processes**
- Each process has a unique **rank** (identifier)
- Developer uses rank to differentiate process behavior

### Basic Structure

```c
#include <mpi.h>

int main(int argc, char** argv) {
    // Include declarations, prototypes, functions
    
    MPI_Init(&argc, &argv);        // Initialize MPI environment
    
    // Code executed in parallel by all processes
    // Can include computations and MPI communication primitives
    
    MPI_Finalize();                // Terminate MPI environment
    
    return 0;
}
```

### MPI Function Format

All MPI functions:
- Have names starting with `MPI_`
- Return an integer error code:
  - `MPI_SUCCESS` if no error occurred
  - Specific error code otherwise

### Environment Management

```c
// Get total number of processes in communicator
MPI_Comm_size(MPI_COMM_WORLD, &size);

// Get rank of calling process
MPI_Comm_rank(MPI_COMM_WORLD, &rank);

// Get processor name
MPI_Get_processor_name(name, &len);
```

### Communicators

**Communicators** define the scope of communication:
- **MPI_COMM_WORLD**: Predefined communicator including all processes
- Processes organized into groups that exchange messages over a communicator
- Most MPI routines require a communicator as an argument (typically the last parameter)
- Ranks are continuous integers starting from 0 within a communicator

---

## Point-to-Point Communication

Point-to-point primitives send messages from **one sender** to **one receiver**.

### Data Types

MPI defines portable data types for cross-platform compatibility:
- `MPI_SHORT`, `MPI_INT`, `MPI_LONG`
- `MPI_FLOAT`, `MPI_DOUBLE`
- `MPI_CHAR`, `MPI_BYTE`
- Custom structured types can be created

### Basic Send and Receive

```c
// Blocking asynchronous send
MPI_Send(data, count, datatype, destination, tag, communicator);

// Blocking receive
MPI_Recv(data, count, datatype, source, tag, communicator, &status);
```

**Parameters**:
- `data`: Pointer to send/receive buffer
- `count`: Number of elements
- `datatype`: MPI data type (e.g., `MPI_INT`)
- `destination/source`: Rank of receiver/sender (or `MPI_ANY_SOURCE`)
- `tag`: Message tag for classification (or `MPI_ANY_TAG`)
- `communicator`: Communication scope
- `status`: Information about received message

### Send and Receive Operations

MPI provides various send and receive operations with different semantics:

#### Send Operations Comparison

| Operation | Function | Blocking | Completion Condition | Use Case |
|-----------|----------|----------|---------------------|----------|
| **Standard Send** | `MPI_Send` | Yes | Safe to reuse buffer (may buffer or wait for receiver) | General purpose, most common |
| **Synchronous Send** | `MPI_Ssend` | Yes | Receiver has started receiving (acknowledgment) | When you need confirmation of delivery |
| **Non-blocking Send** | `MPI_Isend` | No | Returns immediately | Overlap communication with computation |
| **Send-Receive** | `MPI_Sendrecv` | Yes | Both send and receive complete | Avoid deadlocks in exchanges |

#### Receive Operations Comparison

| Operation | Function | Blocking | Completion Condition | Use Case |
|-----------|----------|----------|---------------------|----------|
| **Standard Receive** | `MPI_Recv` | Yes | Message fully received and ready | General purpose, most common |
| **Non-blocking Receive** | `MPI_Irecv` | No | Returns immediately | Overlap communication with computation |
| **Probe** | `MPI_Probe` | Yes | Message available (doesn't receive) | Query message info before receiving |
| **Non-blocking Probe** | `MPI_Iprobe` | No | Returns immediately | Check for messages without blocking |

#### Key Differences Summary

**Blocking vs Non-Blocking**:
- **Blocking**: Function doesn't return until operation can complete safely
- **Non-blocking**: Returns immediately, must use `MPI_Wait`/`MPI_Test` to check completion

**Memory Tricks**:
- **"I" = Immediate**: Functions with **I** in the name (`MPI_Isend`, `MPI_Irecv`) return **I**mmediately = **Non-blocking**
- **No "I" = Must wait**: Functions without "I" (`MPI_Send`, `MPI_Recv`, `MPI_Ssend`) = **Blocking**
- **"S" = Synchronize**: `MPI_Ssend` has **S** for **S**ynchronous = waits for receiver handshake

**Synchronous vs Asynchronous**:
- **Synchronous** (`MPI_Ssend`): Waits for receiver acknowledgment (handshake)
- **Asynchronous** (`MPI_Send`): May return before receiver processes (can use buffering)

**Buffer Safety**:
- After **blocking send** returns: Safe to reuse/modify send buffer
- After **non-blocking send** returns: NOT safe until `MPI_Wait` completes
- After **blocking receive** returns: Data ready to use in receive buffer
- After **non-blocking receive** returns: NOT safe until `MPI_Wait` completes

### Message Ordering

MPI guarantees **FIFO ordering** between one sender and one receiver:
- Messages from sender S to receiver R arrive in the order sent
- Multiple receives from same sender/tag receive messages in order
- Applies only to operations submitted by the same thread

### Avoiding Deadlocks

**Common deadlock scenarios**:

1. **Circular dependencies**:
```c
// DEADLOCK: Each process waits for the other
MPI_Ssend(..., other_rank, ...);  // Both block here
MPI_Recv(..., other_rank, ...);
```

2. **Tag mismatch ordering**:
```c
// Process 0
MPI_Send(..., tag=1, ...);
MPI_Send(..., tag=2, ...);

// Process 1 - DEADLOCK
MPI_Recv(..., tag=2, ...);  // Waits for tag 2
MPI_Recv(..., tag=1, ...);  // But tag 1 arrives first
```

**Solutions**:
- Use **non-blocking operations** (`MPI_Isend`, `MPI_Irecv`)
- Use **MPI_Sendrecv** (posts receive before blocking on send)
- Carefully order send/receive to avoid circular dependencies

### Dynamic Message Sizes

When message size is unknown:

1. **Two-phase protocol**: Sender first sends message length
2. **Probing**:
```c
MPI_Status status;
int count;

// Check for message availability
MPI_Probe(source, tag, communicator, &status);

// Get message size
MPI_Get_count(&status, MPI_INT, &count);

// Allocate buffer and receive
buffer = malloc(count * sizeof(int));
MPI_Recv(buffer, count, MPI_INT, 
         status.MPI_SOURCE, status.MPI_TAG, communicator, &status);
```

---

## Collective Communication

Collective operations involve **all processes** in a communicator collaborating toward a common goal.

### Barrier Synchronization

```c
MPI_Barrier(MPI_COMM_WORLD);
```
- All processes must reach the barrier before any can proceed
- Ensures all processes complete a computation phase before continuing

### Broadcast

```c
MPI_Bcast(data, count, datatype, root, communicator);
```
- **Root process** distributes same data to all processes
- All processes invoke this function
- For root: `data` points to data to send
- For receivers: `data` points to receive buffer

### Reduce

```c
MPI_Reduce(send_data, recv_data, count, datatype, operation, root, communicator);
```
- Performs reduction operation on data from all processes
- Result stored in root process
- **Operations**: `MPI_SUM`, `MPI_MAX`, `MPI_MIN`, `MPI_PROD`, `MPI_LAND`, `MPI_LOR`, etc.
- Example: Sum element-wise across arrays from all processes

**All-Reduce** (`MPI_Allreduce`): Result stored in all processes

### Scatter

```c
MPI_Scatter(send_data, send_count, send_type,
            recv_data, recv_count, recv_type,
            root, communicator);
```
- Root process distributes different data portions to each process
- `send_count`: Number of elements sent to **each** process

### Gather

```c
MPI_Gather(send_data, send_count, send_type,
           recv_data, recv_count, recv_type,
           root, communicator);
```
- Root process collects data from all processes into single array
- Opposite of scatter

---

## Available Examples

The repository includes the following example programs:

### Basic Examples
- **hello_world.c**: Basic MPI program showing initialization, rank queries, and finalization
- **recv.c**: Demonstrates blocking receive and resource usage (busy waiting)

### Point-to-Point Communication
- **deadlock.c**: Exemplifies deadlock scenarios
  - Solutions branch shows strategies to avoid deadlocks (non-blocking, `MPI_Sendrecv`)
- **ping_pong.c**: Two processes exchange messages incrementing a counter
- **ring.c**: Multiple processes exchange tokens in a ring topology
- **probe.c**: Dynamic message receiving using `MPI_Probe`

### Collective Communication
- **average.c**: Distributed average computation using scatter and gather
- **filter.c**: Array filtering using broadcast and dynamic receives
- **char_count.c**: Complex example demonstrating data shuffle complexity

---

## Getting Started

### Installation

#### Linux
```bash
sudo apt-get install openmpi-bin openmpi-common libopenmpi-dev
```

#### macOS
```bash
brew install open-mpi
```

#### Windows
Use Windows Subsystem for Linux (WSL) and follow Linux instructions.

### Compiling and Running

#### Compile
```bash
mpicc program.c -o program
```

#### Run (Single Machine)
```bash
mpirun -np 4 ./program  # Run with 4 processes
```

#### Run (Multiple Machines)
Requirements:
1. Executable must be at the **same path** on all machines
2. Same MPI version and implementation on all machines
3. SSH access with **public key authentication** from master to all nodes

```bash
mpirun --host node1:12,node2:8 ./program
```
- Runs 12 processes on node1, 8 processes on node2

### Clone This Repository

```bash
git clone https://github.com/margara/NSDS_MPI_tutorial.git
cd NSDS_MPI_tutorial
```

---

## Exercises

### Exercise 1: Deadlock Avoidance
**File**: `deadlock.c`

Starting from the deadlock example:
- Modify to avoid deadlock using non-blocking calls (`MPI_Isend`, `MPI_Irecv`)
- Try alternative solution with `MPI_Sendrecv`
- Compare different approaches

**Solution**: Check the `solutions` branch

### Exercise 2: Ping Pong
**File**: `ping_pong.c`

Write a program where:
- Process P0 sends a message with a number to P1
- Process P1 replies with incremented number
- Continue until number exceeds a threshold (e.g., 100)

### Exercise 3: Ring Communication
**File**: `ring.c`

Implement ring token passing:
- N processes organized in a ring
- Each process receives token, processes it, sends to next
- Last process sends back to first
- Run for a specified number of iterations

### Exercise 4: Dynamic Receiving with Probe
**File**: `probe.c`

Complete the receiver code:
- Use `MPI_Probe` to query message availability
- Extract sender, tag, and message size
- Dynamically allocate buffer and receive message
- Print results

### Exercise 5: Distributed Average
**File**: `average.c`

Compute average of large array:
- Process P0 creates array and scatters portions to all processes
- Each process computes local average
- Process P0 gathers partial averages and computes final result

### Exercise 6: Array Filter
**File**: `filter.c`

Filter arrays of numbers:
- Process P0 broadcasts number N
- Each process creates random array
- Filter arrays retaining only multiples of N
- Process P0 receives filtered results, combines, and prints

---

## Compilation and Execution Tips

### Compile All Examples
```bash
cd src
mpicc hello_world.c -o hello_world
mpicc ping_pong.c -o ping_pong
mpicc average.c -o average
# ... etc
```

### Run Examples
```bash
# Hello World (default: one process per core)
mpirun ./hello_world

# Specify number of processes
mpirun -np 4 ./ping_pong

# Run on multiple hosts
mpirun --host solar1:12,solar3:8 ./average
```

### Debug Tips
- Use `printf` with rank to identify which process is executing
- Check return values of MPI functions
- Use MPI error handlers for detailed error information
- Monitor resource usage (MPI uses busy waiting by default)

---

## Additional Resources

- **Official MPI Documentation**: [https://www.mpi-forum.org/](https://www.mpi-forum.org/)
- **OpenMPI Documentation**: [https://www.open-mpi.org/doc/](https://www.open-mpi.org/doc/)
- **MPI Tutorial**: [https://mpitutorial.com/](https://mpitutorial.com/)
- **NSDS Course Materials**: Check course website for lecture slides and videos

---

## Notes

- **Busy Waiting**: MPI blocking receives use busy waiting (100% CPU) to minimize latency
- **Thread Safety**: MPI + multi-threading is complex; most programs use pure MPI
- **Custom Communicators**: MPI supports creating custom communicators via split/merge operations
- **Custom Operators**: You can register user-defined reduction operators with `MPI_Op_create`

---

## License

This tutorial is provided for educational purposes as part of the NSDS course at Politecnico di Milano.

---

## Acknowledgments

Course material developed by **Prof. Margara** at Politecnico di Milano for the Networked Software for Distributed Systems (NSDS) course.
