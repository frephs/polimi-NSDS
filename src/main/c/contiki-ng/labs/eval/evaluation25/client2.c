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

static struct simple_udp_connection udp_conn;
static struct etimer timer;
static unsigned count = 1;

#define READ 0
#define LOCK 1
#define WRITE 2
typedef struct request_ {
  int command;
  int value;
} request_t;
typedef struct response_ {
  int command;
  int status;
  int value;
} response_t;

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
  response_t response = *(response_t *)data;

  LOG_INFO("Received response from ");
  LOG_INFO_6ADDR(sender_addr);
  LOG_INFO_("\n");

  if (response.command == READ) {
    LOG_INFO_("Read value %d\n", response.value);
  } else if (response.command == WRITE) {
    if (response.status == 403) {
      LOG_INFO_("Write error %d, resource locked.\n", response.status);
    } else {
      LOG_INFO_("Write error %d\n", response.status);
    }
  } else {
    LOG_WARN("Unknown command response.\n");
  }
}

static void send_read(uip_ipaddr_t *dest_ipaddr, int timer_seconds) {
  request_t req;

  LOG_INFO_("Requesting read\n");
  req.command = READ;
  simple_udp_sendto(&udp_conn, &req, sizeof(req), dest_ipaddr);

  etimer_set(&timer, timer_seconds * CLOCK_SECOND);
}
static void send_lock(uip_ipaddr_t *dest_ipaddr, int timer_seconds) {
  request_t req;

  LOG_INFO_("Requesting lock\n");
  req.command = LOCK;

  simple_udp_sendto(&udp_conn, &req, sizeof(req), dest_ipaddr);

  etimer_set(&timer, timer_seconds * CLOCK_SECOND);
}
static void send_write(uip_ipaddr_t *dest_ipaddr, int timer_seconds,
                       int should_error) {
  request_t req;

  if (should_error) {
    LOG_INFO_("Requesting write %d (should error)\n", count);
  } else {
    LOG_INFO_("Requesting write %d\n", count);
  }
  req.command = WRITE;
  req.value = count;
  count++;
  simple_udp_sendto(&udp_conn, &req, sizeof(req), dest_ipaddr);

  etimer_set(&timer, timer_seconds * CLOCK_SECOND);
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_client_process, ev, data) {
  static unsigned state = 0;
  uip_ipaddr_t dest_ipaddr;

  PROCESS_BEGIN();

  /* Initialize UDP connection */
  simple_udp_register(&udp_conn, UDP_CLIENT_PORT, NULL, UDP_SERVER_PORT,
                      udp_rx_callback);

  etimer_set(&timer, random_rand() % SEND_INTERVAL);
  while (1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&timer));

    if (NETSTACK_ROUTING.node_is_reachable() &&
        NETSTACK_ROUTING.get_root_ipaddr(&dest_ipaddr)) {

      if (state == 0) {
        etimer_set(&timer, 56 * CLOCK_SECOND); // Stall for a bit...
      } else if (state == 1) {
        send_lock(&dest_ipaddr, 1); // Scenario 4
      } else if (state == 2) {
        send_write(&dest_ipaddr, 10, 1); // Scenario 4
      } else if (state == 3) {
        send_read(&dest_ipaddr, 8); // Scenario 5
      } else if (state == 4) {
        send_lock(&dest_ipaddr, 9); // Scenario 6
      } else if (state == 5) {
        send_write(&dest_ipaddr, 1, 1); // Scenario 6
      }

      state++;
    } else {
      LOG_INFO("Not reachable yet\n");
      /* Add some jitter */
      etimer_set(&timer, SEND_INTERVAL - CLOCK_SECOND +
                             (random_rand() % (2 * CLOCK_SECOND)));
    }
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
