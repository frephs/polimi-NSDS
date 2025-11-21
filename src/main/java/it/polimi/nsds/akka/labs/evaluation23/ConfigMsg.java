package it.polimi.nsds.akka.labs.evaluation23;

import akka.actor.ActorRef;

public class ConfigMsg extends GenerateMsg {

    private ActorRef dispatcher;

    public ConfigMsg(ActorRef dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ActorRef getDispatcher() {
        return dispatcher;
    }
}
