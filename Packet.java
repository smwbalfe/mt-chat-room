import java.io.Serializable;

public class Packet implements Serializable {
    public String message;
    public String sender;
    public String reason;

    public Packet(String message, String sender, String reason){

        this.message = message;
        this.sender = sender;
        this.reason = reason;

    }



}
