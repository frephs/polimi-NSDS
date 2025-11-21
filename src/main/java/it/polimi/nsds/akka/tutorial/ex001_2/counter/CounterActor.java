package it.polimi.nsds.akka.tutorial.ex001_2.counter;

import akka.actor.AbstractActor;
import akka.actor.Props;
import it.polimi.nsds.akka.tutorial.ex001.counter.CounterMessage;
import it.polimi.nsds.akka.tutorial.ex001.counter.DecreaseMessage;
import it.polimi.nsds.akka.tutorial.ex001.counter.IncreaseMessage;

public class CounterActor extends AbstractActor {

    private int counter;

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

    void onCounterMessage(CounterMessage msg) {
        counter += msg.type == CounterMessage.TypeEnum.ADD
            ? (msg.value)
            : (-msg.value);
    }

    static Props props() {
        return Props.create(CounterActor.class);
    }
}
