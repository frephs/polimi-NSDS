package it.polimi.nsds.akka.labs.evaluation23;

import akka.actor.AbstractActor;
import akka.actor.Props;
import java.util.Optional;

public class SensorProcessorActor extends AbstractActor {

    private double currentAverage;
    private double count;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(TemperatureMsg.class, this::gotData)
            .build();
    }

    @Override
    public void preStart() {
        currentAverage = 0;
        count = 0;
    }

    private void gotData(TemperatureMsg msg) throws Exception {
        System.out.println(
            "SENSOR PROCESSOR " + self() + ": Got data from " + msg.getSender()
        );

        if (msg.getTemperature() < 0) {
            System.out.println(
                "Processor got a negative temperature, resuming shortly"
            );
            throw new Exception("Processor got a temperature less than zero");
        } else {
            currentAverage += msg.getTemperature() / (++count);
        }

        System.out.println(
            "SENSOR PROCESSOR " + self() + ": Current avg is " + currentAverage
        );
    }

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) {
        System.out.println("Sensor processor restarted  ");
    }

    static Props props() {
        return Props.create(SensorProcessorActor.class);
    }

    public SensorProcessorActor() {}
}
