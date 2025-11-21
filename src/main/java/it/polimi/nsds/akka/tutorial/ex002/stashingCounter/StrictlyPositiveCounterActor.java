package it.polimi.nsds.akka.tutorial.ex002.stashingCounter;

import akka.actor.Props;

public class StrictlyPositiveCounterActor extends CounterActor {

    @Override
    void onDecreaseMessage(DecreaseMessage msg) {
        if (counter <= 0) {
            stash();
        } else {
            super.onDecreaseMessage(msg);
        }
    }

    @Override
    void onIncreaseMessage(IncreaseMessage msg) {
        ++counter;
        System.out.println("Counter increased to " + counter);
        unstashAll();
    }

    public static Props props() {
        return Props.create(StrictlyPositiveCounterActor.class);
    }
}
