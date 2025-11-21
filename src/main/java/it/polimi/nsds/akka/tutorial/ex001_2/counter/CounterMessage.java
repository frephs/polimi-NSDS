package it.polimi.nsds.akka.tutorial.ex001_2.counter;

public class CounterMessage {

    int value;
    TypeEnum type;

    enum TypeEnum {
        ADD,
        SUBTRACT,
    }
}
