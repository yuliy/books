import java.util.ArrayList;

public class SimpleDotComGame {
    private static final int DOT_COM_SIZE = 3;
    private static final int LOCATION_LEN = 7;

    public static void main(String[] args) {
        SimpleDotComGame game = new SimpleDotComGame();
        game.start();
    }

    public void start() {
        System.out.println("Game started...");
        int numOfGuesses = 0;
        SimpleDotCom dot = new SimpleDotCom();
        generateDotComLocation(dot);
        GameHelper helper = new GameHelper();

        String result;
        while (true) {
            ++numOfGuesses;
            String guess = helper.getUserInput("Enter the number: ");
            result = dot.checkYourself(guess);
            if (result.equals("Drown")) {
                break;
            }
        }
        System.out.println("Game over.");
        System.out.println("It took you "
            + numOfGuesses
            + " step(s) to win the game."
        );
    }

    private void generateDotComLocation(SimpleDotCom dot) {
        int start = (int) (Math.random() * (LOCATION_LEN - DOT_COM_SIZE + 1));
        ArrayList<String> lcells = new ArrayList<String>();
        lcells.add(Integer.toString(start));
        lcells.add(Integer.toString(start+1));
        lcells.add(Integer.toString(start+2));
        dot.setLocationCells(lcells);
    }
}
