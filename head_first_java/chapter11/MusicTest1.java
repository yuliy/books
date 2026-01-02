import javax.sound.midi.*;

public class MusicTest1 {
    public void play() throws Exception {
        try {
            Sequencer player = MidiSystem.getSequencer();
            System.out.println("We have a synthesizer");
            YTools.WaitForUserInput();
            throw new Exception("Hello!");
        } catch (MidiUnavailableException e) {
            System.out.println("We have a problem: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            MusicTest1 mt = new MusicTest1();
            mt.play();
        } catch (Exception e) {
            System.out.println("We have a problem in main(): " + e.getMessage());
            e.printStackTrace();
        }
    }
}
