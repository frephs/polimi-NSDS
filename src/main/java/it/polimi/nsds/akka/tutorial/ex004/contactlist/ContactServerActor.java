package it.polimi.nsds.akka.tutorial.ex004.contactlist;

import akka.actor.AbstractActor;
import akka.actor.Props;
import it.polimi.nsds.akka.tutorial.ex004.contactlist.messages.ContactMessage;
import it.polimi.nsds.akka.tutorial.ex004.contactlist.messages.GetMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ContactServerActor extends AbstractActor {

    Map<String, String> contacts;

    ContactServerActor() {
        this.contacts = new HashMap<>();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ContactMessage.class, this::onPutMessage)
            .match(GetMessage.class, this::onGetMessage)
            .build();
    }

    void onPutMessage(ContactMessage msg) {
        if (contacts.putIfAbsent(msg.name, msg.email) != null) {
            System.out.println(
                "Warning: contact " +
                    msg.name +
                    " was already present and has not been updated."
            );
        } else {
            System.out.println(
                "Contact added: " + msg.name + " : " + msg.email
            );
        }
    }

    void onGetMessage(GetMessage msg) {
        String email = contacts.get(msg.name);
        if (email != null) {
            sender().tell(email, self());
            System.out.println("Contact found: " + email);
        } else {
            sender().tell("Contact not found", self());
        }
    }

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) {
        System.out.print("Preparing to restart...");
    }

    @Override
    public void postRestart(Throwable reason) {
        System.out.println("...now restarted!");
    }

    public static Props props(Map<String, String> contacts) {
        return Props.create(ContactServerActor.class, ContactServerActor::new);
    }
}
