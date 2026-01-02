import javax.sound.midi.*;

public class MiniMiniMusicApp {
    public static void main(String[] args) {
        MiniMiniMusicApp mini = new MiniMiniMusicApp();
        if (args.length < 2) {
            System.out.println("Usage: <instrument> <note>");
        } else {
            int instrument = Integer.parseInt(args[0]);
            int note = Integer.parseInt(args[1]);
            mini.play(instrument, note);
        }
    }

    public void play(int instrument, int note) {
        try {
            System.out.println("Initializing MIDI content...");
            Sequencer player = MidiSystem.getSequencer();
            player.open();
            Sequence seq = new Sequence(Sequence.PPQ, 4);
            Track track = seq.createTrack();

            ShortMessage q = new ShortMessage();
            q.setMessage(192, 1, instrument, 0);
            MidiEvent changeInstrument = new MidiEvent(q, 1);
            track.add(changeInstrument);

            ShortMessage a = new ShortMessage();
            a.setMessage(144, 1, note, 100);
            MidiEvent noteOn = new MidiEvent(a, 2);
            track.add(noteOn);

            ShortMessage b = new ShortMessage();
            b.setMessage(128, 1, note, 100);
            MidiEvent noteOff = new MidiEvent(b, 16);
            track.add(noteOff);

            System.out.println("Starting player...");
            player.setSequence(seq);
            player.start();
            System.out.println("Playing finished.");
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
