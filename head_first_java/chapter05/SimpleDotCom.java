import java.util.ArrayList;

public class SimpleDotCom {
    private ArrayList<String> locationCells;

    public void setLocationCells(ArrayList<String> lcells) {
        locationCells = lcells;
    }

    public String checkYourself(String stringGuess) {
        //int guess = Integer.parseInt(stringGuess);
        String result = "Missed";

        int index = locationCells.indexOf(stringGuess);
        if (index >= 0) {
            locationCells.remove(index);

            if (locationCells.isEmpty()) {
                result = "Drown";
            } else {
                result = "Hit!";
            }
        }

        System.out.println(result);
        return result;
    }
}
