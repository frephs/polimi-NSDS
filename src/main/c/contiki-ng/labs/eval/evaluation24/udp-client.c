/*
 * RPL Monitor Client
 * Sends parent information periodically to the RPL monitor at the root
 */

#include "contiki.h"
#include "net/ipv6/simple-udp.h"
#include "net/netstack.h"
#include "net/routing/routing.h"
#include "net/routing/rpl-lite/rpl.h"
#include "random.h"

#include "sys/log.h"
#define LOG_MODULE "RPL-Client"
#define LOG_LEVEL LOG_LEVEL_INFO

#define UDP_CLIENT_PORT 8765
#define UDP_SERVER_PORT 5678

#define START_INTERVAL (15 * CLOCK_SECOND)
#define SEND_INTERVAL (60 * CLOCK_SECOND)

static struct simple_udp_connection udp_conn;

/* Structure to send parent information */
typedef struct {
    uip_ipaddr_t node_id;
    uip_ipaddr_t parent_id;
} parent_info_t;

/*---------------------------------------------------------------------------*/
PROCESS(udp_client_process, "RPL Monitor Client");
AUTOSTART_PROCESSES(&udp_client_process);
/*---------------------------------------------------------------------------*/
static void udp_rx_callback(struct simple_udp_connection *c,
                            const uip_ipaddr_t *sender_addr,
                            uint16_t sender_port,
                            const uip_ipaddr_t *receiver_addr,
                            uint16_t receiver_port, const uint8_t *data,
                            uint16_t datalen) {
    LOG_INFO("Received response from monitor at ");
    LOG_INFO_6ADDR(sender_addr);
    LOG_INFO_("\n");
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_client_process, ev, data) {
    static struct etimer periodic_timer;
    static parent_info_t parent_info;
    uip_ipaddr_t dest_ipaddr;
    const uip_ipaddr_t *parent_addr;

    PROCESS_BEGIN();

    /* Initialize UDP connection */
    simple_udp_register(&udp_conn, UDP_CLIENT_PORT, NULL, UDP_SERVER_PORT,
                        udp_rx_callback);

    /* Wait a bit before starting to send */
    etimer_set(&periodic_timer,
               START_INTERVAL + (random_rand() % SEND_INTERVAL));

    while (1) {
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));

        if (NETSTACK_ROUTING.node_is_reachable() &&
            NETSTACK_ROUTING.get_root_ipaddr(&dest_ipaddr)) {
            /* Get our own address */
            uip_ipaddr_copy(&parent_info.node_id,
                            &uip_ds6_get_link_local(ADDR_PREFERRED)->ipaddr);

            /* Get parent address */
            parent_addr =
                rpl_parent_get_ipaddr(curr_instance.dag.preferred_parent);

            if (parent_addr != NULL) {
                uip_ipaddr_copy(&parent_info.parent_id, parent_addr);

                LOG_INFO("Sending parent info to monitor - My ID: ");
                LOG_INFO_6ADDR(&parent_info.node_id);
                LOG_INFO_(", Parent ID: ");
                LOG_INFO_6ADDR(&parent_info.parent_id);
                LOG_INFO_("\n");

                simple_udp_sendto(&udp_conn, &parent_info, sizeof(parent_info),
                                  &dest_ipaddr);
            } else {
                LOG_INFO("No preferred parent available yet\n");
            }
        } else {
            LOG_INFO("Root not reachable yet\n");
        }

        /* Send every ~1 minute with some jitter */
        etimer_set(&periodic_timer, SEND_INTERVAL - CLOCK_SECOND +
                                        (random_rand() % (2 * CLOCK_SECOND)));
    }

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
