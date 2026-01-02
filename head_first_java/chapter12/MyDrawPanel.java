import java.awt.*;
import javax.swing.*;

public class MyDrawPanel extends JPanel {
    public static void main(String[] args) {
        MyDrawPanel gui = new MyDrawPanel();
        gui.go();
    }

    public void go() {
        JFrame frame = new JFrame();
        frame.add(this);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Color startColor = new Color(
            (int) (Math.random() * 255),
            (int) (Math.random() * 255),
            (int) (Math.random() * 255)
        );
        Color endColor = new Color(
            (int) (Math.random() * 255),
            (int) (Math.random() * 255),
            (int) (Math.random() * 255)
        );
        GradientPaint gradient = new GradientPaint(
            70, 70, startColor, 150, 50, endColor);
        g2d.setPaint(gradient);
        g2d.fillOval(70, 70, 100, 100);
    }
}
