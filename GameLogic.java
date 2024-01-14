import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;

public class GameLogic {


    private final MapMain map = new MapMain();
    private final Bot botPlayer;
    private int goldCount;
    private int[] enemyCoords;
    public int[] playerCoords;
    private int[] testCoords;
    private final HumanPlayer humanPlayer;
    static HashMap<String, int[]> mapStates = new HashMap<>();
    private static String currentUser;
    ObjectInputStream inStream;
    ObjectOutputStream outStream;


    public GameLogic(ObjectInputStream inStream, ObjectOutputStream outputStream) {

        this.outStream = outputStream;
        this.inStream = inStream;
        botPlayer = new Bot();
        humanPlayer= new HumanPlayer(outputStream, inStream);

    }

    public void addPlayer(String username) {
            int[] newCoordinates = generateRandomCoords(username);
            mapStates.put(username, newCoordinates);
    }
    protected void returnMessage(String message) throws IOException {

        System.out.println("message normal " + message);
        this.outStream.writeObject(new Packet(message, GameLogic.currentUser, "_DOD_RESPONSE_"));

    }
    protected void returnMessageSpecific(String client, String message) throws IOException {

        System.out.println("message other " + message);
        this.outStream.writeObject(new Packet(message, client, "_DOD_RESPONSE_"));

    }
    protected void hello() throws IOException {
        returnMessage( "Gold to Win: " + (map.getGoldRequired()));
    }

    protected void gold() throws IOException {
        returnMessage( "Gold owned: " + goldCount);
    }

    protected void pickup() throws IOException {
        if (map.fetchItem(playerCoords) != 'G') {
             returnMessage( "Failure. Gold owned: " + goldCount);
        }
        else {
            goldCount++;
            map.destroyItem(playerCoords);
            returnMessage( "Success. Gold owned: " + goldCount);
        }

    }
    protected void quitGame() throws IOException {
        if (map.fetchItem(playerCoords) == 'E' && goldCount >= map.getGoldRequired()) {
            SuccessGame();
        } else {
            FailGame();
        }
    }
    public void startGame() throws IOException {

        goldCount = 0;


        enemyCoords = generateRandomCoords("bot");
        String fetchMove;
        while (true) {

            Packet response = humanPlayer.getNextChoice();
            if (!mapStates.containsKey(response.sender)) {
                addPlayer(response.sender);
            }
            boolean ignore = response.message.equals("JOIN");
            fetchMove = response.message;
            GameLogic.currentUser = response.sender;
            playerCoords = mapStates.get(response.sender);

            if (!ignore){
                switch (fetchMove) {
                    // Output the result of pickup which will be success or failure.
                    case "PICKUP":
                        pickup();
                        break;
                    // hello just prints the gold required of the map they are on
                    case "HELLO":
                        hello();
                        break;
                    //Call quit and then check for whether they won within that
                    case "QUIT":
                        quitGame();
                        break;
                    // output the array of the human player with the input as the 5x5 map where the player coordinates
                    // are at the centre which is the view of the players surroundings.
                    case "LOOK":
                        outputArray(returnFivebyFiveMap(playerCoords));
                        break;

                    // Print the ammount of gold the user has/
                    case "GOLD":
                        gold();
                        break;
                    case "IGNORE":
                        ignore = true;
                        break;
                    case "LEAVE":
                        mapStates.remove(currentUser);
                        for (String client : mapStates.keySet()) {
                            if (!client.equals(GameLogic.currentUser)) {
                        
                                returnMessageSpecific(client, GameLogic.currentUser + " has left the game");
                            }
                        }
                }

            
                boolean playing = false;
                for (String client : mapStates.keySet()) {
                    if (client.equals(GameLogic.currentUser)) {
                        playing = true;
                    }
                }
                if(playing) {
            
                    if (fetchMove.contains("MOVE ")) {
                        runPlayerMove(fetchMove, "human");
                    }
                    fetchMove = botPlayer.getNextAction();
                    if (fetchMove.contains("MOVE ")) {
                        runPlayerMove(fetchMove, "bot");
                    } else if (fetchMove.equals("LOOK")) {
                        returnMessage("Bot is scanning the enviroment");
                        botPlayer.environmentScan(returnFivebyFiveMap(enemyCoords));
                    }
                }
            }
            else{
                for (String client : mapStates.keySet()) {
                    if (!client.equals(GameLogic.currentUser)) {
                    
                        returnMessageSpecific(client, GameLogic.currentUser + " has joined the game");
                    }
                }
            }
        }

    }

