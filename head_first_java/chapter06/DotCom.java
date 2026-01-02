import java.util.*;

public class DotCom {
    private String name;
    private ArrayList<String> locationCells;

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }

    public void setLocationCells(ArrayList<String> lcells) {
        locationCells = lcells;
    }

    public String checkYourself(String userInput) {
        String result = "Missed";
        int index = locationCells.indexOf(userInput);
        if (index >= 0) {
            locationCells.remove(index);
            if (locationCells.isEmpty()) {
                result = "Drown";
                System.out.println("Wow! You've drown " + name + " :(");
            } else {
                result = "Hit!";
            }
        }
        return result;
    }
}
