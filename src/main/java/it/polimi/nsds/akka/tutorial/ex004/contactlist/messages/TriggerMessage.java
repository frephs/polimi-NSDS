package it.polimi.nsds.akka.tutorial.ex004.contactlist.messages;

public class TriggerMessage {

    //this could contain the server info
    // using props is ok, completely equivalent
    // client can reroute messages from main.
    // bad to send messages in the prestart

    public TriggerType triggerType;

    public TriggerMessage(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public enum TriggerType {
        PUT,
        GET,
    }
}
