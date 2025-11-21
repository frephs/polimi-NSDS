package it.polimi.nsds.akka.tutorial.ex004.contactlist;

import akka.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;

public class ContactServer {

    // Distributed fashion.
    public static void main(String[] args) {
        Config conf = ConfigFactory.parseFile(new File("server.conf"));
        final ActorSystem server = ActorSystem.create("Server", conf);
        server.actorOf(
            Props.create(ContactServerActor.class),
            "contact-server"
        );
    }
}
