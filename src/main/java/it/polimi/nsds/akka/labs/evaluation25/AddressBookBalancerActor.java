package it.polimi.nsds.akka.labs.evaluation25;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class AddressBookBalancerActor extends AbstractActor {

    ActorRef worker1 = null;
    ActorRef worker0 = null;

    public AddressBookBalancerActor() {}

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ConfigBalancer.class, this::config)
            .match(PutMsg.class, this::storeEntry)
            .match(GetMsg.class, this::routeQuery)
            .build();
    }

    void config(ConfigBalancer msg) {
        worker0 = msg.getWorker0();
        worker1 = msg.getWorker1();
    }

    int splitByInitial(String s) {
        char firstChar = s.charAt(0);

        // Normalize case for comparison
        char upper = Character.toUpperCase(firstChar);

        if (upper >= 'A' && upper <= 'M') {
            return 0;
        } else {
            return 1;
        }
    }

    void routeQuery(GetMsg msg) {
        System.out.println(
            "BALANCER: Received query for name " + msg.getName()
        );

        ActorRef primaryWorker, replicaWorker;
        if (splitByInitial(msg.getName()) == 0) {
            // Worker 0: A-M
            primaryWorker = worker0;
            replicaWorker = worker1;
        } else {
            // Worker 1: L-Z
            primaryWorker = worker1;
            replicaWorker = worker0;
        }

        Future<Object> primaryFuture = Patterns.ask(primaryWorker, msg, 500);
        try {
            ResponseMsg responseMsg = (ResponseMsg) primaryFuture.result(
                Duration.create(2, TimeUnit.SECONDS),
                null
            );
            getSender().tell(responseMsg, getSelf());
        } catch (TimeoutException | InterruptedException e) {
            System.out.println(
                "BALANCER: Primary copy query for name " +
                    msg.getName() +
                    " is resting!"
            );

            Future<Object> replicaFuture = Patterns.ask(
                replicaWorker,
                msg,
                500
            );
            try {
                ResponseMsg responseMsg = (ResponseMsg) replicaFuture.result(
                    Duration.create(2, TimeUnit.SECONDS),
                    null
                );
                getSender().tell(responseMsg, getSelf());
            } catch (TimeoutException | InterruptedException e1) {
                System.out.println(
                    "BALANCER: Both copies are resting for name " +
                        msg.getName() +
                        "!"
                );
                getSender().tell(new TimeoutMsg(), getSelf());
            }
        }
    }

    void storeEntry(PutMsg msg) {
        System.out.println(
            "BALANCER: Received new entry " +
                msg.getName() +
                " - " +
                msg.getEmail()
        );

        ActorRef primaryWorker, replicaWorker;
        if (splitByInitial(msg.getName()) == 0) {
            // Worker 0: A-M
            primaryWorker = worker0;
            replicaWorker = worker1;
        } else {
            // Worker 1: L-Z
            primaryWorker = worker1;
            replicaWorker = worker0;
        }

        primaryWorker.tell(
            new PutAsPrimaryMsg(msg.getName(), msg.getEmail()),
            getSelf()
        );
        replicaWorker.tell(
            new PutAsReplicaMsg(msg.getName(), msg.getEmail()),
            getSelf()
        );
    }

    static Props props() {
        return Props.create(AddressBookBalancerActor.class);
    }
}
