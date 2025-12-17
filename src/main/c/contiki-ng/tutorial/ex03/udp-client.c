#include "contiki.h"
#include "net/ipv6/simple-udp.h"
#include "net/netstack.h"
#include "net/routing/routing.h"
#include "random.h"

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

#define WITH_SERVER_REPLY 1
#define UDP_CLIENT_PORT 8765
#define UDP_SERVER_PORT 5678

static struct simple_udp_connection udp_conn;

#define START_INTERVAL (15 * CLOCK_SECOND)
#define SEND_INTERVAL (60 * CLOCK_SECOND)

#define STOP 10

static struct simple_udp_connection udp_conn;

static unsigned recv_count = 0;
/*---------------------------------------------------------------------------*/
PROCESS(udp_client_process, "UDP client");
AUTOSTART_PROCESSES(&udp_client_process);
/*---------------------------------------------------------------------------*/

static void udp_rx_callback(struct simple_udp_connection *c,
                            const uip_ipaddr_t *sender_addr,
                            uint16_t sender_port,
                            const uip_ipaddr_t *receiver_addr,
                            uint16_t receiver_port, const uint8_t *data,
                            uint16_t datalen) {
    // We only change the client, the callback only saves the received data to a
    // static variable

    recv_count = *(unsigned *)data;

    LOG_INFO("Received response %u from ", recv_count);
    LOG_INFO_6ADDR(sender_addr);
    LOG_INFO_("\n");
}

/*---------------------------------------------------------------------------*/
// CLIENT PROCESS
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_client_process, ev, data) {
    static struct etimer periodic_timer;
    static unsigned count;
    uip_ipaddr_t dest_ipaddr;
    static struct etimer timeout_timer;

    PROCESS_BEGIN();

    while (1) {
        /* Initialize UDP connection */
        simple_udp_register(&udp_conn, UDP_CLIENT_PORT, NULL, UDP_SERVER_PORT,
                            udp_rx_callback);

        etimer_set(&periodic_timer, random_rand() % SEND_INTERVAL);

        while (1) {
            PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));

            if (NETSTACK_ROUTING.node_is_reachable() &&
                NETSTACK_ROUTING.get_root_ipaddr(&dest_ipaddr)) {

                etimer_stop(&periodic_timer);

                /* Send to DAG root */
                LOG_INFO("Starting");
                LOG_INFO("Sending request %u to ", count);
                LOG_INFO_6ADDR(&dest_ipaddr);
                LOG_INFO_("\n");

                count++;
                simple_udp_sendto(&udp_conn, &count, sizeof(count),
                                  &dest_ipaddr);

                etimer_set(&timeout_timer, 3 * CLOCK_SECOND);

                PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timeout_timer) ||
                                         recv_count == count);
                if (count != recv_count) {
                    count = 0;
                } else if (count >= STOP) {
                    break;
                }
            } else {
                LOG_INFO("Not reachable yet\n");
                // Restart from scratch
                count = 0;
                /* Add some jitter */
                etimer_set(&periodic_timer,
                           SEND_INTERVAL - CLOCK_SECOND +
                               (random_rand() % (2 * CLOCK_SECOND)));
            }
        }
    }

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
