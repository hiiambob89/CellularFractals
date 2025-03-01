package cellularfractals.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import cellularfractals.engine.World;
import cellularfractals.particles.Particle;
import cellularfractals.particles.particles.*;
import cellularfractals.engine.ParticleThreadPool;

public class MyPanel extends JPanel {
    private World world;
    private CustomCanvas canvas;
    private Timer updateTimer;
    private Map<String, BiFunction<Double, Double, Particle>> particleFactories = new HashMap<>();
    private BiFunction<Double, Double, Particle> selectedFactory = null;
    private static final long SPAWN_COOLDOWN = 100; // 50ms cooldown
    private long lastSpawnTime = 0;
    private boolean showVectorArrows = true; // New field to track vector arrow visibility

    public MyPanel(World world) {
        this.world = world;
        setLayout(new BorderLayout());

        // Create custom canvas for particle visualization
        canvas = new CustomCanvas();
        canvas.setPreferredSize(new Dimension(400, 400));

        // Add both mouse listener and mouse motion listener for better click detection
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseClick(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        // Add specific method to handle clicks
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        add(canvas, BorderLayout.CENTER);

        // Create control panel with GridBagLayout for better organization
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        // Add information labels
        controlPanel.add(new JLabel("Grid Size: " + world.getWidth() + " x " + world.getHeight()), gbc);
        gbc.gridy++;
        JLabel particleCountLabel = new JLabel("Particles: " + world.getParticleCount());
        controlPanel.add(particleCountLabel, gbc);
        gbc.gridy++;

        // Add vector arrows toggle
        JCheckBox vectorArrowsToggle = new JCheckBox("Show Vector Arrows", showVectorArrows);
        vectorArrowsToggle.addActionListener(e -> {
            showVectorArrows = vectorArrowsToggle.isSelected();
            canvas.repaint();
        });
        controlPanel.add(vectorArrowsToggle, gbc);
        gbc.gridy++;

        // Add particle type buttons panel
        JPanel particleButtonsPanel = new JPanel(new GridLayout(0, 1, 2, 2));
        particleButtonsPanel.setBorder(BorderFactory.createTitledBorder("Spawn Particles"));
        controlPanel.add(particleButtonsPanel, gbc);
        gbc.gridy++;

        /*
         * PUT PARTICLE TYPES HERE WITH CORRECT INFO FOUND IN CONSTRUCTOR FOR THE PARTICLE CLASS PLZ
         *
         */
        registerParticleType("Basic Particle", (x, y) ->
            new BasicParticle(world, x, y, 0, 0));

        registerParticleType("Gravity Particle", (x, y) ->
            new GravityParticle(world, x, y, 0, 0, 10f, 1f));

        registerParticleType("Anti-Gravity Particle", (x, y) ->
            new GravityParticle(world, x, y, 0, 0, 100f, -1f));

        registerParticleType("Demo Particle", (x, y) ->
            new DemoParticle(world, x, y, 0, 0));

        registerParticleType("Ghost particle", (x, y) ->
            new GhostParticle(world, x, y, 0, 0));
        // Create buttons for each particle type
        for (Map.Entry<String, BiFunction<Double, Double, Particle>> entry : particleFactories.entrySet()) {
            JButton button = new JButton(entry.getKey());
            button.addActionListener(e -> {
                selectedFactory = entry.getValue();
                for (Component c : particleButtonsPanel.getComponents()) {
                    if (c instanceof JButton) {
                        c.setBackground(null);
                    }
                }
                button.setBackground(Color.LIGHT_GRAY);
            });
            particleButtonsPanel.add(button);
        }

        // Add reset button
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            world.clear();
            selectedFactory = null;
            for (Component c : particleButtonsPanel.getComponents()) {
                if (c instanceof JButton) {
                    c.setBackground(null);
                }
            }
            canvas.repaint();
        });
        controlPanel.add(resetButton, gbc);

        add(controlPanel, BorderLayout.EAST);

        // Setup update timer for animation
        updateTimer = new Timer(16, e -> {
            particleCountLabel.setText("Particles: " + world.getParticleCount());
            canvas.repaint();
        });
        updateTimer.start();
    }

    public void registerParticleType(String name, BiFunction<Double, Double, Particle> factory) {
        particleFactories.put(name, factory);
    }

    private void handleMouseClick(MouseEvent e) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpawnTime < SPAWN_COOLDOWN) {
            return;  // Still in cooldown period
        }

        if (selectedFactory != null && canvas.getBounds().contains(e.getPoint())) {
            Point canvasPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), canvas);

            // Convert screen coordinates to world coordinates with bounds checking
            double worldX = Math.max(0, Math.min(world.getWidth(),
                (canvasPoint.x * world.getWidth()) / canvas.getWidth()));
            double worldY = Math.max(0, Math.min(world.getHeight(),
                (canvasPoint.y * world.getHeight()) / canvas.getHeight()));

            // Create and add the particle
            Particle newParticle = selectedFactory.apply(worldX, worldY);
            boolean added = world.addParticle(newParticle);

            if (added) {
                lastSpawnTime = currentTime;  // Update last spawn time
                System.out.println("Particle added at: " + worldX + "," + worldY);
            }
        }
    }

    private class CustomCanvas extends JPanel {

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

                // Calculate screen radius (scale the world radius to screen coordinates)
                int screenRadius = (int)(particle.getRadius() * width / world.getWidth());

                // Use particle's cosmetic settings
                g2d.setColor(particle.cosmeticSettings != null ?
                    particle.cosmeticSettings.color : Color.WHITE);

                g2d.fillOval(screenX - screenRadius,
                            screenY - screenRadius,
                            screenRadius * 2,
                            screenRadius * 2);

                // Draw velocity vector from center of particle with trail color
                // Only if vector arrows are enabled
                if (showVectorArrows) {
                    g2d.setColor(particle.cosmeticSettings != null ?
                        particle.cosmeticSettings.trailColor : Color.CYAN);
                    int velX = (int)(particle.getDx() * 20); // Scale velocity for visualization
                    int velY = (int)(particle.getDy() * 20);
                    g2d.drawLine(screenX, screenY, screenX + velX, screenY + velY);
                }
            }
        }
    }

    // Clean up resources
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        ParticleThreadPool.shutdown();
    }
}
