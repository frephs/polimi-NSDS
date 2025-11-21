package it.polimi.nsds.akka.tutorial.ex003.chat;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;

public class ChatServer {

    public static void main(String[] args) {
        Config conf = ConfigFactory.parseFile(new File("server.conf"));
        final ActorSystem server = ActorSystem.create("Server", conf);
        ActorRef chatServer = server.actorOf(
            Props.create(ChatServerActor.class),
            "chatServer"
        );
        System.out.println(
            "Server started at: " + server.provider().getDefaultAddress()
        );
    }
}
