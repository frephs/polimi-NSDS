package it.polimi.nsds.akka.tutorial.ex004.contactlist.messages;

public class ContactMessage {

    public String name;
    public String email;

    public ContactMessage(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
