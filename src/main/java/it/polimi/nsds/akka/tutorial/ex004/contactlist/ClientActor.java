package it.polimi.nsds.akka.tutorial.ex004.contactlist;

import static akka.pattern.Patterns.ask;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import it.polimi.nsds.akka.tutorial.ex004.contactlist.messages.ContactMessage;
import it.polimi.nsds.akka.tutorial.ex004.contactlist.messages.GetMessage;
import it.polimi.nsds.akka.tutorial.ex004.contactlist.messages.TriggerMessage;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class ClientActor extends AbstractActor {

    String serverAddress;
    ActorSelection server;

    public ClientActor(String serverAddress) {
        this.serverAddress = serverAddress;
        server = getContext().actorSelection(serverAddress);
    }

    //do not assume anything about the ordering of messages
    @Override
    public void preStart() {
        System.out.println(
            "ClientActor starting, connecting to server at " + serverAddress
        );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(TriggerMessage.class, this::onTriggerMessage)
            .build();
    }

    public void onTriggerMessage(TriggerMessage message) {
        Random rand = new Random();
        String name = "Name" + rand.nextInt(10);
        String email = "name" + rand.nextInt(10);

        try {
            if (
                message.triggerType == TriggerMessage.TriggerType.GET
            ) getContact(name);
            else if (
                message.triggerType == TriggerMessage.TriggerType.PUT
            ) addContact(name, email);
            else System.out.println("Unknown trigger type");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object getContact(String name) throws Exception {
        Future<Object> result = ask(server, new GetMessage(name), 5000);
        return Await.result(result, Duration.create(5, TimeUnit.SECONDS));
    }

    public Object addContact(String name, String email) throws Exception {
        Future<Object> result = ask(
            server,
            new ContactMessage(name, email),
            5000
        );
        return Await.result(result, Duration.create(5, TimeUnit.SECONDS));
    }

    public Props props(String serverAddress) {
        return Props.create(ClientActor.class, () ->
            new ClientActor(serverAddress)
        );
    }

    public static void main(String[] args) {}
}
