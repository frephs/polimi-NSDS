# Evaluation lab - Contiki-NG

## Group number: XX

## Group members

- Student 1 
- Student 2
- Student 3

## Solution description

The RPL Monitor system consists of two components: udp-client and udp-server.

**UDP Client (udp-client.c)**: Each client node periodically sends parent information to the root monitor every ~60 seconds. The client retrieves its own IPv6 address and its preferred parent's address using rpl_parent_get_ipaddr() function. A parent_info_t structure containing both addresses is transmitted via UDP to the server. Random jitter is added to prevent synchronized transmissions.

**UDP Server (udp-server.c)**: The monitor runs at the RPL root and maintains a node table tracking up to 20 nodes. When receiving parent information, it updates or creates node entries, recording the parent relationship and last-seen timestamp. A separate process prints the complete topology every 60 seconds and removes nodes not heard from in 3+ consecutive minutes. Parent changes are logged immediately for network monitoring.

**Testing**: The rpl-monitor-topology.csc simulation file creates a 7-node network with one root and six clients arranged to form a multi-level tree topology, allowing testing of parent changes by moving nodes in COOJA.

