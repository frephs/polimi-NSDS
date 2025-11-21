package it.polimi.nsds.akka.labs.evaluation25;

public class ResponseMsg {

    private String name;
    private String email;

    public ResponseMsg(String name, String email) {
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
