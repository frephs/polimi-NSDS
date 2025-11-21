# Evaluation lab - Akka

## Group number: 28

## Group members

- Francesco Genovese [10765842] 
- Federico Grandi [10802488]
- Jonatan Sciaky [10741047]

## Description of message flows

Right after instantiating all the actors we send a two configuration messages:
- `ConfigBalancer` to the balancer, containing the `ActorRef`s of the two workers;
- `ConfigClient` to the client, containing the `ActorRef` of the balancer.

Every time the client receives a `PutMsg` it forwards it to the balancer, which will determine the primary and replica workers, and will send them the corresponding `PutAsPrimaryMsg` or `PutAsReplicaMsg`.

If the client receives a `GetMsg` it will forward it to the balancer using an ask pattern, and will then check for the class of the received message, as it could either be a `TimeoutMsg` or a `ResponseMsg`.  
The balancer will *ask* the address to the primary worker: if it times out (because it's resting) it will ask the replica worker. If both workers are resting, the balancer will then reply with a `TimeoutMsg`, otherwise it will send a `ResponseMsg` (which may or may not include the email depending on whether it was saved).

The workers are able to process `RestMsg`s and `ResumeMsg`s and will change their own behavior accordingly; when a worker is "resting" it will stop processing `GetMsg`s,  while keeping to process the other types such as put messages. 


