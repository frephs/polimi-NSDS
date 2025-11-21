package it.polimi.nsds.akka.tutorial.ex004.contactlist.messages;

public class GetMessage {

    public String name;

    public GetMessage(String name) {
        this.name = name;
    }
}
