package it.polimi.nsds.akka.labs.evaluation25;

public class PutAsPrimaryMsg {

    private String name;
    private String email;

    public PutAsPrimaryMsg(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
