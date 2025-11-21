package it.polimi.nsds.akka.labs.evaluation25;

import akka.actor.ActorRef;

public class ConfigClient {

    private final ActorRef balancer;

    public ActorRef getBalancer() {
        return balancer;
    }

    public ConfigClient(ActorRef balancer) {
        this.balancer = balancer;
    }
}
