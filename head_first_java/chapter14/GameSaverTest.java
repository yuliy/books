import java.io.*;

public class GameSaverTest {
    public static void main(String[] args) {
        GameCharacter one = new GameCharacter(50, "Elf", new String[] {"bow", "sword", "castet"});
        GameCharacter two = new GameCharacter(200, "Troll", new String[] {"fists", "huge axe"});
        GameCharacter three = new GameCharacter(120, "Sorcerer", new String[] {"spells", "invisibility"});

        try {
            ObjectOutputStream os = new ObjectOutputStream(
                new FileOutputStream("Game.ser"));
            os.writeObject(one);
            os.writeObject(two);
            os.writeObject(three);
            os.close();
        } catch (IOException x) {
            x.printStackTrace();
        }

        one = null;
        two = null;
        three = null;

        try {
            ObjectInputStream is = new ObjectInputStream(
                new FileInputStream("Game.ser"));
            GameCharacter oneRestore = (GameCharacter) is.readObject();
            GameCharacter twoRestore = (GameCharacter) is.readObject();
            GameCharacter threeRestore = (GameCharacter) is.readObject();

            System.out.println("1st object type: " + oneRestore.getType());
            System.out.println("2nd object type: " + twoRestore.getType());
            System.out.println("3rd object type: " + threeRestore.getType());
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
