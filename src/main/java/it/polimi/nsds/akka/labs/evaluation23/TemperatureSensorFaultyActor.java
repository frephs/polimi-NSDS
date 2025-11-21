package it.polimi.nsds.akka.labs.evaluation23;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class TemperatureSensorFaultyActor extends TemperatureSensorActor {

    private ActorRef dispatcher;
    private static final int FAULT_TEMP = -50;

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
            .match(GenerateMsg.class, this::onGenerate)
            .match(ConfigMsg.class, this::configure)
            .build();
    }

    private void onGenerate(GenerateMsg msg) {
        System.out.println(
            "TEMPERATURE SENSOR: " +
                self() +
                " Sensing temperature! Sending measurement of " +
                FAULT_TEMP +
                " to: " +
                dispatcher
        );
        if (dispatcher != null) {
            dispatcher.tell(new TemperatureMsg(FAULT_TEMP, self()), self());
        }
    }

    static Props props() {
        return Props.create(TemperatureSensorFaultyActor.class);
    }
}
