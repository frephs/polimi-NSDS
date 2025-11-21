package it.polimi.nsds.akka.labs.evaluation25;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.concurrent.TimeUnit;

public class ReplicatedAddressBook {

    public static void main(String[] args) {
        // Timeouts are used to ensure message queues are flushed
        final int timeStep = 2;

        final ActorSystem sys = ActorSystem.create("System");
        final ActorRef worker0 = sys.actorOf(
            AddressBookWorkerActor.props(),
            "worker0"
        );
        final ActorRef worker1 = sys.actorOf(
            AddressBookWorkerActor.props(),
            "worker1"
        );
        final ActorRef balancer = sys.actorOf(
            AddressBookBalancerActor.props(),
            "balancer"
        );
        final ActorRef client = sys.actorOf(
            AddressBookClientActor.props(),
            "client"
        );

        balancer.tell(
            new ConfigBalancer(worker0, worker1),
            ActorRef.noSender()
        );
        client.tell(new ConfigClient(balancer), ActorRef.noSender());

        // Waiting until system is ready
        try {
            TimeUnit.SECONDS.sleep(timeStep);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Populate some entries
        client.tell(
            new PutMsg("Alessandro", "alessandro.margara@polimi.it"),
            ActorRef.noSender()
        );
        client.tell(
            new PutMsg("Salvatore", "salvo.zanella@polimi.it"),
            ActorRef.noSender()
        );

        // Waiting until system is ready
        try {
            TimeUnit.SECONDS.sleep(timeStep);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Return alessandro.margara@polimi.it from worker0 at the client
        client.tell(new GetMsg("Alessandro"), ActorRef.noSender());

        // Waiting until system is ready
        try {
            TimeUnit.SECONDS.sleep(timeStep);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Make worker0 rest
        worker0.tell(new RestMsg(), ActorRef.noSender());

        // Waiting until system is ready
        try {
            TimeUnit.SECONDS.sleep(timeStep);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Now return alessandro.margara@polimi.it from worker1 at the client, worker1
        // has the secondary copy
        client.tell(new GetMsg("Alessandro"), ActorRef.noSender());

        // Waiting until system is ready
        try {
            TimeUnit.SECONDS.sleep(timeStep);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Make worker1 rest also
        worker1.tell(new RestMsg(), ActorRef.noSender());

        // Waiting until system is ready
        try {
            TimeUnit.SECONDS.sleep(timeStep);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // No copy is available; the client receives a TimeoutMsg as a Reply!
        client.tell(new GetMsg("Alessandro"), ActorRef.noSender());

        // Waiting until system is ready
        try {
            TimeUnit.SECONDS.sleep(timeStep);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        worker1.tell(new ResumeMsg(), ActorRef.noSender());
        try {
            TimeUnit.SECONDS.sleep(timeStep);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        client.tell(new GetMsg("Salvatore"), ActorRef.noSender());

        sys.terminate();
    }
}
