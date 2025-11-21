package it.polimi.nsds.akka.tutorial.ex000.counter;

public class DataMessage {

    private int code;

    public int getCode() {
        return code;
    }

    public DataMessage(int code) {
        this.code = code;
    }
}