    public void runPlayerMove(String input, String player) throws IOException {

        // Create a string array to hold the value of 'MOVE' and DIRECTION out of {N,S,W,E}
        String[] pointTo = input.split("MOVE ");

        // hold the direction in a char by pointing to the first item and setting it to be a char
        char directionInputValue = pointTo[1].charAt(0);

        // execute the move of the Player with that direction where Player can be the bot or the human.
        move(directionInputValue, player);


    }

    public int[] generateRandomCoords(String player) {

        // Loop so it keeps generating random coordinates until a valid statement
        // which reaches return is reached
        while (true) {

            // Obtain map size
            int[] mapSize = map.returnSizeOfMap();

            // Pick a random x and y coordinate.
            double x = Math.random() * mapSize[0];
            double y = Math.random() * mapSize[1];

            // define a temp variable to hold this value of x and y.
            testCoords = new int[]{(int) x, (int) y};

            // cannot be a wall for both the bot and human
            if (map.fetchItem(testCoords) != '#') {
                // if the player is a bot
                if (player.equals("bot")){

                    // it cannot be on the same location as the player
                    if (testCoords != this.playerCoords) {
                        return testCoords;
                    }
                }
                // otherwise the player cannot be spawned on a gold item
                else if (map.fetchItem(testCoords) != 'G') {
                    return testCoords;
                }
            }


        }

    }
    // The actual shifting of the players are performed in this function which takes the direction of movement
    // and which player is to be moved.

    public void move(char direction, String player) throws IOException {

        // Used as a temp store of current players coordinates
        int[] prevCoords = new int[]{-1, -1};

        // determine which player instance to define the previous coordinates as
        if (player.equals("bot")) {
            prevCoords = enemyCoords;
        } else if (player.equals("human")) {

            prevCoords = playerCoords;

        }


        // set the coordinates of testCoords to hold the location that is to be intended to be the
        // new coordinates.
        switch (direction) {
            case 'N':
                testCoords = new int[]{prevCoords[0] - 1, prevCoords[1]};
                break;
            case 'S':
                testCoords = new int[]{prevCoords[0] + 1, prevCoords[1]};
                break;
            case 'E':
                testCoords = new int[]{prevCoords[0], prevCoords[1] + 1};
                break;
            case 'W':
                testCoords = new int[]{prevCoords[0], prevCoords[1] - 1};
                break;

        }
        try {

            if (map.fetchItem(testCoords) == '#') {

                if (player.equals("human")) {
                    returnMessage( "Failure, Cannot Move Here");
                }

            } else {


                if (player.equals("human")) {
                    playerCoords = testCoords;
                    mapStates.put(currentUser,playerCoords);

                } else if (player.equals("bot")) {
                    returnMessage( "Bot Chases");
                    enemyCoords = testCoords;


                }

                if (Arrays.equals(playerCoords, enemyCoords)) {
                    returnMessage( "You were devoured by the Bot");
                    FailGame();
                }


            }

            // Out of bounds just means the player is moving to somewhere it cannot reach which is of course a failed move.
        } catch (ArrayIndexOutOfBoundsException | IOException e) {
            returnMessage( "Failure, Cannot Move Here");
        }

    }

    public char[][] returnFivebyFiveMap(int[] centre) {
        char[][] mapOutput = new char[5][5];
        char[][] fullMap = map.getMap();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                try {
                    int [] check  = new int[]{centre[0] - 2 + x,centre[1] - 2 + y};
                    if ((x == 2 && y == 2 && centre == playerCoords) || (centre[0] - 2 + x == playerCoords[0] && centre[1] - 2 + y == playerCoords[1])) {
                        mapOutput[x][y] = 'P';
                    }
                    else if ((centre[0] - 2 + x == enemyCoords[0] && centre[1] - 2 + y == enemyCoords[1]) && centre != enemyCoords) {
                        mapOutput[x][y] = 'B';
                    }else if(mapStates.containsValue(check) && centre != playerCoords) {
                            mapOutput[x][y] = 'P';

                    } else {
                        mapOutput[x][y] = fullMap[centre[0] - 2 + x][centre[1] - 2 + y];
                    }
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    mapOutput[x][y] = '#';
                }
            }
        }
        return mapOutput;
    }
    public void outputArray(char[][] arr) throws IOException {

        returnMessage("");
        for (char[] chars : arr) {
            String row = new String(chars);
            returnMessage(row);
        }
        returnMessage( "");
    }
    public void SuccessGame() throws IOException {
        returnMessage( "WIN, exiting game for all players");
        outStream.writeObject(new Packet("", "__DOD__CLIENT__", "__ENDGAME__"));
        DoDClient.oos.clear();
    }
    public void FailGame() throws IOException {
        returnMessage( "LOSE, exiting game for all players");
        outStream.writeObject(new Packet("", "__DOD__CLIENT__", "__ENDGAME__"));
        DoDClient.oos.clear();
    }
    public static void main(String[] args) {
    }

}