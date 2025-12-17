# Evaluation lab - Contiki-NG

## Group number: 28

## Group members

- Francesco Gennovese
- Federico Grandi
- Jonatan Sciaky

## Solution description

We have a server and 2 clients to simulate the different scenarios.

On lock commands, the server stores locally the address of the client who locked the resource, sets a flag that states that the resource is locked, then starts a ctimer which will release the lock once expired. If the resource is already locked by the same client, the server will restart expiration timer. If the resource is already locked by another client, then the server won't do anything.

On read commands the servers always sends the stored value back to the client who requested it.

On write commands the server first checks weather the client has locked the resource (checking the flag and address); if so, then updates the value with the received value, otherwise it will deny the request and send back an error message to the client.

The clients are state machines that will send predetermined commands using timers. The scenarios are:
1. [Client1] Lock, then write
2. [Client1] Write without lock (error)
3. [Client1] Read
4. [Client1] Lock -> [Client2] Lock and write (error) -> [Client1]  Write
5. [Client1] Lock -> [Client2] Read ->[Client1] Write
6. [Client2] Lock -> timeout -> Write (error)
