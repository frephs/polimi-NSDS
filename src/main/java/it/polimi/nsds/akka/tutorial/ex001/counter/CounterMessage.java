package it.polimi.nsds.akka.tutorial.ex001.counter;

public class CounterMessage {

    public int value;
    TypeEnum type;

    public enum TypeEnum {
        ADD,
        SUBTRACT,
    }
}
