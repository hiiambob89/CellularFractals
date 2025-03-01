package cellularfractals.GUI;

import javax.swing.JFrame;

public class MainFrame extends JFrame {
    public MainFrame() {
        // Set the title of the JFrame
        setTitle("Particle Interaction Simulator");

        // Set the default close operation
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create an instance of the custom JPanel
        MyPanel customPanel = new MyPanel();

        // Add the custom panel to the JFrame
        add(customPanel);

        // Set the size of the JFrame
        setSize(600, 400);

        // Make the JFrame not resizable
        setResizable(false);

        // Pack the components within the JFrame
        pack();

        // Set the JFrame to be visible
        setVisible(true);
    }

    public static void main(String[] args) {
        // Create an instance of the MainFrame class
        new MainFrame();
    }
}
