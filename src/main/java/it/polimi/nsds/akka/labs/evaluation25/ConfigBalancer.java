package it.polimi.nsds.akka.labs.evaluation25;

import akka.actor.ActorRef;

public class ConfigBalancer {

    private final ActorRef worker0;
    private final ActorRef worker1;

    public ActorRef getWorker0() {
        return worker0;
    }

    public ActorRef getWorker1() {
        return worker1;
    }

    public ConfigBalancer(ActorRef worker0, ActorRef worker1) {
        this.worker0 = worker0;
        this.worker1 = worker1;
    }
}
