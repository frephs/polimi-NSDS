#include "contiki.h"
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#define LEN 3
#define MAX_FOR_TIMER 2

/*---------------------------------------------------------------------------*/
PROCESS(producer_process, "Producer process");
PROCESS(consumer_process, "Consumer process");
AUTOSTART_PROCESSES(&producer_process, &consumer_process);
/*---------------------------------------------------------------------------*/

// Let's declare processes
static process_event_t producer_event;
static process_event_t consumer_event;

// Queue it's the same across threads
static int queue[LEN];

// Same for index
static int i;

// Same for variables
static int pushed;
static int initialized = 0;

// Make sure all these are static

/*---------------------------------------------------------------------------*/
// PRODUCER SETUP FUNCTIONS
/*---------------------------------------------------------------------------*/

static void print_queue() {
    printf("## queue -> [");
    for (i = 0; i < LEN; i++) {
        if (i != LEN - 1) {
            printf("%d,  ", queue[i]);
        } else {
            printf("%d]\n\n", queue[i]);
        }
    }
}

static void initialize() {
    // Initialize the queue, we assume 0s are empty spaces.
    for (i = 0; i < LEN; i++) {
        queue[i] = 0;
    }
    printf("[prod] queue init\n");
    print_queue();

    srand(time(NULL));
    initialized = 1;
}

/*---------------------------------------------------------------------------*/
// PRODUCER THREAD
/*---------------------------------------------------------------------------*/

PROCESS_THREAD(producer_process, ev, data) {
    static struct etimer timer_p;
    static int random_p;

    if (initialized == 0) {
        initialize();
    }

    // Start the producer thread
    PROCESS_BEGIN();

    // let the other protothread (consumer) start
    PROCESS_PAUSE();

    // Let's create a pointer to for the event to notify the consumer
    producer_event = process_alloc_event();

    while (1) {
        // Look for a free spot in the queue
        pushed = 0;
        for (i = 0; i < LEN; i++) {
            if (queue[i] == 0) { // there's space, push data
                queue[i] = i + 1;
                pushed = 1;
                break;
            }
        }

        // If a free spot is not found
        if (pushed == 0) {
            // queue->full, wait for an event triggered by the consumer process
            printf("[prod] queue FULL -> waiting for consumer\n");
            print_queue();
            PROCESS_WAIT_EVENT_UNTIL(ev == consumer_event);
        } else {
            // If a free spot is found, let's push our data and event
            // new element -> send producer_event to let the consumer resume
            printf("[prod] new element pushed -> random timer + notify "
                   "consumer\n");
            print_queue();
            random_p = (rand() % MAX_FOR_TIMER) + 1;

            // Send event to let consumer consume
            process_post(&consumer_process, producer_event, NULL);

            // Wait some seconds before trying again to post
            etimer_set(&timer_p, CLOCK_SECOND * random_p);
            PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer_p));
        }
    }
    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
// CONSUMER THREAD
/*---------------------------------------------------------------------------*/

// It would be even better with a circular list

PROCESS_THREAD(consumer_process, ev, data) {

    static struct etimer timer_c;
    static int random_c;

    PROCESS_BEGIN();

    // Let's allocate a pointer for the event to notify the producer
    consumer_event = process_alloc_event();

    while (1) {

        if (queue[0] != 0) {
            // The queue is not empty, first element not 0
            for (i = 0; i < LEN; i++) {
                // Consume the element
                if (queue[i] == 0) {
                    // No segfault here, cannot be first element
                    queue[i - 1] = 0;
                    break;
                } else if (i == LEN - 1 && queue[i] != 0) {
                    queue[i] = 0;
                }
            }
            printf("[cons] pulled element -> random timer + notify producer\n");
            print_queue();

            // Send event to the producer, so that he can resume from suspended
            // state
            process_post(&producer_process, consumer_event, NULL);

            // Start new timer to wait for a new pull
            random_c = (rand() % MAX_FOR_TIMER) + 1;
            etimer_set(&timer_c, CLOCK_SECOND * random_c);
            PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer_c));

        } else {
            // The queue is empty
            printf("[cons] queue empty -> waiting for producer\n");
            print_queue();
            PROCESS_WAIT_EVENT_UNTIL(ev == producer_event);
        }
    }

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
