/*
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of the Contiki operating system.
 *
 */

#include "contiki.h"
#include "net/ipv6/simple-udp.h"
#include "net/netstack.h"
#include "net/routing/routing.h"
#include "net/routing/rpl-lite/rpl.h"

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

#define UDP_CLIENT_PORT 8765
#define UDP_SERVER_PORT 5678

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

#define LOCK_TIMEOUT (5 * CLOCK_SECOND)
static struct simple_udp_connection udp_conn;
static int value;
static int is_locked;
static uip_ipaddr_t lock_address;
static struct ctimer lock_timeout;

PROCESS(udp_server_process, "UDP server");
AUTOSTART_PROCESSES(&udp_server_process);
/*---------------------------------------------------------------------------*/
// ctimer callback
static void release_lock_cb(void *data) {
  LOG_INFO_("Lock timeout expired, releasing lock.\n");
  is_locked = 0;
}

static void udp_rx_callback(struct simple_udp_connection *c,
                            const uip_ipaddr_t *sender_addr,
                            uint16_t sender_port,
                            const uip_ipaddr_t *receiver_addr,
                            uint16_t receiver_port, const uint8_t *data,
                            uint16_t datalen) {
  // Process requests from the client
  request_t payload = *(request_t *)data;

  if (payload.command == READ) {
    LOG_INFO_("Read request from ");
    LOG_INFO_6ADDR(sender_addr);
    LOG_INFO_("\n");

    // Respond with value
    response_t res;
    res.command = READ;
    res.status = 200;
    res.value = value;

    simple_udp_sendto(&udp_conn, &res, sizeof(res), sender_addr);
  } else if (payload.command == LOCK) {
    LOG_INFO_("Lock request from ");
    LOG_INFO_6ADDR(sender_addr);
    LOG_INFO_("\n");

    // Check whether the client already has a lock
    if (!is_locked ||
        (is_locked && uip_ipaddr_cmp(sender_addr, &lock_address))) {
      LOG_INFO_("Lock request granted.\n");
      is_locked = 1;
      uip_ipaddr_copy(&lock_address, sender_addr);

      // Set (or re-set the ctimer)
      ctimer_set(&lock_timeout, LOCK_TIMEOUT, release_lock_cb, NULL);
    } else {
      LOG_INFO_("Lock request denied, noop.\n");
    }
  } else if (payload.command == WRITE) {
    LOG_INFO_("Write request from ");
    LOG_INFO_6ADDR(sender_addr);
    LOG_INFO_("\n");

    if (is_locked && uip_ipaddr_cmp(sender_addr, &lock_address)) {
      LOG_INFO_("Write request granted.\n");

      // Write and release lock
      value = payload.value;
      is_locked = 0;
      ctimer_stop(&lock_timeout);
    } else {
      LOG_INFO_("Write request denied.\n");

      // Respond with an error
      response_t res;
      res.command = WRITE;
      res.status = 403;

      simple_udp_sendto(&udp_conn, &res, sizeof(res), sender_addr);
    }
  } else {
    LOG_WARN("Unknown command %d.\n", payload.command);
  }
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_server_process, ev, data) {
  is_locked = 0;
  value = 0;

  PROCESS_BEGIN();

  /* Initialize DAG root */
  NETSTACK_ROUTING.root_start();

  /* Initialize UDP connection */
  simple_udp_register(&udp_conn, UDP_SERVER_PORT, NULL, UDP_CLIENT_PORT,
                      udp_rx_callback);

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
