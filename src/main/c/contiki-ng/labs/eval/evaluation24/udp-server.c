/*
 * RPL Monitor Server
 * Tracks RPL topology by receiving parent information from nodes
 * Prints topology every minute and removes stale nodes after 3 minutes
 */

#include "contiki.h"
#include "net/ipv6/simple-udp.h"
#include "net/netstack.h"
#include "net/routing/routing.h"

#include "sys/log.h"
#define LOG_MODULE "RPL-Monitor"
#define LOG_LEVEL LOG_LEVEL_INFO

#define UDP_CLIENT_PORT 8765
#define UDP_SERVER_PORT 5678

#define MAX_NODES 20
#define TIMEOUT_MINUTES 3
#define PRINT_INTERVAL (60 * CLOCK_SECOND)

static struct simple_udp_connection udp_conn;

/* Structure to receive parent information */
typedef struct {
    uip_ipaddr_t node_id;
    uip_ipaddr_t parent_id;
} parent_info_t;

/* Structure to track node information */
typedef struct {
    uip_ipaddr_t node_id;
    uip_ipaddr_t parent_id;
    unsigned long last_seen; /* In seconds */
    uint8_t active;
} node_entry_t;

static node_entry_t node_table[MAX_NODES];
static unsigned long current_time_sec = 0;

PROCESS(udp_server_process, "RPL Monitor Server");
PROCESS(print_topology_process, "Print Topology Process");
AUTOSTART_PROCESSES(&udp_server_process, &print_topology_process);

/*---------------------------------------------------------------------------*/
/* Find or create a node entry in the table */
static node_entry_t *find_or_create_node(const uip_ipaddr_t *node_id) {
    int i;
    int free_slot = -1;

    /* First, try to find existing entry */
    for (i = 0; i < MAX_NODES; i++) {
        if (node_table[i].active &&
            uip_ipaddr_cmp(&node_table[i].node_id, node_id)) {
            return &node_table[i];
        }
        if (!node_table[i].active && free_slot == -1) {
            free_slot = i;
        }
    }

    /* If not found, create new entry */
    if (free_slot != -1) {
        node_table[free_slot].active = 1;
        uip_ipaddr_copy(&node_table[free_slot].node_id, node_id);
        LOG_INFO("New node added to monitor: ");
        LOG_INFO_6ADDR(node_id);
        LOG_INFO_("\n");
        return &node_table[free_slot];
    }

    LOG_WARN("Node table full, cannot add new node\n");
    return NULL;
}
/*---------------------------------------------------------------------------*/
/* Remove stale nodes that haven't been heard from in TIMEOUT_MINUTES */
static void remove_stale_nodes(void) {
    int i;
    unsigned long timeout_sec = TIMEOUT_MINUTES * 60;

    for (i = 0; i < MAX_NODES; i++) {
        if (node_table[i].active) {
            if ((current_time_sec - node_table[i].last_seen) >= timeout_sec) {
                LOG_INFO("Removing stale node (not heard for %lu minutes): ",
                         TIMEOUT_MINUTES);
                LOG_INFO_6ADDR(&node_table[i].node_id);
                LOG_INFO_("\n");
                node_table[i].active = 0;
            }
        }
    }
}
/*---------------------------------------------------------------------------*/
/* Print the current topology */
static void print_topology(void) {
    int i;
    int active_nodes = 0;

    LOG_INFO("=== RPL Topology Monitor ===\n");
    LOG_INFO("Current time: %lu seconds\n", current_time_sec);

    for (i = 0; i < MAX_NODES; i++) {
        if (node_table[i].active) {
            active_nodes++;
            LOG_INFO("Node ");
            LOG_INFO_6ADDR(&node_table[i].node_id);
            LOG_INFO_(" -> Parent ");
            LOG_INFO_6ADDR(&node_table[i].parent_id);
            LOG_INFO_(" (last seen: %lu sec ago)\n",
                      current_time_sec - node_table[i].last_seen);
        }
    }

    LOG_INFO("Total active nodes: %d\n", active_nodes);
    LOG_INFO("===========================\n");
}
/*---------------------------------------------------------------------------*/
static void udp_rx_callback(struct simple_udp_connection *c,
                            const uip_ipaddr_t *sender_addr,
                            uint16_t sender_port,
                            const uip_ipaddr_t *receiver_addr,
                            uint16_t receiver_port, const uint8_t *data,
                            uint16_t datalen) {
    parent_info_t *info = (parent_info_t *)data;
    node_entry_t *entry;

    if (datalen != sizeof(parent_info_t)) {
        LOG_WARN("Received packet with wrong size: %d bytes\n", datalen);
        return;
    }

    LOG_INFO("Received parent info from ");
    LOG_INFO_6ADDR(&info->node_id);
    LOG_INFO_(" (parent: ");
    LOG_INFO_6ADDR(&info->parent_id);
    LOG_INFO_(")\n");

    /* Find or create node entry */
    entry = find_or_create_node(&info->node_id);

    if (entry != NULL) {
        /* Update parent if it changed */
        if (!uip_ipaddr_cmp(&entry->parent_id, &info->parent_id)) {
            LOG_INFO("Parent changed for node ");
            LOG_INFO_6ADDR(&info->node_id);
            LOG_INFO_(" from ");
            LOG_INFO_6ADDR(&entry->parent_id);
            LOG_INFO_(" to ");
            LOG_INFO_6ADDR(&info->parent_id);
            LOG_INFO_("\n");
            uip_ipaddr_copy(&entry->parent_id, &info->parent_id);
        }

        /* Update last seen time */
        entry->last_seen = current_time_sec;
    }
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_server_process, ev, data) {
    PROCESS_BEGIN();

    /* Initialize node table */
    int i;
    for (i = 0; i < MAX_NODES; i++) {
        node_table[i].active = 0;
    }

    /* Initialize DAG root */
    NETSTACK_ROUTING.root_start();
    LOG_INFO("RPL Monitor started as DAG root\n");

    /* Initialize UDP connection */
    simple_udp_register(&udp_conn, UDP_SERVER_PORT, NULL, UDP_CLIENT_PORT,
                        udp_rx_callback);

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(print_topology_process, ev, data) {
    static struct etimer periodic_timer;

    PROCESS_BEGIN();

    /* Wait a bit before starting to print */
    etimer_set(&periodic_timer, PRINT_INTERVAL);

    while (1) {
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));

        /* Increment time (approximately) */
        current_time_sec += 60;

        /* Remove stale nodes */
        remove_stale_nodes();

        /* Print current topology */
        print_topology();

        /* Reset timer for next print */
        etimer_set(&periodic_timer, PRINT_INTERVAL);
    }

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
