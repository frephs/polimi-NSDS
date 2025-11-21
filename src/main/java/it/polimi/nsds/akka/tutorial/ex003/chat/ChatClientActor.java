package it.polimi.nsds.akka.tutorial.ex003.chat;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import it.polimi.nsds.akka.tutorial.ex003.chat.messages.SleepMessage;
import it.polimi.nsds.akka.tutorial.ex003.chat.messages.TextMessage;
import it.polimi.nsds.akka.tutorial.ex003.chat.messages.TriggerMessage;
import it.polimi.nsds.akka.tutorial.ex003.chat.messages.WakeupMessage;
import java.util.Random;

public class ChatClientActor extends AbstractActor {

    private static final int numberOfMessages = 10;

    ActorSelection server;

    public ChatClientActor(String serverAddress) {
        server = getContext().actorSelection(serverAddress);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(TriggerMessage.class, this::onTriggerMessage)
            .build();
    }

    public void onTriggerMessage(TriggerMessage msg) {
        System.out.println("TriggerMessage received in ChatClientActor");

        Random rand = new Random();
        for (int i = 0; i < numberOfMessages; i++) {
            try {
                switch (rand.nextInt(3)) {
                    case 0:
                        server.tell(
                            new TextMessage(
                                "This is a random message with id: " +
                                    rand.nextInt(1000)
                            ),
                            self()
                        );
                        break;
                    case 1:
                        server.tell(new SleepMessage(), self());
                        break;
                    case 2:
                        server.tell(new WakeupMessage(), self());
                        break;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Props props() {
        return Props.create(ChatClientActor.class);
    }
}
