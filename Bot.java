import java.util.Vector;

public class Bot {

    private int[] enemyGridCoordinates;

    private char[][] recentMapView;

    private int[] recentHumanCoordinates;

    private int[] tempEnemyCoordinates;

    private char[] actionList = new char[]{'W', 'N', 'E', 'S'};

    private Vector<String> movesToExecute;

    private int m = 2;

    private char moveDirection;

    private Vector<String> previousTwoMoves;

    private int duplicateMove;

    int notFound;

    public Bot() {
        // initialise the map memory for the bot
        recentMapView = new char[5][5];

        //intialise the player memory for the bot
        recentHumanCoordinates = new int[]{-1, -1};

        // The coordinates stored here are always relative to the 5x5 map of which the bot is always
        // taken to be at the centre.
        enemyGridCoordinates = new int[]{2, 2};

        // vector to hold the moves to execute on the move turns of the bot
        movesToExecute = new Vector<>(2);

        // set some random start direction as a placeholder
        moveDirection = actionList[(int) (Math.random()) * 4];

        // hold previous two moves in order to check for duplicate moves of the bot.
        previousTwoMoves = new Vector<>();


    }

    public String getNextAction() {
        // if 2 moves have been then reset the counter and call LOOK again to update memory and
        // produce a new move set
        if (m == 2) {
            m = 0;

            // use this value when searching neighbours at depth 2
            // Once a move has been determined it updates temp enemy coordinates to be the location of where this move would
            // be in order to use this to determine the next neighbours the bot must consider without changing the actual location
            // of where the bot last saw the enemy.
            tempEnemyCoordinates = enemyGridCoordinates;

            // execute LOOK
            return "LOOK";

        } else {

            // increment m which is the depth essentially.
            m++;

            // fetch the next move
            String nextMove = movesToExecute.elementAt(0);

            // remove the  move from the vector
            movesToExecute.removeElementAt(0);

            // extract the direction of the move to use for checking whether a move is possible
            String[] pointTo = nextMove.split("MOVE ");
            char directionInputValue = pointTo[1].charAt(0);

            // if bot has done same set of moves more than 20 times just generate a random move
            if (duplicateMove > 20){
                duplicateMove = 0;
                return "Move " + generateRandomMove();
            }

            // check to see if the move doesn't go into a wall '#'
            if (isMovePossible(directionInputValue)) {

                return nextMove;
            } else {
                // if the move does go into a wall then return a random move


                return "Move " + generateRandomMove();
            }

        }

    }



    public char getMoveDirection() {

        // set move as the default move direction assigned at the creation of the bot
        char move = moveDirection;

        // set lowest value to be a value that is impossible to be greater than.
        double lowestValue = Double.POSITIVE_INFINITY;

        // if the bot has no memory of the location of the player , its coordinates will be [-1][-1]
        // therefore just test for a single -1 and then move the bot randomly.

        if (recentHumanCoordinates[0] == -1) {
            return generateRandomMove();
        }

        // Go through each of the 4 potential new locations the bot can move
        for (int i = 0; i < 4; i++) {

            // actionList[i] holds a direction and the coordinates being used are temp as its
            // searching ahead in future to test a move set
            int[] evaluateLocation = testDirection(tempEnemyCoordinates, actionList[i]);

            // calculate the manhattan distance at that new location to the player
            double value = manhattanDistance(evaluateLocation);

            // discard any move that moves to a wall
            if (getTileAtCoordinate(evaluateLocation) == '#') {

                continue;

            // if a move gets to a player always select and return straight off.
            } else if (getTileAtCoordinate(evaluateLocation) == 'P') {

                return actionList[i];
            }

            // replace the value with the lower value found
            if (value < lowestValue) {
                lowestValue = value;

                // set the new move to return to be the direction that generated this lower value.
                move = actionList[i];
            }
        }

        // update the coordinates so if the bot tests N for example, this would now hold the coordinates
        // in direction of north to test for the second depth.
        tempEnemyCoordinates = testDirection(tempEnemyCoordinates, move);

        return move;


    }




