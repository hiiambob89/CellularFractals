package cellularfractals.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cellularfractals.engine.World;
import cellularfractals.particles.Particle;
import cellularfractals.particles.particles.GravityParticle;
import cellularfractals.particles.particles.BasicParticle;

public class MyPanel extends JPanel {
    private World world;
    private CustomCanvas canvas;
    private Timer updateTimer;

    public MyPanel(World world) {
        this.world = world;
        setLayout(new BorderLayout());

        // Create custom canvas for particle visualization
        canvas = new CustomCanvas();
        canvas.setPreferredSize(new Dimension(400, 400));
        add(canvas, BorderLayout.CENTER);

        // Create control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(0, 1));
        
        // Add information labels
        JLabel sizeLabel = new JLabel(String.format("Grid Size: %.1f x %.1f", world.getWidth(), world.getHeight()));
        JLabel particleCountLabel = new JLabel("Particles: " + world.getParticleCount());
        
        // Add buttons
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            world.clear();
            canvas.repaint();
        });

        // Add components to control panel
        controlPanel.add(sizeLabel);
        controlPanel.add(particleCountLabel);
        controlPanel.add(resetButton);
        add(controlPanel, BorderLayout.EAST);

        // Setup update timer for animation
        updateTimer = new Timer(16, e -> {
            particleCountLabel.setText("Particles: " + world.getParticleCount());
            canvas.repaint();
        });
        updateTimer.start();
    }

    private class CustomCanvas extends JPanel {
        private static final int PARTICLE_SIZE = 6;
        
        public CustomCanvas() {
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // Enable antialiasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw grid lines
            g2d.setColor(new Color(30, 30, 30));
            double cellSize = 10.0;
            int width = getWidth();
            int height = getHeight();
            
            // Vertical lines
            for (double x = 0; x < world.getWidth(); x += cellSize) {
                int screenX = (int)(x * width / world.getWidth());
                g2d.drawLine(screenX, 0, screenX, height);
            }
            
            // Horizontal lines
            for (double y = 0; y < world.getHeight(); y += cellSize) {
                int screenY = (int)(y * height / world.getHeight());
                g2d.drawLine(0, screenY, width, screenY);
            }

            // Draw particles
            for (Particle particle : world.getParticles()) {
                // Convert world coordinates to screen coordinates
                int screenX = (int)(particle.getX() * width / world.getWidth());
                int screenY = (int)(particle.getY() * height / world.getHeight());
                
                // Draw different particle types in different colors
                if (particle instanceof GravityParticle) {
                    g2d.setColor(Color.RED);
                } else {
                    g2d.setColor(Color.WHITE);
                }
                
                g2d.fillOval(screenX - PARTICLE_SIZE/2, 
                            screenY - PARTICLE_SIZE/2, 
                            PARTICLE_SIZE, 
                            PARTICLE_SIZE);
                
                // Draw velocity vector
                g2d.setColor(Color.CYAN);
                // int velX = (int)(particle.getDx() * 20); // Scale velocity for visualization
                // int velY = (int)(particle.getDy() * 20);
                // g2d.drawLine(screenX, screenY, screenX + velX, screenY + velY);
            }
        }
    }

    // Clean up resources
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }
}
