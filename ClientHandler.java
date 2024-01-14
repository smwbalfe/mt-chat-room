import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    static HashMap<Socket, String> clientIDs = new HashMap<>();
    static HashMap<Socket, ObjectOutputStream> oos = new HashMap<>();
    static boolean gameRunning = false;
    private String username;
    private boolean playingDod = false;

    public ClientHandler(Socket clientSocket) {
        this.socket = clientSocket;

    }
    public void run() {

            try {
                ObjectOutputStream outStream= new ObjectOutputStream(this.socket.getOutputStream());
                oos.put(this.socket, outStream);
                ObjectInputStream inStream = new ObjectInputStream(this.socket.getInputStream());
                /* Loop for user input packets*/
                while (true) {
                    /* if the client has not been assigned a username prompt them so*/
                    while (!clientIDs.containsKey(this.socket)) {
                        /* send this message asking to enter the username*/
                        message("--- Enter Username ---", "server", "__RECEIVE__", outStream);
                        /* they enter it here*/
                        Packet username = (Packet) inStream.readObject();
                        /* Check for duplicate usernames*/
                        if (clientIDs.containsValue(username.message)) {
                            message("That username is in use", "server", "__RECEIVE__", outStream);
                        } else {
                            /* set the current username to username requested*/
                            this.username = username.message;
                            message("", "server", "__SUCCESS__", outStream);
                            /* associate their socket with their username*/
                            clientIDs.put(this.socket, username.message);
                            broadcast(new Packet("joined the server!", "server", "__BROADCAST__"), "JOIN");
                        }
                    }
                Packet clientResponse = (Packet) inStream.readObject();
                //System.out.println("Reason: "+clientResponse.reason+ " sender: "+clientResponse.sender+" message " +clientResponse.message);
                switch (clientResponse.reason) {
                    /* notify of a user leaving the server*/
                    case "__DISCONNECT__":
                        broadcast(new Packet("left the server", "server", "__BROADCAST__"), "EXIT");
                        clientIDs.remove(this.socket);
                        message("Disconnected From the Server", "server", "__DISCONNECT__", outStream);
                        this.socket.close();
                        break;
                    /* send a message to every user*/
                    case "__BROADCAST__":
                        broadcast(clientResponse, "__RECEIVE__");
                        break;
                    /* join the DOD game*/
                    case "__JOIN__GAME__":
                        clientResponse.sender = this.username;
                        ObjectOutputStream command = oos.get(fetchSocket("__DOD__CLIENT__"));
                        /* if a game has not started up then initialse it*/
                        if (!gameRunning){
                            this.playingDod = true;
                            command.writeObject(new Packet("", this.username, "__JOIN__GAME__"));
                            gameRunning = true;
                        }
                        else{
                            this.playingDod = true;
                            message("You have joined the DOD game", "dod", "__RECEIVE__", outStream);
                            command.writeObject(new Packet(clientResponse.message, this.username, "__JOIN__GAME__"));
                        }
                        break;
                        /* issue a dodd command to a  user playing the game*/
                    case "__ISSUE__COMMAND__" :
                        command = oos.get(fetchSocket("__DOD__CLIENT__"));
                        if (this.playingDod){
                            /* if leave then notify then they have left the game and set their dod playing game status to false*/
                            if(clientResponse.message.equals("LEAVE")){
                                message("You have left the DOD game", "dod", "__RECEIVE__", outStream);
                                this.playingDod = false;
                            }
                            /* using a lock to make it thread safe send the command to dod*/
                            synchronized (this){
                                command.writeObject(new Packet(clientResponse.message, this.username, "__ISSUE__COMMAND__"));
                            }
                        }
                        /* handle responses from the dod to send to the specific client requested.*/
                    case "_DOD_RESPONSE_":
                        ObjectOutputStream dodResponse = oos.get(fetchSocket(clientResponse.sender));
                        if (dodResponse != null){
                            dodResponse.writeObject(new Packet(clientResponse.message, "dod", "__RECEIVE__"));
                        }
                        break;
                        /* special request for a bot broadcast*/
                    case "__BOT__REPLY__":
                        broadcast(new Packet(clientResponse.message, "ChatBot", "__RECEIVE__"),"bot");
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
                try{
                    /* close sockets on forceful exit*/
                    this.socket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
        }
    }

    private void broadcast(Packet message, String type) {
        /* go throuhg each socket in the hashmap*/
        for (Socket client : clientIDs.keySet()) {
            try {
                /* dont send to  the dod client or the person who sent it*/
                if (!client.equals(this.socket)){
                    ObjectOutputStream broadcast3 = oos.get(client);
                    message.reason = "__RECEIVE__";
                    message.sender = clientIDs.get(this.socket);
                    /* send the message to the client and then move on to sending to the next one, using a lock as potential for multiple writes to this same stream*/
                    synchronized (this){
                        broadcast3.writeObject(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* input = username, output = the socket associated with that username*/
    private synchronized Socket fetchSocket(String username) {
        Socket socket = null;
        for (Socket client : clientIDs.keySet()) {
            if (clientIDs.get(client).equals(username)) {
                return client;
            }
        }
        return socket;
    }

    /* send a message back to the client associated with this*/
    private synchronized void message(String message, String sender, String reason, ObjectOutputStream outStream) throws IOException {
        outStream.writeObject(new Packet(message, sender, reason));
    }
}