    public char generateRandomMove() {
        char randomMove;

        // Loops until its find a move that doesn't lead the bot into a wall.
        while (true){
            // generate a random move from the list of moves.
            randomMove = actionList[(int) (Math.random() * 4)];

            // passes in the coordinates of the enemy alongside the proposed move in form of coordinates to
            // test whether the tile at those coordinates is a wall.
            if (!(getTileAtCoordinate(testDirection(enemyGridCoordinates,randomMove))=='#')){
                return randomMove;
            }
        }
    }


    // simple manhattan distance calculation using the designated coordiantes on the map
    public double manhattanDistance(int[] coordinates) {

        int distance = Math.abs((recentHumanCoordinates[0]) - (coordinates[0] )) + Math.abs((recentHumanCoordinates[1]) - (coordinates[1]));
        return distance;

    }


    // when the bot is scanning this is called to try and update the memory it has of the player.
    public boolean locatePlayer(){


        // Search through the map memory the bot has
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                // if any location on there has 'P' for player then store these coordinates.
                if (recentMapView[i][j] == 'P') {

                    recentHumanCoordinates = new int[]{i, j};

                    // indicate the player has been found
                    return true;
                }
            }
        }
        // indicate the player has not been found
        return false;
    }

    public void environmentScan(char[][] array) {

        boolean testFindPlayer;

        // update the map view using the passed in array which is the surroundings the bot
        // will use to generate the next two moves.
        recentMapView = new char[5][5];
        recentMapView = array;

        // try and find the player and store this result as a boolean
        testFindPlayer = locatePlayer();

        // If it has not located the player
        if(!testFindPlayer){
            //increment notFound
            notFound++;

            //If they have not located the player for more than 4 times scanning then
            // reset the memory of the coordinates of the player.
            if (notFound > 3){

                // reset not found counter.
                notFound = 0;
                recentHumanCoordinates = new int[]{-1,-1};
            }
        }

        // Generate the next two moves.
        fillMoveSet();


    }

    protected char getTileAtCoordinate(int[] coordinates) throws ArrayIndexOutOfBoundsException {

        // fetch an item at the specified coordinates ensuring this throws exceptions for out of bounds errors.
        return recentMapView[coordinates[0]][coordinates[1]];

    }


    public static int[] testDirection(int[] oldCoordinates, char direction) {

        // set coordinates to be returned
        int[] newCoordinates = new int[]{-1, -1};

        //just as in game logic test the locations to move in specified direction
        switch (direction) {
            case 'N':
                newCoordinates = new int[]{oldCoordinates[0] - 1, oldCoordinates[1]};
                break;
            case 'S':
                newCoordinates = new int[]{oldCoordinates[0] + 1, oldCoordinates[1]};
                break;
            case 'E':
                newCoordinates = new int[]{oldCoordinates[0], oldCoordinates[1] + 1};
                break;
            case 'W':
                newCoordinates = new int[]{oldCoordinates[0], oldCoordinates[1] - 1};
                break;
        }

        // return the test coordinates.
        return newCoordinates;
    }

    public boolean isMovePossible(char direction) {

        try {

            // test to see if a certain location being tested in a direction is a wall
            if (getTileAtCoordinate(testDirection(tempEnemyCoordinates, direction)) != '#') {
                return true;
            } else {
                return false;
            }
        } catch (ArrayIndexOutOfBoundsException a) {
            // any out of bounds exception is returned as a true value as if the bot
            // was to know that an out of bounds was false then it would just loop endleslly and never return a move.
            return true;
        }
    }


    public void fillMoveSet() {


        // Fill the next two moves using the getMoveDirection generator.
        for (int m = 0; m < 2; m++) {

           movesToExecute.add("MOVE " + getMoveDirection());

        }

        // check for duplicate moves and increment if so
        if (previousTwoMoves.equals(movesToExecute)){
            duplicateMove++;
        }

        // update the previous two moves value to hold the current moves to execute.
        previousTwoMoves = movesToExecute;



    }


    public void outputArray(char[][] arr) {

        System.out.println("");

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[i][j]);

                if (arr[i].length - j <= 1) {
                    System.out.println("");
                }


            }

        }
        System.out.println("");

    }

}
