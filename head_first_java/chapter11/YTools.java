import java.io.*;

public class YTools {
    public static void WaitForUserInput() {
        System.out.print("Press enter to continue ");
        try {
            BufferedReader is = new BufferedReader(
                new InputStreamReader(System.in)
            );
            is.readLine();
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }
}
