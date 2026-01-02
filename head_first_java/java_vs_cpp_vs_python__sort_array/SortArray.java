import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;

public class SortArray {
    public static void main(String[] args) {
        System.out.println("");
        System.out.println("<<< JAVA >>>");
        final int size = 100 * 1000 * 1000;
        System.out.println("Size: " + size);

        //
        Instant start = Instant.now();
        ArrayList<Integer> array = generateRandomArray(size);
        printTimeCheckPoint(start, "Random array generated in ");

        //
        start = Instant.now();
        Collections.sort(array);
        printTimeCheckPoint(start, "Array sorted in ");

        //
        start = Instant.now();
        System.out.println("sorted: " + checkSorted(array));
        printTimeCheckPoint(start, "Sort correctness is checked in ");
    }

    public static void printTimeCheckPoint(Instant start, String msg) {
        System.out.println(msg
            + Duration.between(start, Instant.now()).toNanos() / 1000000.0
            + " ms"
        );
    }

    public static ArrayList<Integer> generateRandomArray(int size) {
        ArrayList<Integer> res = new ArrayList<Integer>();
        for (int i = 0; i < size; ++i) {
            int elem = (int) (Math.random() * 2e6 - 1e6);
            res.add(elem);
        }
        return res;
    }

    public static boolean checkSorted(ArrayList<Integer> arr) {
        final int size = arr.size();
        for (int i = 1; i < size; ++i) {
            if (arr.get(i) < arr.get(i-1))
                return false;
        }
        return true;
    }
}
