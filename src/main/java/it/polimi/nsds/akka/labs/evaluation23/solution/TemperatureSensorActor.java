package it.polimi.nsds.akka.labs.evaluation23.solution;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import java.util.concurrent.ThreadLocalRandom;

public class TemperatureSensorActor extends AbstractActor {

    private ActorRef dispatcher;
    private static final int MIN_TEMP = 0;
    private static final int MAX_TEMP = 50;

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
            .match(ConfigMsg.class, this::configure)
            .match(GenerateMsg.class, this::onGenerate)
            .build();
    }

    private void configure(ConfigMsg msg) {
        System.out.println(
            "TEMPERATURE SENSOR: Received configuration message!"
        );
        this.dispatcher = msg.getDispatcher();
    }

    private void onGenerate(GenerateMsg msg) {
        System.out.println("TEMPERATURE SENSOR: Sensing temperature!");
        int temp = ThreadLocalRandom.current().nextInt(MIN_TEMP, MAX_TEMP + 1);
        dispatcher.tell(new TemperatureMsg(temp, self()), self());
    }

    static Props props() {
        return Props.create(TemperatureSensorActor.class);
    }
}
