package it.polimi.nsds.akka.tutorial.ex003.chat;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import it.polimi.nsds.akka.tutorial.ex003.chat.messages.TriggerMessage;
import java.util.concurrent.ExecutorService;

public class ChatClient {

    public static final String serverAddress = "akka://Server/user/chatServer";
    public static final int numMessagesPerClient = 10;

    public static void main(String[] args) {
        ActorSystem client = ActorSystem.create("chat-client");
        ActorRef chatActor = client.actorOf(
            Props.create(ChatClientActor.class, serverAddress)
        );

        ExecutorService exec =
            java.util.concurrent.Executors.newFixedThreadPool(5);

        chatActor.tell(new TriggerMessage(), ActorRef.noSender());
    }
}
