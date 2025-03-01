package cellularfractals.GUI;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.GridLayout;

public class MyPanel extends JPanel {
    public MyPanel() {
        // Set the layout of the JPanel
        setLayout(new BorderLayout());

        // Create a canvas for the left side
        Canvas canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(400, 400));
        add(canvas, BorderLayout.CENTER);

        // Create a panel for the buttons on the right side
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1)); // One column, multiple rows

        // Add buttons to the button panel
        buttonPanel.add(new JButton("Button 1"));
        buttonPanel.add(new JButton("Button 2"));
        buttonPanel.add(new JButton("Button 3"));

        // Add the button panel to the right side of the main panel
        add(buttonPanel, BorderLayout.EAST);
    }
}
