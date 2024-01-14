import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


/**
 * Reads and contains in memory the map of the game.
 *
 */
public class MapMain {

    /* Representation of the map */
    private char[][] map;

    /* Map name */
    private String mapName;

    /* Gold required for the human player to win */
    private int goldRequired;

    /* For validation of map tiles */
    private List<Character> mapTiles = Arrays.asList('#','E','.','G');

    /* Validation whether exit exists*/
    private boolean exit = false;

    /* Validation for a correct ammount of gold*/
    private int goldCheck = 0;


    /**
     * Default constructor, creates the default map "Very small Labyrinth of doom".
     */
    public MapMain() {
        mapName = "Very small Labyrinth of Doom";
        goldRequired = 2;
        map = new char[][]{
                {'#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#'},
                {'#','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','#'},
                {'#','.','.','.','.','.','.','G','.','.','.','.','.','.','.','.','.','E','.','#'},
                {'#','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','#'},
                {'#','.','.','E','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','#'},
                {'#','.','.','.','.','.','.','.','.','.','.','.','G','.','.','.','.','.','.','#'},
                {'#','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','#'},
                {'#','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','#'},
                {'#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#','#'}
        };
    }

    /**
     * Constructor that accepts a map to read in from.
     *
     * @param : The filename of the map file.
     */

    public MapMain(String fileName) throws Exception {
        readMap(fileName);
    }

    /**
     * @return : Gold required to exit the current map.
     */
    protected int getGoldRequired() {

        return goldRequired;
    }

    /**
     * @return : The map as stored in memory.
     */
    protected char[][] getMap() {
        return map;
    }


    /**
     * @return : The name of the current map.
     */
    protected String getMapName() {
        return mapName;
    }


    /**
     * Reads the map from file.
     *
     * @param : Name of the map's file.
     */
    protected void readMap(String fileName) throws Exception {



        // Locate the file entered by the user and store it in a File object.
        File myObj = new File("./"+fileName);

        // start a new scanner in order to read the file
        Scanner reader = new Scanner(myObj);

        // fetch the first two lines
        String topLine = reader.nextLine();

        String secondLine = reader.nextLine();

        // if the top line contains a number then it has to be the gold required value therefore
        // set goldRequired as the top line and mapName second
        if (topLine.matches(".*\\d.*")){
            goldRequired = Integer.parseInt(topLine.replaceAll("\\D+",""));
            mapName = secondLine.replace("name", "");
        }
        else{
            // vice versa if its not.
            mapName = topLine.replace("name", "");
            goldRequired = Integer.parseInt(secondLine.replaceAll("\\D+",""));
        }

        if (goldRequired < 0){

            // if the map has negative gold count then just revert it to positive.
            goldRequired = -goldRequired;
        }

        // count the number of files negating 2 for the first 2 lines that have been read already
        int currentLine = countLinesOfFile(fileName) - 2;

        // create a new map to be read where the number of rows is known already from currentLine.
        map = new char[currentLine][];

        // read each line as a character array
        for (int i = 0; i < currentLine; i++) {

            // store an array of the characters on each row to validate.
            char[] arrayToCheck = reader.nextLine().toCharArray();

            for (int c = 0; c < arrayToCheck.length; c++){

                // any invalid character just set it to be a wall.
                if (!(mapTiles.contains(arrayToCheck[c])) ){
                    arrayToCheck[c] = '.';
                }
                // check this if an exit is found
                if (arrayToCheck[c] == 'E'){
                    exit = true;
                }

                // count number of gold in map to determine valid ammount.
                if(arrayToCheck[c] == 'G'){
                    goldCheck++;
                }
            }
            // load the character array to the map at each row.
            map[i] = arrayToCheck;

            if (map[i].length != map[0].length) {
                throw new RuntimeException("The map is of invalid shape, it must be rectangular to be read in");
            }

        }
        // cant play a map with no exit.
        if (!exit){
            throw new RuntimeException("Invalid Map , contains no exit tile");
        }

        // cant play a map with the gold required to win more than the gold in the map.
        if (goldCheck < goldRequired){
            throw new RuntimeException("Invalid Map, You map does not contain enough gold to win");
        }

        // close the reader
        reader.close();
    }

    public int countLinesOfFile(String filename){

        // keep reading and on each line increment the number of lines.
        int lines = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader("./"+filename))){

            // while the reader is still reading something from the file increment the number of lines
            while(reader.readLine() != null) lines++;

        // any error whilst reading is printed out here.
        } catch (IOException e){
            e.printStackTrace();

        }

        return lines;

    }

    // fetches the item at the associated coordinates on the map
    public char fetchItem(int[] coordinates ){

        return map[coordinates[0]][coordinates[1]];

    }

    // sets the item at the specified coordinates to a floor tile
    public void destroyItem(int[] coordinates){
        map[coordinates[0]][coordinates[1]] = '.';
    }

    // returns dimensions of the map to use for the random spawns of bot and human
    public int[] returnSizeOfMap(){

        return new int[] {map.length, map[0].length};

    }




}
