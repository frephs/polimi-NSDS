package it.polimi.nsds.akka.labs.evaluation23;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import scala.concurrent.duration.Duration;

public class DispatcherActor extends AbstractActorWithStash {

    private static final int NO_PROCESSORS = 2;

    private ActorSystem processing;

    //LoadBalancer
    private final Map<ActorRef, Integer> processorLoad; //keyset used by iterator
    private final Map<ActorRef, ActorRef> dispatchMap;

    // RoundRobin
    private Iterator<ActorRef> processorIterator;

    public DispatcherActor() {
        processorLoad = new HashMap<>();
        dispatchMap = new HashMap<>();

        for (int i = 0; i < NO_PROCESSORS; i++) {
            processing = ActorSystem.create("Processing");

            System.out.println("DISPATCHER: starting a new processor " + i);

            processorLoad.put(
                processing.actorOf(
                    Props.create(SensorProcessorActor.class),
                    "processor" + i
                ),
                0
            );
        }

        processorIterator = processorLoad.keySet().iterator();
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return loadBalancer();
    }

    private void onDispatchLogicMsg(DispatchLogicMsg msg) {
        if (msg.getLogic() == DispatchLogicMsg.ROUND_ROBIN) {
            this.getContext().become(roundRobin());
        } else if (msg.getLogic() == DispatchLogicMsg.LOAD_BALANCER) {
            this.getContext().become(loadBalancer());
        } else sender().tell("Unknown dispatch logic" /*todo*/, getSelf());
    }

    private Receive loadBalancer() {
        System.out.println(
            "\nDISPATCHER: Dispatcher Logic set to LOAD_BALANCER"
        );
        dispatchMap.clear();
        processorLoad
            .keySet()
            .forEach(processor -> {
                processorLoad.put(processor, 0);
            });
        return ReceiveBuilder.create()
            .match(DispatchLogicMsg.class, this::onDispatchLogicMsg)
            .match(TemperatureMsg.class, this::dispatchDataLoadBalancer)
            .build();
    }

    private Receive roundRobin() {
        System.out.println("\nDISPATCHER: Dispatcher Logic set to ROUND_ROBIN");
        return ReceiveBuilder.create()
            .match(DispatchLogicMsg.class, this::onDispatchLogicMsg)
            .match(TemperatureMsg.class, this::dispatchDataRoundRobin)
            .build();
    }

    private void dispatchDataLoadBalancer(TemperatureMsg msg) {
        if (dispatchMap.containsKey(sender())) {
            System.out.println(
                "DISPATCHER: dispatching Temperature message from sender: " +
                    sender() +
                    "to " +
                    dispatchMap.get(sender())
            );
            dispatchMap.get(sender()).tell(msg, sender());
        } else {
            System.out.println(
                "DISPATCHER: sensor unregistered, selecting processor"
            );
            if (!processorIterator.hasNext()) {
                processorIterator = processorLoad.keySet().iterator();
            }
            dispatchMap.put(sender(), processorIterator.next());
            dispatchDataLoadBalancer(msg);
        }
    }

    private void dispatchDataRoundRobin(TemperatureMsg msg) {
        if (processorIterator.hasNext()) {
            System.out.println(
                "DISPATCHER: dispatching message to next processor"
            );
            processorIterator.next().tell(msg, getSelf());
        } else {
            System.out.println("DISPATCHER: new round");
            processorIterator = processorLoad.keySet().iterator();
            dispatchDataRoundRobin(msg);
        }
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(
            1,
            Duration.create(5, TimeUnit.SECONDS),
            DeciderBuilder.match(Exception.class, e ->
                SupervisorStrategy.resume()
            ).build()
        );
    }

    static Props props() {
        return Props.create(DispatcherActor.class);
    }
}
