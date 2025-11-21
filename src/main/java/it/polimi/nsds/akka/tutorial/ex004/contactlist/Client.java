package it.polimi.nsds.akka.tutorial.ex004.contactlist;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.polimi.nsds.akka.tutorial.ex004.contactlist.messages.TriggerMessage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private static final int numThreads = 5;
    private static final int numMessages = 100;

    private static final String serverAddress =
        "akka.tcp://Server@127.0.0.1:6123/user/Contact-Server";

    public static void main(String[] args) {
        // Let's simulate many clients sending messages in parallel
        final ExecutorService exec = Executors.newFixedThreadPool(numThreads);

        ActorRef client = akka.actor.ActorSystem.create("Client").actorOf(
            Props.create(ClientActor.class, serverAddress)
        );

        for (int i = 0; i < numMessages; i++) {
            exec.submit(() -> {
                client.tell(
                    new TriggerMessage(TriggerMessage.TriggerType.PUT),
                    ActorRef.noSender()
                );
            });
        }
        for (int i = 0; i < numMessages; i++) {
            exec.submit(() -> {
                client.tell(
                    new TriggerMessage(TriggerMessage.TriggerType.GET),
                    ActorRef.noSender()
                );
            });
        }
    }
}
