package it.polimi.nsds.akka.labs.evaluation25;

import static java.util.concurrent.TimeUnit.SECONDS;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import java.util.concurrent.TimeoutException;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class AddressBookClientActor extends AbstractActor {

    private ActorRef balancer;
    private scala.concurrent.duration.Duration timeout =
        scala.concurrent.duration.Duration.create(3, SECONDS);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(PutMsg.class, this::putEntry)
            .match(GetMsg.class, this::query)
            .match(ConfigClient.class, this::config)
            .build();
    }

    void config(ConfigClient msg) {
        balancer = msg.getBalancer();
    }

    void putEntry(PutMsg requestMsg) {
        // Only queries are required to use the Ask Pattern
        // Put messages are always accepted by the balancer and the workers
        balancer.tell(requestMsg, getSelf());
    }

    void query(GetMsg msg) throws InterruptedException, TimeoutException {
        System.out.println("CLIENT: Issuing query for " + msg.getName());
        Future<Object> resultFuture = Patterns.ask(balancer, msg, 5000);
        Object response = resultFuture.result(
            Duration.create(5, SECONDS),
            null
        );

        if (response.getClass() == TimeoutMsg.class) {
            System.out.println(
                "CLIENT: Received timeout, both copies are resting!"
            );
        } else if (response.getClass() == ResponseMsg.class) {
            ResponseMsg responseMsg = (ResponseMsg) response;

            if (responseMsg.getEmail() != null) {
                System.out.println(
                    "CLIENT: Received reply! Email:" + responseMsg.getEmail()
                );
            } else {
                System.out.println("CLIENT: Received reply, no email found!");
            }
        } else {
            throw new RuntimeException("CLIENT: Unknown response type.");
        }
    }

    static Props props() {
        return Props.create(AddressBookClientActor.class);
    }
}
