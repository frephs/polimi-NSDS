package it.polimi.nsds.akka.labs.evaluation25;

import akka.actor.AbstractActor;
import akka.actor.Props;
import java.util.HashMap;

public class AddressBookWorkerActor extends AbstractActor {

    private HashMap<String, String> primaryAddresses;
    private HashMap<String, String> replicaAddresses;

    public AddressBookWorkerActor() {
        this.primaryAddresses = new HashMap<String, String>();
        this.replicaAddresses = new HashMap<String, String>();
    }

    @Override
    public Receive createReceive() {
        return awake();
    }

    private Receive resting() {
        return receiveBuilder()
            .match(ResumeMsg.class, this::resume)
            .match(PutAsPrimaryMsg.class, this::putPrimary)
            .match(PutAsReplicaMsg.class, this::putReplica)
            // No-op for these two classes of message
            .match(RestMsg.class, obj -> {})
            .match(GetMsg.class, obj -> {})
            .build();
    }

    private Receive awake() {
        return receiveBuilder()
            .match(RestMsg.class, this::rest)
            .match(PutAsPrimaryMsg.class, this::putPrimary)
            .match(PutAsReplicaMsg.class, this::putReplica)
            .match(GetMsg.class, this::generateReply)
            // No-op for resume message if already awake
            .match(ResumeMsg.class, obj -> {})
            .build();
    }

    private void resume(ResumeMsg msg) {
        getContext().become(awake());
    }

    private void rest(RestMsg msg) {
        System.out.println("RESTING " + getSelf());
        getContext().become(resting());
    }

    private void putPrimary(PutAsPrimaryMsg msg) {
        primaryAddresses.put(msg.getName(), msg.getEmail());
    }

    private void putReplica(PutAsReplicaMsg msg) {
        replicaAddresses.put(msg.getName(), msg.getEmail());
    }

    void generateReply(GetMsg msg) {
        System.out.println(
            this.toString() + ": Received query for name " + msg.getName()
        );

        String result = primaryAddresses.get(msg.getName());
        if (result == null) {
            result = replicaAddresses.get(msg.getName());
        }

        getSender().tell(new ResponseMsg(msg.getName(), result), getSelf());
    }

    static Props props() {
        return Props.create(AddressBookWorkerActor.class);
    }
}
