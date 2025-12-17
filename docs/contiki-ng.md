# Contiki-NG: Comprehensive Guide to IoT Operating System

## Table of Contents
1. [Introduction to IoT and Contiki-NG](#introduction)
2. [What Makes IoT Special](#iot-characteristics)
3. [Contiki-NG Overview](#contiki-overview)
4. [Concurrency Models](#concurrency-models)
5. [Protothread Programming](#protothread-programming)
6. [Timer Libraries](#timer-libraries)
7. [Networking in IoT](#networking)
8. [RPL (Routing Protocol for Low-Power and Lossy Networks)](#rpl)
9. [MQTT Protocol Integration](#mqtt)
10. [Cooja Simulator](#cooja-simulator)
11. [Practical Examples](#practical-examples)
12. [Example Programs by Concept](#examples-by-concept)
13. [Development Workflow](#development-workflow)

---

## Introduction to IoT and Contiki-NG <a name="introduction"></a>

### What is the Internet of Things?

The Internet of Things (IoT) represents a paradigm shift from traditional internet usage. While the conventional internet is about **data created by people** (web pages, documents, social media), the IoT is about **data created by objects** - machines, sensors, and devices that interact with the physical environment without human intervention.

**Key Definitions:**
- **Gartner**: "A scenario where objects, animals, or people are provided with unique identifiers and have the ability to transfer data over a network without requiring human intervention."
- **Cisco**: "A network of physical objects accessed through the internet with embedded technology that allows them to interact with internal states or the external environment."
- **Khaled Sami Hall**: "The network of anything - to sense, collect, transmit, analyze, and distribute data for people to process information and acquire higher-level knowledge."

### Computing Power in IoT Devices

Modern IoT devices (approximately the size of AA batteries) have computing and communication capabilities equivalent to desktop computers from the mid-1990s:
- **RAM**: Typically 10-100 KB (compared to gigabytes in modern computers)
- **Processing Power**: 8-32 MHz (compared to multi-GHz processors)
- **Flash Storage**: 48-512 KB (compared to terabytes in modern storage)
- **Radio Communication**: Low-power wireless (802.15.4, BLE)

---

## What Makes IoT Special <a name="iot-characteristics"></a>

### Resource Constraints

IoT devices operate under severe constraints:

1. **Energy Limitations**
   - Battery-powered devices must last months or years
   - Energy consumption must be minimized in all operations
   - Radio communication is the most energy-intensive operation

2. **Memory Constraints**
   - Limited RAM for program execution
   - Small flash memory for code storage
   - Cannot afford memory-hungry abstractions like multi-threading

3. **Processing Power**
   - Simple microcontrollers with limited CPU cycles
   - Must balance computation efficiency with functionality

### Multi-hop Networking

IoT networks typically use **multi-hop communication** rather than single long-distance transmissions due to energy efficiency:

**Physics of Wireless Transmission:**
The power relationship in wireless communication follows:
```
PR = PT × (GT × GR × λ²) / ((4π)² × R²)
```
Where:
- PR = Received power
- PT = Transmitted power  
- GT, GR = Antenna gains
- λ = Wavelength
- R = Distance

**Key Insight**: Energy cost grows **quadratically** with distance! 
- If distance doubles from 2m to 4m, energy cost increases **4x** (not 2x)
- Multi-hop routing with shorter hops is more energy-efficient than single long-distance transmission

---

## Contiki-NG Overview <a name="contiki-overview"></a>

### History and Design Philosophy

**Contiki-NG** (Next Generation) is a fork of the original Contiki OS, focused on:
- **IPv6 networking support** (6LoWPAN, RPL)
- **32-bit platforms** 
- **Higher code quality** through focused scope
- **~15 years of development** and maturity

### Key Features

1. **Small Memory Footprint**
   - Minimal RAM and flash usage
   - Optimized for resource-constrained devices

2. **Easy Portability**
   - Straightforward to port to new platforms
   - Well-defined hardware abstraction layer

3. **Industry and Academic Use**
   - Production-ready for commercial IoT systems
   - Widely used in research and education

4. **Programming Model**
   - Programs written in **C with macros** (dialect of C)
   - Event-driven with protothread-based concurrency
   - Runs on real hardware, native platform, or simulator (Cooja)

### Supported Platforms

- **Native**: Run on host machine (Linux, macOS)
- **Real Hardware**: Various MCU platforms (CC2538, CC26xx, nRF52, etc.)
- **Simulator**: Cooja with MSPSim emulator

---

## Concurrency Models <a name="concurrency-models"></a>

### Why Not Multi-threading?

Traditional multi-threading is **rare in IoT** due to memory constraints:

**Problems with Multi-threading:**
- Each thread requires its own stack
- Stack size must be over-provisioned (unknown maximum)
- Memory inefficiency in constrained devices
- Difficult to tune and manage

**Advantages (outweighed by cons):**
- Familiar to programmers
- Easy to port non-embedded code
- Natural concurrency abstraction

### Event-Driven Programming

Most IoT systems use **event-driven architectures**:
- Single execution thread
- Event loop dispatches handlers
- Memory efficient (one shared stack)

**Drawbacks:**
- Manual state management
- Complex control flow
- Difficult to maintain blocking operations

### Protothreads: The Contiki Solution

**Protothreads** provide thread-like programming model without multi-threading overhead:
- **Stackless threads** - no separate stack per thread
- **Non-preemptive** - cooperative multitasking
- **Memory efficient** - all protothreads share single stack
- **Event-driven underneath** - syntactic sugar over events

---

## Protothread Programming <a name="protothread-programming"></a>

### Basic Structure

Every Contiki application consists of one or more **processes** (protothreads):

```c
#include "contiki.h"
#include <stdio.h>

// Declare the process
PROCESS(my_process, "My Process Description");

// Set process to auto-start
AUTOSTART_PROCESSES(&my_process);

// Define the process thread
PROCESS_THREAD(my_process, ev, data)
{
  PROCESS_BEGIN();
  
  // Process code here
  printf("Hello from Contiki-NG!\n");
  
  PROCESS_END();
}
```

### Key Macros

1. **PROCESS(name, description)** - Declares a process
2. **AUTOSTART_PROCESSES(&process)** - Auto-start on boot
3. **PROCESS_THREAD(name, ev, data)** - Defines process implementation
4. **PROCESS_BEGIN()** - Marks beginning of process code
5. **PROCESS_END()** - Marks end of process code

### State Persistence: The Static Keyword

**Critical Rule**: All local variables that need to persist across blocking operations **must be declared static**!

```c
PROCESS_THREAD(counter_process, ev, data)
{
  static int count = 0;  // MUST be static!
  static struct etimer timer;
  
  PROCESS_BEGIN();
  
  etimer_set(&timer, CLOCK_SECOND * 2);
  
  while(1) {
    printf("Count: %d\n", count);
    count++;  // Value persists across iterations
    
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer));
    etimer_reset(&timer);
  }
  
  PROCESS_END();
}
```

**Why?** Protothreads are stackless - when a process yields, local variables on the stack are lost. Static variables persist in data segment.

### Waiting and Blocking

**PROCESS_WAIT_EVENT()** - Wait for any event:
```c
PROCESS_WAIT_EVENT();
```

**PROCESS_WAIT_EVENT_UNTIL(condition)** - Wait for specific condition:
```c
PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer));
```

**PROCESS_YIELD()** - Voluntarily yield CPU:
```c
PROCESS_YIELD();
```

### Example: Hello World with Counter

```c
#include "contiki.h"
#include <stdio.h>

PROCESS(hello_world_process, "Hello world process");
AUTOSTART_PROCESSES(&hello_world_process);

PROCESS_THREAD(hello_world_process, ev, data)
{
  static struct etimer timer;
  static int counter = 0;

  PROCESS_BEGIN();

  etimer_set(&timer, CLOCK_SECOND * 2);

  while(1) {
    printf("Hello, world #%d\n", counter);
    counter++;

    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer));
    etimer_reset(&timer);
  }

  PROCESS_END();
}
```

---

## Timer Libraries <a name="timer-libraries"></a>

Contiki-NG provides multiple timer abstractions for different use cases:

### Timer Hierarchy

```
timer (base) 
    ├── etimer (event timer - generates events)
    ├── stimer (seconds timer - long periods)
    ├── ctimer (callback timer - executes callback)
    └── rtimer (real-time timer - preemptive)
```

### Quick Reference Table

| Timer | Preemptive | Time Unit | Typical Use | Declaration | Set Timer | Wait/Check |
|-------|-----------|-----------|-------------|-------------|-----------|------------|
| **etimer** | No | Ticks | Periodic tasks, timeouts | `static struct etimer t;` | `etimer_set(&t, CLOCK_SECOND*5);` | `PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&t));` |
| **stimer** | No | Seconds | Long timeouts (minutes) | `static struct stimer t;` | `stimer_set(&t, 60);` | `if(stimer_expired(&t)) {...}` |
| **ctimer** | No | Ticks | Callbacks, decoupled logic | `static struct ctimer t;` | `ctimer_set(&t, CLOCK_SECOND, callback, ptr);` | Callback fires automatically |
| **rtimer** | **YES** | Absolute | Precise timing, TDMA | `static struct rtimer t;` | `rtimer_set(&t, RTIMER_NOW()+delay, 0, cb, ptr);` | Callback fires at interrupt level |

### Essential API Summary

```c
// ETIMER - Event Timer (most common)
static struct etimer timer;
etimer_set(&timer, CLOCK_SECOND * 5);              // Set for 5 seconds
PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer));  // Wait in protothread
etimer_reset(&timer);                               // Restart with same period
etimer_stop(&timer);                                // Cancel timer

// STIMER - Seconds Timer (long periods)
static struct stimer timer;
stimer_set(&timer, 60);                             // Set for 60 seconds
if(stimer_expired(&timer)) { /* ... */ }            // Poll to check
unsigned long remaining = stimer_remaining(&timer); // Get remaining time

// CTIMER - Callback Timer
static struct ctimer timer;
void callback(void *ptr) { /* ... */ }
ctimer_set(&timer, CLOCK_SECOND, callback, NULL);   // Set with callback
ctimer_reset(&timer);                                // Restart with same period
ctimer_stop(&timer);                                 // Cancel timer

// RTIMER - Real-Time Timer (preemptive!)
static struct rtimer timer;
void callback(struct rtimer *t, void *ptr) { /* keep SHORT! */ }
rtimer_set(&timer, RTIMER_NOW() + delay, 0, callback, NULL);
rtimer_clock_t now = RTIMER_NOW();                   // Get current time
```

### 1. Event Timers (etimer)

**Most commonly used** - generates events for protothreads:

```c
static struct etimer my_timer;

// Set timer for 5 seconds
etimer_set(&my_timer, CLOCK_SECOND * 5);

// Wait for timer to expire
PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&my_timer));

// Reset timer for another period
etimer_reset(&my_timer);

// Stop timer
etimer_stop(&my_timer);
```

**Characteristics:**
- Non-preemptive
- Integrates with protothread event system
- Suitable for periodic tasks and timeouts

**Example 1: Periodic Sensor Reading**
```c
PROCESS_THREAD(sensor_process, ev, data)
{
  static struct etimer periodic_timer;
  
  PROCESS_BEGIN();
  
  // Set timer for 30 seconds
  etimer_set(&periodic_timer, CLOCK_SECOND * 30);
  
  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));
    
    // Read sensor and send data
    int temperature = read_temperature_sensor();
    printf("Temperature: %d C\n", temperature);
    send_sensor_data(temperature);
    
    // Reset for next reading
    etimer_reset(&periodic_timer);
  }
  
  PROCESS_END();
}
```

**Example 2: One-shot Timeout for Network Operations**
```c
PROCESS_THREAD(udp_client_process, ev, data)
{
  static struct etimer send_timer;
  static struct etimer response_timer;
  
  PROCESS_BEGIN();
  
  // Wait 10 seconds before first transmission
  etimer_set(&send_timer, CLOCK_SECOND * 10);
  PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&send_timer));
  
  while(1) {
    // Send UDP packet
    printf("Sending request...\n");
    send_udp_packet();
    
    // Set 5-second timeout for response
    etimer_set(&response_timer, CLOCK_SECOND * 5);
    
    PROCESS_WAIT_EVENT_UNTIL(ev == udp_response_event || 
                             etimer_expired(&response_timer));
    
    if(ev == udp_response_event) {
      printf("Response received!\n");
      etimer_stop(&response_timer); // Cancel timeout
    } else {
      printf("Request timeout - no response\n");
    }
    
    // Wait before next transmission
    etimer_set(&send_timer, CLOCK_SECOND * 60);
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&send_timer));
  }
  
  PROCESS_END();
}
```

**Example 3: Adding Random Jitter to Avoid Collisions**
```c
PROCESS_THREAD(broadcast_process, ev, data)
{
  static struct etimer periodic_timer;
  
  PROCESS_BEGIN();
  
  while(1) {
    // Base interval: 60 seconds + random jitter (0-10 seconds)
    clock_time_t interval = CLOCK_SECOND * 60 + 
                            (random_rand() % (CLOCK_SECOND * 10));
    
    etimer_set(&periodic_timer, interval);
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));
    
    // Broadcast beacon with random delay to avoid collisions
    printf("Broadcasting beacon\n");
    broadcast_beacon();
  }
  
  PROCESS_END();
}
```

### 2. Seconds Timers (stimer)

For **longer time periods** (seconds to minutes):

```c
static struct stimer long_timer;

// Set timer for 60 seconds
stimer_set(&long_timer, 60);

// Check if expired
if(stimer_expired(&long_timer)) {
  // Timer has expired
}
```

**Characteristics:**
- Uses seconds instead of clock ticks
- Lower memory overhead for long durations
- Non-blocking - must poll to check expiration
- Good for timeouts measured in minutes/hours

**Example 1: Node Aging/Expiration Tracking**
```c
#define NODE_TIMEOUT_SEC (3 * 60)  // 3 minutes

typedef struct {
  uip_ipaddr_t addr;
  struct stimer last_seen;
  int is_active;
} node_entry_t;

static node_entry_t node_table[20];

void update_node_entry(uip_ipaddr_t *addr) {
  node_entry_t *entry = find_or_create_node(addr);
  
  // Update last-seen timestamp
  stimer_set(&entry->last_seen, NODE_TIMEOUT_SEC);
  entry->is_active = 1;
}

void cleanup_stale_nodes(void) {
  for(int i = 0; i < 20; i++) {
    if(node_table[i].is_active && stimer_expired(&node_table[i].last_seen)) {
      printf("Node expired: ");
      print_ipaddr(&node_table[i].addr);
      node_table[i].is_active = 0;
    }
  }
}
```

**Example 2: Connection Keep-Alive Management**
```c
static struct stimer connection_timer;

PROCESS_THREAD(connection_manager, ev, data)
{
  static struct etimer check_timer;
  
  PROCESS_BEGIN();
  
  // Initialize connection with 5-minute timeout
  establish_connection();
  stimer_set(&connection_timer, 5 * 60);
  
  // Check every 30 seconds
  etimer_set(&check_timer, CLOCK_SECOND * 30);
  
  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&check_timer));
    
    if(stimer_expired(&connection_timer)) {
      printf("Connection timeout - reconnecting...\n");
      establish_connection();
      stimer_set(&connection_timer, 5 * 60);
    } else {
      printf("Connection still alive (%lu sec remaining)\n", 
             stimer_remaining(&connection_timer));
    }
    
    etimer_reset(&check_timer);
  }
  
  PROCESS_END();
}
```

**Example 3: Rate Limiting / Throttling**
```c
static struct stimer rate_limit_timer;
static int messages_sent = 0;

#define MAX_MESSAGES_PER_HOUR 100
#define RATE_WINDOW_SEC (60 * 60)  // 1 hour

int can_send_message(void) {
  // Reset counter if time window expired
  if(stimer_expired(&rate_limit_timer)) {
    messages_sent = 0;
    stimer_set(&rate_limit_timer, RATE_WINDOW_SEC);
  }
  
  // Check if under rate limit
  if(messages_sent < MAX_MESSAGES_PER_HOUR) {
    messages_sent++;
    return 1;
  }
  
  printf("Rate limit exceeded - wait %lu seconds\n",
         stimer_remaining(&rate_limit_timer));
  return 0;
}
```

### 3. Callback Timers (ctimer)

Execute **callback function** when timer expires:

```c
static struct ctimer callback_timer;

void timer_callback(void *ptr)
{
  printf("Timer fired!\n");
  // Do work here
}

// Set callback timer
ctimer_set(&callback_timer, CLOCK_SECOND, timer_callback, NULL);
```

**Important**: Provides **lower bound** on execution time - callback runs **at least** this long in the future, but may be delayed if another protothread is running.

**Characteristics:**
- Non-preemptive (callback runs in protothread context)
- No need to PROCESS_WAIT - callback executes automatically
- Useful for decoupling timer logic from main process flow
- Can pass pointer to callback for context

**Example 1: Delayed LED Toggle**
```c
static struct ctimer led_timer;

void toggle_led_callback(void *ptr)
{
  static int led_state = 0;
  
  led_state = !led_state;
  if(led_state) {
    leds_on(LEDS_GREEN);
  } else {
    leds_off(LEDS_GREEN);
  }
  
  printf("LED toggled: %s\n", led_state ? "ON" : "OFF");
  
  // Restart timer for next toggle (1 second)
  ctimer_reset(&led_timer);
}

PROCESS_THREAD(blink_process, ev, data)
{
  PROCESS_BEGIN();
  
  // Start periodic LED blinking
  ctimer_set(&led_timer, CLOCK_SECOND, toggle_led_callback, NULL);
  
  // Process continues - doesn't need to wait
  PROCESS_WAIT_EVENT();
  
  PROCESS_END();
}
```

**Example 2: Button Debouncing**
```c
static struct ctimer debounce_timer;
static int button_stable = 1;

void debounce_callback(void *ptr)
{
  // Re-enable button after debounce period
  button_stable = 1;
  printf("Button ready\n");
}

void button_press_handler(void)
{
  if(!button_stable) {
    printf("Button press ignored (debouncing)\n");
    return;
  }
  
  // Handle button press
  printf("Button pressed!\n");
  handle_button_action();
  
  // Disable button and start debounce timer (200ms)
  button_stable = 0;
  ctimer_set(&debounce_timer, CLOCK_SECOND / 5, debounce_callback, NULL);
}
```

**Example 3: Multi-step Operation with Callbacks**
```c
typedef struct {
  int step;
  int sensor_value;
} operation_context_t;

static struct ctimer operation_timer;
static operation_context_t ctx;

void step_callback(void *ptr)
{
  operation_context_t *context = (operation_context_t *)ptr;
  
  switch(context->step) {
    case 0:
      printf("Step 1: Warming up sensor...\n");
      sensor_warmup();
      context->step = 1;
      ctimer_set(&operation_timer, CLOCK_SECOND * 2, step_callback, ptr);
      break;
      
    case 1:
      printf("Step 2: Reading sensor...\n");
      context->sensor_value = read_sensor();
      context->step = 2;
      ctimer_set(&operation_timer, CLOCK_SECOND, step_callback, ptr);
      break;
      
    case 2:
      printf("Step 3: Sending value: %d\n", context->sensor_value);
      send_data(context->sensor_value);
      sensor_powerdown();
      printf("Operation complete!\n");
      break;
  }
}

void start_sensor_operation(void)
{
  ctx.step = 0;
  ctx.sensor_value = 0;
  ctimer_set(&operation_timer, CLOCK_SECOND, step_callback, &ctx);
}
```

**Example 4: Retry Mechanism with Exponential Backoff**
```c
static struct ctimer retry_timer;
static int retry_count = 0;
static int max_retries = 5;

void retry_callback(void *ptr)
{
  if(send_network_packet() == SUCCESS) {
    printf("Transmission successful!\n");
    retry_count = 0;
  } else {
    retry_count++;
    
    if(retry_count < max_retries) {
      // Exponential backoff: 1, 2, 4, 8, 16 seconds
      clock_time_t backoff = CLOCK_SECOND * (1 << retry_count);
      printf("Retry %d/%d in %lu seconds\n", 
             retry_count, max_retries, backoff / CLOCK_SECOND);
      ctimer_set(&retry_timer, backoff, retry_callback, NULL);
    } else {
      printf("Max retries reached - giving up\n");
      retry_count = 0;
    }
  }
}

void send_with_retry(void)
{
  retry_count = 0;
  retry_callback(NULL);  // Start immediately
}
```

### 4. Real-Time Timers (rtimer)

**Preemptive** timers for time-critical operations:

```c
static struct rtimer real_timer;

void rtimer_callback(struct rtimer *t, void *ptr)
{
  // This executes with high priority!
  // Can preempt running protothreads
}

// Initialize rtimer subsystem
rtimer_init();

// Schedule rtimer
rtimer_set(&real_timer, RTIMER_NOW() + RTIMER_SECOND, 0, 
           rtimer_callback, NULL);
```

**Characteristics:**
- **Only preemptive timer** in Contiki-NG
- Uses absolute time (not relative)
- For time-predictable operations
- Higher overhead than other timers
- Executes in interrupt context (keep callbacks SHORT)

**⚠️ CRITICAL**: rtimer callbacks run at interrupt level - avoid:
- Long computations
- Blocking operations
- printf() or other I/O (may corrupt state)
- Calling process_post() directly

**Example 1: Precise Sampling at Fixed Intervals**
```c
static struct rtimer sampling_timer;
static uint16_t sample_buffer[100];
static int sample_index = 0;

void rtimer_sampling_callback(struct rtimer *t, void *ptr)
{
  // High-precision sampling (runs every 10ms exactly)
  if(sample_index < 100) {
    // Read ADC value with minimal jitter
    sample_buffer[sample_index++] = adc_read_fast();
    
    // Schedule next sample at precise interval
    rtimer_set(t, RTIMER_TIME(t) + (RTIMER_SECOND / 100), 1,
               rtimer_sampling_callback, ptr);
  } else {
    // Sampling complete - post event to process
    process_post(&data_process, sample_complete_event, NULL);
    sample_index = 0;
  }
}

void start_precise_sampling(void)
{
  sample_index = 0;
  rtimer_set(&sampling_timer, RTIMER_NOW() + RTIMER_SECOND / 100, 1,
             rtimer_sampling_callback, NULL);
}
```

**Example 2: Time-Synchronized Radio Operations**
```c
static struct rtimer radio_timer;

typedef struct {
  rtimer_clock_t sync_time;
  int slot_number;
} tdma_context_t;

static tdma_context_t tdma_ctx;

void rtimer_radio_callback(struct rtimer *t, void *ptr)
{
  tdma_context_t *ctx = (tdma_context_t *)ptr;
  
  // Execute radio TX/RX at precise time slot
  if(ctx->slot_number == MY_SLOT) {
    // Our turn to transmit
    radio_transmit_packet();
  } else {
    // Listen for incoming packets
    radio_receive_mode();
  }
  
  // Move to next slot (10ms slots)
  ctx->slot_number = (ctx->slot_number + 1) % TOTAL_SLOTS;
  
  // Schedule next slot at precise time
  rtimer_set(t, RTIMER_TIME(t) + (RTIMER_SECOND / 100), 1,
             rtimer_radio_callback, ptr);
}

void start_tdma_sync(rtimer_clock_t network_sync_time)
{
  tdma_ctx.sync_time = network_sync_time;
  tdma_ctx.slot_number = 0;
  
  rtimer_set(&radio_timer, network_sync_time, 1,
             rtimer_radio_callback, &tdma_ctx);
}
```

**Example 3: Watchdog with Precise Timing**
```c
static struct rtimer watchdog_timer;
static volatile int heartbeat_received = 0;

void rtimer_watchdog_callback(struct rtimer *t, void *ptr)
{
  if(!heartbeat_received) {
    // No heartbeat in last interval - trigger emergency action
    // Use process_post() carefully or set flag for main process
    emergency_flag = 1;
  }
  
  heartbeat_received = 0;  // Reset for next check
  
  // Check again in exactly 1 second
  rtimer_set(t, RTIMER_TIME(t) + RTIMER_SECOND, 1,
             rtimer_watchdog_callback, ptr);
}

void signal_heartbeat(void)
{
  heartbeat_received = 1;
}

void start_watchdog(void)
{
  heartbeat_received = 1;
  rtimer_set(&watchdog_timer, RTIMER_NOW() + RTIMER_SECOND, 1,
             rtimer_watchdog_callback, NULL);
}
```

**Example 4: Measuring Precise Timing with rtimer**
```c
static rtimer_clock_t start_time;

void start_timing_measurement(void)
{
  start_time = RTIMER_NOW();
}

unsigned long get_elapsed_microseconds(void)
{
  rtimer_clock_t elapsed = RTIMER_NOW() - start_time;
  
  // Convert rtimer ticks to microseconds
  // (depends on platform - example for 32kHz clock)
  return (elapsed * 1000000UL) / RTIMER_SECOND;
}

// Example usage in code profiling
void profile_function(void)
{
  start_timing_measurement();
  
  // Execute function to measure
  some_critical_function();
  
  unsigned long us = get_elapsed_microseconds();
  printf("Function took %lu microseconds\n", us);
}
```

**Comparison: When to Use Which Timer**

| Use Case | Best Timer | Reason |
|----------|-----------|--------|
| Periodic sensor readings | etimer | Simple, integrates with protothreads |
| Network packet timeouts | etimer | Event-driven, non-blocking |
| Node expiration tracking | stimer | Long periods (minutes), low memory |
| Button debouncing | ctimer | Callback fires automatically |
| LED blinking | ctimer or etimer | Both work well |
| Retry with backoff | ctimer | Callback-based state machine |
| Precise ADC sampling | rtimer | Time-predictable, preemptive |
| TDMA time slots | rtimer | Exact timing critical |
| Connection keep-alive | stimer | Long timeout periods |
| Rate limiting | stimer | Hour-long windows |

---

## Networking in IoT <a name="networking"></a>

### Protocol Stack

Contiki-NG supports full IPv6 networking stack:

```
Application Layer:  MQTT, CoAP, HTTP
Transport Layer:    UDP, TCP
Network Layer:      IPv6, RPL
Adaptation Layer:   6LoWPAN
MAC Layer:          CSMA, TSCH
Physical Layer:     IEEE 802.15.4
```

### 6LoWPAN (IPv6 over Low-Power Wireless)

**6LoWPAN** enables IPv6 communication over IEEE 802.15.4 networks:
- Header compression (reduces overhead)
- Fragmentation and reassembly
- Adapts IPv6 to resource-constrained networks

### NullNet: Simplest Networking

For basic broadcast/unicast without routing:

```c
#include "net/netstack.h"
#include "net/nullnet/nullnet.h"

void input_callback(const void *data, uint16_t len,
                    const linkaddr_t *src, const linkaddr_t *dest)
{
  printf("Received %d bytes from ", len);
  printf("%02x:%02x\n", src->u8[0], src->u8[1]);
}

PROCESS_THREAD(nullnet_process, ev, data)
{
  static uint8_t buffer[32];
  
  PROCESS_BEGIN();
  
  // Register callback
  nullnet_set_input_callback(input_callback);
  
  // Send broadcast
  nullnet_buf = buffer;
  nullnet_len = sizeof(buffer);
  NETSTACK_NETWORK.output(NULL);  // NULL = broadcast
  
  PROCESS_END();
}
```

### Simple UDP

For UDP communication over IPv6/RPL:

```c
#include "net/ipv6/simple-udp.h"

static struct simple_udp_connection udp_conn;

void udp_rx_callback(struct simple_udp_connection *c,
                     const uip_ipaddr_t *sender_addr,
                     uint16_t sender_port,
                     const uip_ipaddr_t *receiver_addr,
                     uint16_t receiver_port,
                     const uint8_t *data,
                     uint16_t datalen)
{
  printf("Received UDP packet\n");
}

PROCESS_THREAD(udp_process, ev, data)
{
  PROCESS_BEGIN();
  
  // Register UDP connection
  simple_udp_register(&udp_conn, 5678, NULL, 8765, udp_rx_callback);
  
  // Send UDP packet
  uip_ipaddr_t dest_addr;
  simple_udp_sendto(&udp_conn, "Hello", 5, &dest_addr);
  
  PROCESS_END();
}
```

---

## RPL (Routing Protocol for Low-Power and Lossy Networks) <a name="rpl"></a>

### Overview

**RPL** creates tree-shaped routing topology for IoT networks:
- One **root node** (connected to internet)
- **Tree structure** radiating from root
- Optimized for **many-to-one** traffic patterns (sensors → cloud)

### How RPL Works

1. **Root announces itself** via DIO (DODAG Information Object) messages
2. **Nodes receive DIOs** and evaluate neighbors as potential parents
3. **Objective function** ranks neighbors (link quality, hop count, etc.)
4. **Each node selects best parent** → tree forms naturally
5. **Tree maintained** via periodic DIO messages

### Objective Functions

**OF0 (Objective Function Zero):**
- Simple, fast computation
- Selects "good enough" parent + backup
- May be unstable or inefficient
- Low processing overhead

**MRHOF (Minimum Rank with Hysteresis Objective Function):**
- Sophisticated link quality metrics
- Considers stability over time
- Hysteresis prevents parent flipping
- Higher processing overhead
- More reliable and efficient routes

### Traffic Flow

**Upward (to root):**
- DIO messages build tree structure
- Data flows naturally toward root via selected parents

**Downward (from root):**
- DAO (Destination Advertisement Object) messages
- Advertise routes to specific destinations
- Supports point-to-point and multicast

### RPL in Contiki-NG

**Starting as root:**
```c
#include "net/routing/routing.h"

PROCESS_THREAD(root_process, ev, data)
{
  PROCESS_BEGIN();
  
  // Initialize as RPL root (border router)
  NETSTACK_ROUTING.root_start();
  
  PROCESS_END();
}
```

**Normal node** (no code needed - automatically joins network):
```c
// RPL automatically handles:
// - Parent selection
// - Route maintenance  
// - Forwarding packets toward root
```

### Example: UDP Server at Root

```c
#include "contiki.h"
#include "net/routing/routing.h"
#include "net/ipv6/simple-udp.h"

static struct simple_udp_connection udp_conn;

PROCESS(udp_server_process, "UDP server");
AUTOSTART_PROCESSES(&udp_server_process);

static void udp_rx_callback(struct simple_udp_connection *c,
                           const uip_ipaddr_t *sender_addr,
                           uint16_t sender_port,
                           const uip_ipaddr_t *receiver_addr,
                           uint16_t receiver_port,
                           const uint8_t *data,
                           uint16_t datalen)
{
  unsigned count = *(unsigned *)data;
  printf("Received request %u from sensor\n", count);
  
  // Send response
  simple_udp_sendto(&udp_conn, &count, sizeof(count), sender_addr);
}

PROCESS_THREAD(udp_server_process, ev, data)
{
  PROCESS_BEGIN();

  // Become RPL root
  NETSTACK_ROUTING.root_start();

  // Initialize UDP connection
  simple_udp_register(&udp_conn, 5678, NULL, 8765, udp_rx_callback);

  PROCESS_END();
}
```

---

## MQTT Protocol Integration <a name="mqtt"></a>

### Overview

**MQTT** (Message Queuing Telemetry Transport) is a lightweight pub/sub protocol ideal for IoT:
- Publish/Subscribe model with broker
- Topics for message routing
- Quality of Service (QoS) levels
- Small code footprint

### Contiki-NG MQTT Implementation

**Characteristics:**
- Compliant with MQTT v3.1
- **No QoS level 2** (too memory-intensive)
- Can act as publisher, subscriber, or both
- Runs over TCP/IPv6/RPL stack

### MQTT API

**1. Connection Setup:**
```c
#include "net/app-layer/mqtt/mqtt.h"

static struct mqtt_connection conn;

// Event callback - handles all MQTT events
void mqtt_event_callback(struct mqtt_connection *m,
                        mqtt_event_t event,
                        void *data)
{
  switch(event) {
    case MQTT_EVENT_CONNECTED:
      printf("Connected to broker\n");
      break;
    case MQTT_EVENT_DISCONNECTED:
      printf("Disconnected\n");
      break;
    case MQTT_EVENT_PUBLISH:
      printf("Received message\n");
      break;
    // ... handle other events
  }
}

// Register connection
mqtt_register(&conn, process, "client-id", mqtt_event_callback, 128);
```

**2. Connect to Broker:**
```c
mqtt_connect(&conn, "mqtt-broker.example.com", 1883, 60);
// Parameters: connection, host, port, keep-alive
```

**3. Subscribe to Topic:**
```c
mqtt_subscribe(&conn, NULL, "sensors/temperature", MQTT_QOS_LEVEL_0);
```

**4. Publish Message:**
```c
char *topic = "sensors/temperature";
char *payload = "22.5";

mqtt_publish(&conn, NULL, topic, (uint8_t *)payload, 
             strlen(payload), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
```

### Complete MQTT Example

```c
#include "contiki.h"
#include "net/routing/routing.h"
#include "net/app-layer/mqtt/mqtt.h"
#include <string.h>

PROCESS(mqtt_client_process, "MQTT Client");
AUTOSTART_PROCESSES(&mqtt_client_process);

static struct mqtt_connection conn;
static char *broker_ip = "fd00::1";  // IPv6 address
static uint16_t broker_port = 1883;

void mqtt_event_callback(struct mqtt_connection *m,
                        mqtt_event_t event,
                        void *data)
{
  switch(event) {
    case MQTT_EVENT_CONNECTED:
      printf("Connected to MQTT broker\n");
      // Subscribe to topic
      mqtt_subscribe(&conn, NULL, "commands/#", MQTT_QOS_LEVEL_0);
      break;
      
    case MQTT_EVENT_DISCONNECTED:
      printf("Disconnected from broker\n");
      break;
      
    case MQTT_EVENT_PUBLISH:
      {
        struct mqtt_message *msg = (struct mqtt_message *)data;
        printf("Received: %s\n", msg->topic);
        // Process message payload
      }
      break;
      
    default:
      break;
  }
}

PROCESS_THREAD(mqtt_client_process, ev, data)
{
  static struct etimer periodic_timer;
  static char payload[32];
  static int counter = 0;
  
  PROCESS_BEGIN();
  
  // Wait for network to be ready
  PROCESS_PAUSE();
  
  // Register MQTT connection
  mqtt_register(&conn, &mqtt_client_process, "contiki-sensor",
                mqtt_event_callback, 256);
  
  // Connect to broker
  mqtt_connect(&conn, broker_ip, broker_port, 60);
  
  etimer_set(&periodic_timer, CLOCK_SECOND * 10);
  
  while(1) {
    PROCESS_WAIT_EVENT();
    
    if(etimer_expired(&periodic_timer)) {
      // Publish sensor reading
      sprintf(payload, "Reading: %d", counter++);
      mqtt_publish(&conn, NULL, "sensors/data", 
                   (uint8_t *)payload, strlen(payload),
                   MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
      
      etimer_reset(&periodic_timer);
    }
  }
  
  PROCESS_END();
}
```

### Mosquitto MQTT Broker

**Mosquitto** (Eclipse Mosquitto) is the most commonly used open-source MQTT broker for IoT development and testing.

**What is an MQTT Broker?**

The broker acts as a central message hub:
1. **Receives messages** from publishers (IoT devices sending sensor data)
2. **Routes messages** to subscribers based on topic subscriptions
3. **Manages connections** from multiple MQTT clients
4. **Handles QoS levels** (Quality of Service guarantees)
5. **Maintains message queues** for offline clients

**Architecture with Contiki-NG:**

```
[Contiki Device A] --publish("temp", "22°C")--> 
                                                [Mosquitto Broker]
[Contiki Device B] <--receive("temp", "22°C")--
                   (subscribed to "temp")
```

**Installation:**

```bash
# Ubuntu/Debian
sudo apt-get install mosquitto mosquitto-clients

# macOS
brew install mosquitto

# Start broker
mosquitto -v  # Verbose mode shows all connections/messages
```

**Configuration:**

Default config: `/etc/mosquitto/mosquitto.conf`

Basic config for local testing:
```conf
listener 1883
allow_anonymous true
```

**Testing with Command-Line Tools:**

```bash
# Subscribe to topic (terminal 1)
mosquitto_sub -h localhost -t "sensors/#" -v

# Publish message (terminal 2)
mosquitto_pub -h localhost -t "sensors/temperature" -m "22.5"

# Subscribe with IPv6
mosquitto_sub -h fd00::1 -t "test/topic"
```

**Common Options:**
- `-h <host>` - Broker hostname/IP (default: localhost)
- `-p <port>` - Port (default: 1883)
- `-t <topic>` - Topic to subscribe/publish
- `-m <message>` - Message payload
- `-v` - Verbose (show topic names)
- `-q <qos>` - QoS level (0, 1, or 2)

**Using with Contiki-NG:**

1. **Run Mosquitto** on your PC or border router
2. **Configure broker IP** in your Contiki code:
   ```c
   static char *broker_ip = "fd00::1";  // IPv6 of PC/border router
   ```
3. **Ensure routing** via RPL border router
4. **Monitor traffic** using `mosquitto_sub -h localhost -t "#" -v`

### Border Router Configuration

To connect IoT network to internet via MQTT:

```
[IoT Devices] <-- 802.15.4/6LoWPAN/RPL --> [Border Router] <-- Ethernet/WiFi --> [MQTT Broker (Mosquitto)]
```

**Setup:**
1. Border router runs `rpl-border-router` example
2. Mosquitto runs on same machine as border router (or accessible via network)
3. Contiki devices connect to Mosquitto using IPv6 address of border router
4. Border router bridges 802.15.4 network with Ethernet/WiFi

**Typical IPv6 Setup:**
- **Border router**: `fd00::1` (RPL root)
- **Mosquitto**: Running on same machine, listening on all interfaces
- **IoT devices**: Get IPv6 addresses from RPL (e.g., `fd00::212:4b00:...`)

---

## Cooja Simulator <a name="cooja-simulator"></a>

### Overview

**Cooja** is Contiki-NG's integrated network simulator:
- Full-featured GUI for IoT network simulation
- Multiple node types (Cooja motes, Sky motes)
- Various wireless channel models
- Reproducible simulations via random seeds

### Node Types

**1. Cooja Motes:**
- C processes running in JVM
- Fastest simulation speed
- No memory limits
- Local processing takes zero simulation time
- Best for: Protocol development, large networks

**2. Sky Motes (MSPSim):**
- Emulates MSP430 microcontroller instruction-by-instruction
- Time-accurate simulation
- Realistic memory constraints
- Slower simulation speed
- Best for: Timing-sensitive code, realistic behavior

### Getting Started with Cooja

**1. Start Cooja:**
```bash
cd tools/cooja
ant run
```

**2. Create New Simulation:**
- File → New Simulation
- Set radio channel model (UDGM, Unit Disk Graph Medium)
- Set random seed (for reproducibility)

**3. Add Nodes:**
- Motes → Add Motes → Create New Mote Type
- Select mote type (Cooja mote or Sky mote)
- Browse to application `.c` file
- Compile
- Position nodes in network view

**4. Run Simulation:**
- Click Start
- Observe mote output window
- Monitor radio activity in timeline
- Use plugins (e.g., Radio Logger) for debugging

### Example Simulation Scenario

```
Simulation: Temperature Sensor Network
- 1 Sky mote (root/border router) running rpl-border-router
- 5 Cooja motes (sensors) running temperature-client
- UDGM Distance Loss radio model
- Random seed: 123456 (reproducible)
```

### Cooja Interface

**Network View:**
- Visualize node placement
- Shows radio range circles
- Drag nodes to reposition

**Mote Output:**
- Console output from all nodes
- Filter by node ID
- Timestamps in simulation time

**Timeline:**
- Radio activity visualization
- Packet transmission/reception
- Useful for debugging collisions

**Simulation Control:**
- Start/Stop/Step through simulation
- Speed control (real-time vs. fast-forward)
- Set breakpoints

---

## Practical Examples <a name="practical-examples"></a>

### Example 1: Periodic Sensor Reading

```c
#include "contiki.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include <stdio.h>

PROCESS(sensor_process, "Sensor Reading Process");
AUTOSTART_PROCESSES(&sensor_process);

PROCESS_THREAD(sensor_process, ev, data)
{
  static struct etimer timer;
  static int temperature = 20;  // Simulated sensor
  
  PROCESS_BEGIN();
  
  printf("Sensor process started\n");
  etimer_set(&timer, CLOCK_SECOND * 5);
  
  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer));
    
    // Simulate reading sensor
    temperature = 20 + (random_rand() % 10);
    printf("Temperature: %d C\n", temperature);
    
    // Blink LED to indicate reading
    leds_toggle(LEDS_GREEN);
    
    etimer_reset(&timer);
  }
  
  PROCESS_END();
}
```

### Example 2: Multi-Process Communication

```c
#include "contiki.h"
#include <stdio.h>

// Custom event type
static process_event_t sensor_event;

PROCESS(sensor_process, "Sensor");
PROCESS(logger_process, "Logger");
AUTOSTART_PROCESSES(&sensor_process, &logger_process);

// Sensor process - generates events
PROCESS_THREAD(sensor_process, ev, data)
{
  static struct etimer timer;
  static int reading = 0;
  
  PROCESS_BEGIN();
  
  // Allocate custom event
  sensor_event = process_alloc_event();
  
  etimer_set(&timer, CLOCK_SECOND * 3);
  
  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer));
    
    reading++;
    printf("Sensor: Broadcasting reading %d\n", reading);
    
    // Broadcast event to all processes
    process_post(PROCESS_BROADCAST, sensor_event, &reading);
    
    etimer_reset(&timer);
  }
  
  PROCESS_END();
}

// Logger process - receives events
PROCESS_THREAD(logger_process, ev, data)
{
  PROCESS_BEGIN();
  
  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(ev == sensor_event);
    
    int *reading = (int *)data;
    printf("Logger: Recorded reading %d\n", *reading);
  }
  
  PROCESS_END();
}
```

### Example 3: NullNet Broadcast

```c
#include "contiki.h"
#include "net/netstack.h"
#include "net/nullnet/nullnet.h"
#include <string.h>

#define SEND_INTERVAL (8 * CLOCK_SECOND)

PROCESS(nullnet_example_process, "NullNet broadcast example");
AUTOSTART_PROCESSES(&nullnet_example_process);

void input_callback(const void *data, uint16_t len,
                    const linkaddr_t *src, const linkaddr_t *dest)
{
  if(len == sizeof(unsigned)) {
    unsigned count;
    memcpy(&count, data, sizeof(count));
    printf("Received %u from %02x:%02x\n", count, src->u8[0], src->u8[1]);
  }
}

PROCESS_THREAD(nullnet_example_process, ev, data)
{
  static struct etimer periodic_timer;
  static unsigned count = 0;

  PROCESS_BEGIN();

  // Initialize NullNet
  nullnet_buf = (uint8_t *)&count;
  nullnet_len = sizeof(count);
  nullnet_set_input_callback(input_callback);

  etimer_set(&periodic_timer, SEND_INTERVAL);
  
  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));
    
    printf("Sending %u\n", count);
    memcpy(nullnet_buf, &count, sizeof(count));
    
    NETSTACK_NETWORK.output(NULL);  // Broadcast
    count++;
    
    etimer_reset(&periodic_timer);
  }

  PROCESS_END();
}
```

### Example 4: Button and LED Interaction

```c
#include "contiki.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include <stdio.h>

PROCESS(button_led_process, "Button LED Process");
AUTOSTART_PROCESSES(&button_led_process);

PROCESS_THREAD(button_led_process, ev, data)
{
  static int led_state = 0;
  
  PROCESS_BEGIN();
  
  printf("Press button to toggle LED\n");
  
  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(ev == button_hal_press_event);
    
    // Toggle LED on button press
    led_state = !led_state;
    
    if(led_state) {
      leds_on(LEDS_RED);
      printf("LED ON\n");
    } else {
      leds_off(LEDS_RED);
      printf("LED OFF\n");
    }
  }
  
  PROCESS_END();
}
```

---

## Example Programs by Concept <a name="examples-by-concept"></a>

This section organizes Contiki-NG example programs by the concepts they demonstrate, helping you quickly find relevant code for learning or reference.

### Basic Concepts

#### Getting Started
- **`examples/hello-world/`** - Basic process, AUTOSTART, printf output
- **`examples/blink/`** - LED control, simple process structure
- **`examples/blink-shell/`** - LED control + shell interface

#### Timers
- **`examples/timers/`** - All timer types: etimer, ctimer, stimer, rtimer
- **`examples/blink-dutycycle/`** - Periodic LED blinking with etimers
- **`examples/labs/ex02/`** - ctimer callbacks and rtimer (preemptive scheduling)

#### Process Management
- **`examples/protothreads-events/`** - Custom events, inter-process communication
- **`examples/labs/ex01/`** - Producer-consumer pattern with custom events
- **`examples/soft-hard-state/`** - State management patterns

### Hardware Interaction

#### Sensors and Actuators
- **`examples/dev/`** - Device drivers (buttons, sensors, LEDs)
- **`examples/platform-specific/`** - Platform-specific hardware features

#### Energy Management
- **`examples/blink-energest/`** - Energy estimation and monitoring
- **`examples/benchmarks/`** - Performance and energy benchmarking

### Networking - MAC Layer

#### NullNet (Simplest)
- **`examples/nullnet/`** - Broadcast/unicast without routing
  - Concepts: Link-layer addressing, callbacks, basic radio operation

#### 6TiSCH
- **`examples/6tisch/`** - Time-slotted channel hopping
  - Concepts: TSCH scheduling, time synchronization, channel hopping

### Networking - Network Layer (IPv6/RPL)

#### RPL Routing
- **`examples/rpl-border-router/`** - RPL root/border router setup
  - Concepts: NETSTACK_ROUTING.root_start(), bridging networks
- **`examples/rpl-udp/`** - UDP client/server over RPL
  - Concepts: simple-udp API, reachability checking, DAG root communication
- **`examples/rpl-udp-tsch/`** - RPL with TSCH MAC layer
  - Concepts: Combining routing with deterministic MAC
- **`examples/labs/ex03/`** - UDP ping-pong over RPL
  - Concepts: Bidirectional UDP, packet loss handling, sequence numbers

#### IPv6 Multicast
- **`examples/multicast/`** - IPv6 multicast communication
  - Concepts: Group addressing, efficient one-to-many communication

#### IPv4/IPv6 Translation
- **`examples/ip64-router/`** - IPv4/IPv6 gateway functionality
  - Concepts: Bridging legacy IPv4 networks with IoT

### Application Protocols

#### CoAP (Constrained Application Protocol)
- **`examples/coap/`** - RESTful services for IoT
  - Concepts: Resource discovery, GET/POST/PUT/DELETE, observe pattern

#### MQTT
- **`examples/mqtt-client/`** - MQTT publish/subscribe client
  - Concepts: Broker connection, topics, QoS levels
- **`examples/mqtt-demo/`** - Complete MQTT application demo
  - Concepts: Sensor data publishing, command handling

#### LWM2M (Lightweight M2M)
- **`examples/lwm2m-ipso-objects/`** - Device management protocol
  - Concepts: Object model, device registration, resource management

#### WebSocket
- **`examples/websocket/`** - WebSocket client implementation
  - Concepts: Real-time bidirectional communication over HTTP

### Storage and Persistence

#### File Systems
- **`examples/storage/`** - Coffee filesystem operations
  - Concepts: File I/O, persistent storage, flash memory management

### Testing and Debugging

#### Network Analysis
- **`examples/sensniff/`** - Packet sniffing and network monitoring
  - Concepts: Wireshark integration, protocol analysis

#### Serial Communication
- **`examples/slip-radio/`** - SLIP-based radio interface
  - Concepts: Serial line IP, tunneling packets

### Library Demonstrations

#### Utilities
- **`examples/libs/`** - Various library functions
  - Concepts: Crypto, compression, data structures

### Lab Exercises (Complete Applications)

#### Lab Exercise 1: Producer-Consumer
- **`examples/labs/ex01/ex1.c`**
  - **Concepts**: Custom events, process communication, queue implementation, static variables, event timers

#### Lab Exercise 2: Timer Callbacks
- **`examples/labs/ex02/ex2.c`**
  - **Concepts**: ctimer callbacks, rtimer (preemptive), timer rescheduling, rtimer_init()

#### Lab Exercise 3: UDP Client-Server
- **`examples/labs/ex03/`**
  - `ex3-server.c`: RPL root setup, UDP server, packet counting
  - `ex3-client.c`: RPL client, DAG reachability, UDP communication with timeout
  - **Concepts**: Simple UDP API, RPL routing, IPv6 addressing, packet loss handling, logging macros

### Quick Reference by Use Case

**Want to...**
- **Blink an LED?** → `examples/blink/`
- **Read a button?** → `examples/dev/`
- **Send periodic messages?** → `examples/rpl-udp/`
- **Create REST API?** → `examples/coap/`
- **Publish sensor data?** → `examples/mqtt-client/`
- **Build mesh network?** → `examples/rpl-border-router/` + `examples/rpl-udp/`
- **Save data to flash?** → `examples/storage/`
- **Monitor energy use?** → `examples/blink-energest/`
- **Debug network packets?** → `examples/sensniff/`
- **Test inter-process communication?** → `examples/protothreads-events/` or `examples/labs/ex01/`
- **Learn all timer types?** → `examples/timers/` or `examples/labs/ex02/`
- **Build UDP application?** → `examples/rpl-udp/` or `examples/labs/ex03/`

---

## Development Workflow <a name="development-workflow"></a>

### Compilation

**For Native Platform (testing on host):**
```bash
cd examples/hello-world
make TARGET=native
./hello-world.native
```

**For Real Hardware (e.g., CC2538):**
```bash
make TARGET=zoul BOARD=firefly
```

**For Cooja Simulation:**
- Compile automatically when adding mote to Cooja
- Or manually: `make TARGET=cooja`

### Project Structure

Typical Contiki-NG application:
```
my-app/
├── my-app.c           # Main application code
├── Makefile           # Build configuration
├── project-conf.h     # Project-specific configuration (optional)
└── README.md          # Documentation
```

**Makefile example:**
```makefile
CONTIKI_PROJECT = my-app
all: $(CONTIKI_PROJECT)

CONTIKI = ../..  # Path to Contiki-NG root
include $(CONTIKI)/Makefile.include
```

### Configuration

**project-conf.h** overrides default settings:
```c
#ifndef PROJECT_CONF_H_
#define PROJECT_CONF_H_

// Networking configuration
#define LOG_LEVEL_APP LOG_LEVEL_INFO
#define NETSTACK_CONF_WITH_IPV6 1

// RPL configuration
#define RPL_CONF_SUPPORTED 1

// TSCH configuration
#define MAC_CONF_WITH_TSCH 0

#endif /* PROJECT_CONF_H_ */
```

### Debugging

**1. Printf debugging:**
```c
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

LOG_INFO("Message: %d\n", value);
LOG_WARN("Warning!\n");
LOG_ERR("Error occurred\n");
```

**2. Cooja debugging:**
- Mote output window shows all printf
- Timeline shows radio activity
- Add breakpoints in simulation
- Single-step through code

**3. Hardware debugging:**
- Use serial console (USB)
- Monitor with `make login`
- Flash LEDs for visual debugging

### Deployment

**Upload to hardware:**
```bash
# Flash program
make TARGET=zoul BOARD=firefly my-app.upload

# Connect to serial console
make TARGET=zoul BOARD=firefly login
```

**Border router setup:**
```bash
cd examples/rpl-border-router
make TARGET=zoul BOARD=firefly
make TARGET=zoul BOARD=firefly border-router.upload
make connect-router-cooja  # Or connect-router-native
```

---

## Best Practices

### Memory Management
1. Always use `static` for persistent local variables
2. Minimize dynamic memory allocation
3. Reuse buffers where possible
4. Check stack usage during compilation

### Concurrency
1. Keep protothread code simple and sequential
2. Use events for inter-process communication
3. Avoid long-running operations in single iteration
4. Use `PROCESS_YIELD()` in long loops

### Networking
1. Use RPL for multi-hop networks
2. Implement exponential backoff for retransmissions
3. Minimize packet size (header overhead significant)
4. Use QoS level 0 for MQTT (QoS 2 unsupported)

### Energy Efficiency
1. Use event-driven design (avoid polling)
2. Maximize sleep time between operations
3. Batch network transmissions
4. Disable radio when not needed

### Testing
1. Test on native platform first (rapid iteration)
2. Validate in Cooja simulator (network behavior)
3. Deploy to hardware for final validation
4. Use reproducible random seeds in simulations

---

## Resources

**Official Documentation:**
- Contiki-NG Website: https://contiki-ng.org
- GitHub Repository: https://github.com/contiki-ng/contiki-ng
- Documentation: https://docs.contiki-ng.org

**Community:**
- Mailing list: contiki-ng-users@googlegroups.com
- GitHub Issues: Bug reports and feature requests

**Hardware Platforms:**
- Zolertia Firefly (Zoul)
- TI CC26xx LaunchPad
- Nordic nRF52 DK
- OpenMote

**Related Technologies:**
- 6LoWPAN: RFC 6282, RFC 4944
- RPL: RFC 6550
- CoAP: RFC 7252
- MQTT: http://mqtt.org

---

## Conclusion

Contiki-NG provides a mature, production-ready platform for IoT development. Its unique protothread-based concurrency model, comprehensive networking stack, and excellent tooling (especially Cooja simulator) make it ideal for both academic research and industrial deployment.

Key takeaways:
- **Protothreads** offer thread-like programming without memory overhead
- **Static variables** are essential for state persistence
- **Event-driven** architecture maximizes energy efficiency
- **RPL** provides efficient multi-hop routing for IoT
- **Cooja** enables rapid development and testing

Start with simple examples, use the simulator extensively, and gradually move to real hardware as your application matures. The community is active and helpful - don't hesitate to ask questions!

---

*This guide is based on lecture materials from NSDS (Networked Software for Distributed Systems) course at Politecnico di Milano, Prof. Luca Mottola.*
