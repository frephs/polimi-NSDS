package it.polimi.nsds.akka.labs.evaluation23;

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

    protected void configure(ConfigMsg msg) {
        System.out.println(
            "TEMPERATURE SENSOR: " +
                self() +
                ": Received ConfigMsg: dispatcher set to" +
                msg.getDispatcher()
        );
        dispatcher = msg.getDispatcher();
    }

    private void onGenerate(GenerateMsg msg) {
        if (dispatcher != null) {
            int temp = ThreadLocalRandom.current().nextInt(
                MIN_TEMP,
                MAX_TEMP + 1
            );
            System.out.println(
                "TEMPERATURE SENSOR: " +
                    self() +
                    " Sensing temperature! Sending measurement of " +
                    temp +
                    " to: " +
                    dispatcher
            );
            dispatcher.tell(new TemperatureMsg(temp, self()), self());
        }
    }

    static Props props() {
        return Props.create(TemperatureSensorActor.class);
    }
}
