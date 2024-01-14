import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class ChatServer{

    private ServerSocket ss;
    BufferedReader userInput;
    static Vector<Socket> socketsConnected = new Vector<>();

   public ChatServer(int port){
       try {
           ss = new ServerSocket(port);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   public void start() {

       try {
           /* Thread runs to check for console input in case the user wishes to EXIT the server */
           Thread readTerminal = new Thread(() -> {
               /* server shut down on EXIT */
               boolean serverOnline = true;
                while(serverOnline){
                    userInput = new BufferedReader(new InputStreamReader(System.in));
                    try {
                        if (userInput.readLine().toUpperCase().equals("EXIT")){
                            serverOnline = false;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
               try {

                   System.out.println("Server shutting down...");
                   /* close the clients socket that are connected to the server*/
                   for (Socket socket: socketsConnected){
                       socket.close();
                   }
                   /* close the readers and server socket then shut down*/
                   ss.close();
                   userInput.close();
                   System.exit(0);
               } catch (IOException e) {
                   e.printStackTrace();
               }
           });
           readTerminal.start();
           while (true) {

               /* accept new incoming connections*/
               Socket s = ss.accept();

               /* create a new thread to handle the clients seperately passing in their unique socket*/
               new Thread(new ClientHandler(s)).start();

           }
       } catch (Exception e) {
           System.out.println("FORCEFUL CLIENT EXIT CAUGHT");
       }
   }

   public static void main(String[] args) {
       int port = 14002;
       for ( int i = 0; i < args.length; i++ ) {
           if ( args[i].equals("-csp") ) {
               i++;
               port = Integer.parseInt(args[i]);
           }
       }
       ChatServer server = new ChatServer(port);
       server.start();

   }


}


