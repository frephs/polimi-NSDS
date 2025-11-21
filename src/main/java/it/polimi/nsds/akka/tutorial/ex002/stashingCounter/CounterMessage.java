package it.polimi.nsds.akka.tutorial.ex002.stashingCounter;

public class CounterMessage {

    int value;
    TypeEnum type;

    enum TypeEnum {
        ADD,
        SUBTRACT,
    }
}
