package it.polimi.nsds.akka.tutorial.ex002.stashingCounter;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;

public class CounterActor extends AbstractActorWithStash {

    protected int counter;

    public CounterActor() {
        this.counter = 0;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(IncreaseMessage.class, this::onIncreaseMessage)
            .match(DecreaseMessage.class, this::onDecreaseMessage)
            .build();
    }

    void onIncreaseMessage(IncreaseMessage msg) {
        ++counter;
        System.out.println("Counter increased to " + counter);
    }

    void onDecreaseMessage(DecreaseMessage msg) {
        --counter;
        System.out.println("Counter decreased to " + counter);
    }

    public static Props props() {
        return Props.create(CounterActor.class);
    }
}
