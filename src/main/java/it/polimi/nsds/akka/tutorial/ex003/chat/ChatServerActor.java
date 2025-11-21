package it.polimi.nsds.akka.tutorial.ex003.chat;

import akka.actor.AbstractActorWithStash;
import akka.japi.pf.ReceiveBuilder;
import it.polimi.nsds.akka.tutorial.ex003.chat.messages.SleepMessage;
import it.polimi.nsds.akka.tutorial.ex003.chat.messages.TextMessage;
import it.polimi.nsds.akka.tutorial.ex003.chat.messages.WakeupMessage;

public class ChatServerActor extends AbstractActorWithStash {

    //private State state = State.AWAKE;

    /*ABSOLUTELY NOT
    enum State {
        SLEEPING,
        AWAKE
    }*/

    @Override
    public Receive createReceive() {
        return active();
    }

    public Receive active() {
        return new ReceiveBuilder()
            .match(TextMessage.class, this::onTextMessage)
            .match(SleepMessage.class, this::onSleep)
            .match(WakeupMessage.class, this::onWakeup)
            .build();
    }

    public Receive sleeping() {
        return new ReceiveBuilder()
            .match(WakeupMessage.class, this::onWakeup)
            .matchAny(message -> {
                stash();
            })
            .build();
    }

    private void onSleep(SleepMessage message) {
        System.out.println("Server going to sleep...");
        getContext().become(sleeping());
    }

    private void onWakeup(WakeupMessage message) {
        System.out.println("Server is waking up...");
        getContext().become(active());
        unstashAll();
    }

    private void onTextMessage(TextMessage message) {
        System.out.println("Received message: " + message.text);
        sender().tell(message.text, getSelf());
    }
}
