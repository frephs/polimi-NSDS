#include "contiki.h"

#include <stdio.h> /* For printf() */

/*---------------------------------------------------------------------------*/
static void ctimer_callback(void *data);
#define CTIMER_INTERVAL 2 * CLOCK_SECOND
static struct ctimer print_ctimer;
/*---------------------------------------------------------------------------*/

static void rtimer_callback(struct rtimer *t, void *data);
#define RTIMER_HARD_INTERVAL 2 * RTIMER_SECOND
static struct rtimer print_rtimer;

/*---------------------------------------------------------------------------*/
PROCESS(hello_world_ctimer, "Hello world process");
AUTOSTART_PROCESSES(&hello_world_ctimer);
/*---------------------------------------------------------------------------*/
static void ctimer_callback(void *data) {
    printf("%s", (char *)data);

    // This schedules a ctimer after the ctimer_interval but there is no queue,
    // the timer is just a static variable. This way, we schedule it, and the r
    // timer is being rescheduled it right after its execution  at the same spot
    // in which the rtimer would execute it. It works but it's redundant
    ctimer_set(&print_ctimer, CTIMER_INTERVAL, ctimer_callback,
               "Hello world CT\n")
}
/*---------------------------------------------------------------------------*/
static void rtimer_callback(struct rtimer *t, void *data) {

    printf("%s", (char *)data);

    // After the first r_timer, this callback is executed every rtimer_interval
    /* Schedule the ctimer */

    // If we fire the ctimer right now, they execute together.
    // Fires right away.
    ctimer_set(&print_ctimer, 0, ctimer_callback, "Hello world CT\n");

    /* Reschedule the rtimer */
    rtimer_set(&print_rtimer, RTIMER_NOW() + RTIMER_HARD_INTERVAL, 0,
               rtimer_callback, "Hello world RT\n");
}

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(hello_world_ctimer, ev, data) {
    PROCESS_BEGIN();

    // Set up the rtimer as it does need additional resources
    rtimer_init();

    /* Schedule the rtimer: absolute time */
    // For the first time, we execute an r_timer
    rtimer_set(&print_rtimer, RTIMER_NOW() + RTIMER_HARD_INTERVAL, 0,
               rtimer_callback, "Hello world RT\n");

    /* Only useful for platform native */
    PROCESS_WAIT_EVENT();

    PROCESS_END();
}
