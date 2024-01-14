import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;


public class HumanPlayer extends Player {


    private final ObjectOutputStream sendBack;
    private final ObjectInputStream readIn;
    private static String currentPlayer;


    public HumanPlayer(ObjectOutputStream serverOut, ObjectInputStream serverIn) {
        this.readIn = serverIn;
        this.sendBack = serverOut;

    }

    ;

    // store valid commands as a list for quick validation on which moves are allowed.
    private final List<String> choiceList = Arrays.asList("HELLO", "GOLD", "MOVE N", "MOVE S", "MOVE W", "MOVE E", "LOOK", "PICKUP", "QUIT","LEAVE");

    public Packet getNextChoice() {

        // fetch action from console

        try {
            while (true) {
                Packet action = (Packet) this.readIn.readObject();
                boolean ignore = false;
                if (action.reason.equals("__RECEIVE__")){
                    ignore = true;
                }
                System.out.println(action);
                currentPlayer = action.sender;

                if (choiceList.contains(action.message) || action.message.equals("JOIN")) {
                    return action;

                }else if (ignore){
                    System.out.println("ignore input");
                } else {
                    this.sendBack.writeObject(new Packet("Invalid Selection, Please enter again", currentPlayer, "return_dod"));
                }
            }
        } catch (IOException | ClassNotFoundException ioException) {
            ioException.printStackTrace();
        }
        return new Packet("", currentPlayer, "__VOID__");
    }

}