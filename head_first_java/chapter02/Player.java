public class Player {
    int number = 0;

    public int guess() {
        number = (int) (Math.random() * 10);
        System.out.println("I think this number is " + number);
        return number;
    }
}
