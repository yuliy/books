public class GuessGame {
    Player p1;
    Player p2;
    Player p3;

    public void startGame() {
        System.out.println("Game started...");

        p1 = new Player();
        p2 = new Player();
        p3 = new Player();

        boolean p1isRight = false;
        boolean p2isRight = false;
        boolean p3isRight = false;

        int targetNumber = (int) (Math.random() * 10);
        System.out.println("Random number generated.");

        while (!p1isRight & !p2isRight & !p3isRight) {
            System.out.println("\nNew round...");
            System.out.println("The number to be guessed is: " + targetNumber);

            System.out.println("1st player thinks it is: " + p1.guess());
            System.out.println("2nd player thinks it is: " + p2.guess());
            System.out.println("3rd player thinks it is: " + p3.guess());

            p1isRight = p1.number == targetNumber;
            p2isRight = p2.number == targetNumber;
            p3isRight = p3.number == targetNumber;
        }

        System.out.println("\nWe have a winner!");
        System.out.println("1st player is right? " + p1isRight);
        System.out.println("2nd player is right? " + p2isRight);
        System.out.println("3rd player is right? " + p3isRight);
        System.out.println("Game over.");
    }
}
