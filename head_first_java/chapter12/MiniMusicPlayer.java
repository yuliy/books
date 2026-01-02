import javax.sound.midi.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;

public class MiniMusicPlayer {
    static final int CMD_NOTE_ON = 144;
    static final int CMD_NOTE_OFF = 128;
    static final int CMD_CONTROLLER_EVENT = 176;
    static final int CMD_CHANGE_INSTRUMENT = 192;

    static JFrame f = new JFrame("My first musical performance");
    static MyDrawPanel ml;

    public static void main(String[] args) {
        MiniMusicPlayer mini = new MiniMusicPlayer();
        mini.go();
    }

    public void setUpGui() {
        ml = new MyDrawPanel();
        f.setContentPane(ml);
        f.setBounds(30, 30, 500, 500);
        f.setVisible(true);
    }

    public void go() {
        setUpGui();

        try {
            Sequencer sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.addControllerEventListener(ml, new int[] {127});
            Sequence seq = new Sequence(Sequence.PPQ, 4);
            Track track = seq.createTrack();

            final int instrument = 60;
            track.add(makeEvent(CMD_CHANGE_INSTRUMENT, 1, instrument, 0, 0));

            int r = 0;
            for (int i = 0; i < 60; i += 4) {
                r = (int) ((Math.random() * 50) + 40);
                track.add(makeEvent(CMD_CONTROLLER_EVENT, 1, 127, 0,   i));
                track.add(makeEvent(CMD_NOTE_ON,          1, r,   100, i));
                track.add(makeEvent(CMD_NOTE_OFF,         1, r,   100, i+10));
            }

            sequencer.setSequence(seq);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception x) {
            x.printStackTrace();
        }
        return event;
    }

    class MyDrawPanel extends JPanel implements ControllerEventListener {
        boolean msg = false;

        public void controlChange(ShortMessage event) {
            msg = true;
            repaint();
        }

        public void paintComponent(Graphics g) {
            if (msg) {
                Graphics2D g2 = (Graphics2D) g;

                g.setColor(new Color(
                    (int) (Math.random() * 250),
                    (int) (Math.random() * 250),
                    (int) (Math.random() * 250)
                ));

                int height = (int) ((Math.random() * 290) + 10);
                int width = (int) ((Math.random() * 290) + 10);

                int x = (int) ((Math.random() * 200) + 10);
                int y = (int) ((Math.random() * 200) + 10);

                g.fillRect(x, y, height, width);
                msg = false;
            }
        }
    }
}
