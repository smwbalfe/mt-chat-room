import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

public class DoDClient {



    Vector<String> playersActive = new Vector<>();
    private Socket serverSocket;
    private boolean connectedToServer;
    public static HashMap<String, ObjectInputStream> oos = new HashMap<>();
    GameLogic game;

    public DoDClient(String address, Integer portNumber){

        try {
            this.serverSocket = new Socket(address, portNumber); /* Create the connnection that will be made to the server side*/
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public void loadClient(){

        try {
            ObjectOutputStream outStream = new ObjectOutputStream(this.serverSocket.getOutputStream());
            ObjectInputStream inStream = new ObjectInputStream(this.serverSocket.getInputStream());


            Thread DoDOutput = new Thread(() -> {
                while (true) {
                    try {
                        /* add the dod client automatically.*/
                        if (!connectedToServer){
                           connectedToServer = true;

                            outStream.writeObject(new Packet("__DOD__CLIENT__", "__DOD__CLIENT__","__CONNECT__"));

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            Thread DodInput = new Thread(() -> {
                while(true) {
                    try {
                        /* if the input stream has not been added, if added it prevents input being read from the user and directs it to the running DOD game instead*/
                        if (!oos.containsValue(inStream)) {
                            Packet serverResponse = (Packet) inStream.readObject();
                            if ("__JOIN__GAME__".equals(serverResponse.reason)) {
                                /* place the in stream in the list*/
                                oos.put(serverResponse.sender, inStream);
                                /* add the player who started the game and then start the actual game*/
                                playersActive.add(serverResponse.sender);
                                this.game = new GameLogic(inStream, outStream);
                                this.game.startGame();
                            }
                         }
                    } catch (IOException | ClassNotFoundException e) {

                        if(e.getMessage().equals("Connection reset")){
                            try {
                                inStream.close();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                            try {
                                outStream.close();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                            System.out.println("Server has shut down, Connection reset");
                            System.exit(0);
                        }
                        System.out.println(e.getMessage());
                    }
            }
            });
            DoDOutput.start();
            DodInput.start();
        }catch(IOException e){
            e.printStackTrace();
        }

    }
    public static void main(String[] args){

        String ip = "localhost";
        int port = 14002;
        for ( int i = 0; i < args.length; i++ ) {
            if ( args[i].equals("-ccp") ) {
                i++;
                port = Integer.parseInt(args[i]);
            } else if ( args[i].equals("-cca") ) {
                i++;
                ip = args[i];
            }
        }
        DoDClient client = new DoDClient(ip, port);
        client.loadClient();

    }

}
