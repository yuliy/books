import java.util.*;

public class DotComBust {
    private GameHelper helper = new GameHelper();
    private ArrayList<DotCom> dotComs = new ArrayList<DotCom>();
    int numOfGuesses = 0;

    public static void main(String[] args) {
        DotComBust game = new DotComBust();
        game.setUpGame();
        game.start();
        game.finish();
    }

    private void setUpGame() {
        System.out.println("Initializing new game...");
        String[] names = {"godaddy.com", "google.com", "facebook.com"};
        //for (var i = 0; i < names.length; ++i) {
        for (String name : names) {
            DotCom dot = new DotCom();
            dot.setName(name);
            ArrayList<String> location = helper.placeDotCom(3);
            dot.setLocationCells(location);
            dotComs.add(dot);
        }

        System.out.println("Your purpose is to drown three dotcom's:");
        for (String name : names) {
            System.out.println("\t" + name);
        }
        System.out.println("Try to do it using as little steps as possible.");
    }

    private void start() {
        System.out.println("Game started...");
        while (!dotComs.isEmpty()) {
            String guess = helper.getUserInput("Your turn: ");
            checkUserGuess(guess);
        }
    }

    private void checkUserGuess(String guess) {
        ++numOfGuesses;
        String result = "Missed";

        for (DotCom dot : dotComs) {
            result = dot.checkYourself(guess);
            if (result.equals("Hit!")) {
                break;
            }
            if (result.equals("Drown")) {
                dotComs.remove(dot);
                break;
            }
        }
        System.out.println(result);
    }

    private void finish() {
        System.out.println("Game over.");
        System.out.println("It took you "
            + numOfGuesses
            + " step(s) to win the game."
        );
    }
}
