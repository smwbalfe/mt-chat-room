import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class ChatClient{

    private Socket serverSocket;
    private boolean online = true;
    private boolean connectedToServer = true;
    private final List<String> choiceList = Arrays.asList("HELLO", "GOLD", "MOVE N","MOVE S","MOVE W","MOVE E", "LOOK","PICKUP", "QUIT", "LEAVE");

    public ChatClient(String address, Integer portNumber) {

        try {
            this.serverSocket = new Socket(address, portNumber); /* Create the connection that will be made to the server side*/
        } catch (IOException e){
            System.out.println("Server Offline...");
            System.exit(0);
        }

    }

    public void loadClient(){
        try {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            ObjectOutputStream outStream = new ObjectOutputStream(this.serverSocket.getOutputStream());
            ObjectInputStream inStream = new ObjectInputStream(this.serverSocket.getInputStream());

            Thread message = new Thread(() -> {
                while (online) {
                    try {

                        /* write the response to server accordingly via the user input*/
                        String message1 = userInput.readLine();

                        if(!this.connectedToServer){
                            /* send if they havent connected yet*/
                            outStream.writeObject(new Packet(message1, "User", "__CONNECT__"));
                        }
                        else if (message1.toUpperCase().equals("EXIT")){
                            /* send if they wish to leave the server*/
                            outStream.writeObject(new Packet(message1, "User", "__DISCONNECT__"));
                        }
                        else if (message1.toUpperCase().equals("JOIN")){
                            /* send if they wish to join the dod game*/
                            outStream.writeObject(new Packet(message1, "User", "__JOIN__GAME__"));
                        }
                        else if (choiceList.contains(message1)){
                            /* send if they wish to issue a command*/
                            outStream.writeObject(new Packet(message1, "User", "__ISSUE__COMMAND__"));
                        }
                        else{
                            /* send any message to all clients otherwise*/
                            outStream.writeObject(new Packet(message1, "User", "__BROADCAST__"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            Thread readMessage = new Thread(() -> {
                while(online){
                    try {
                        /* read inputs from the server*/
                        Packet serverResponse = (Packet) inStream.readObject(); /* read in the response of the server based on the request above */
                        switch (serverResponse.reason){
                            /* disconnect reason is for when they leave the server and it needs to stop the threads*/
                            case "__DISCONNECT__":
                                System.out.println(serverResponse.message);
                                online = false;
                                System.exit(0);
                                break;
                                /*default response receveid*/
                            case "__RECEIVE__":
                                System.out.println("["+serverResponse.sender+"]: "+serverResponse.message);
                                break;
                                /* confirmation of succesfull connection*/
                            case "__SUCCESS__":
                                System.out.println("You have successfully connected to the server");
                                this.connectedToServer = true;
                                break;
                        }
                    }catch (IOException | ClassNotFoundException e){
                        /* if the server shut downs this errror is caught and readers are closed */
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
            message.start();
            readMessage.start();
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
        ChatClient client = new ChatClient(ip, port);
        client.loadClient();

    }
}
