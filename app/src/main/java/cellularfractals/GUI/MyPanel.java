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
    private JPanel parameterPanel; // Panel to hold parameter sliders
    private Map<String, Map<String, Double>> particleParameters; // Store parameters for each particle type
    private String selectedParticleType = null; // Currently selected particle type
    private boolean parameterPanelExpanded = true; // Track if parameters are expanded or collapsed
    private JButton toggleParametersButton; // Button to toggle parameters visibility

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

        // Initialize parameter storage
        initializeParameterMap();

        // Create control panel with GridBagLayout for better organization
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;

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
        
        // Add collapsible parameters panel header
        JPanel parameterHeaderPanel = new JPanel(new BorderLayout());
        parameterHeaderPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        
        // Add toggle button
        toggleParametersButton = new JButton("▼"); // Down arrow for collapse
        toggleParametersButton.setToolTipText("Collapse parameters");
        toggleParametersButton.setPreferredSize(new Dimension(50, 20));
        toggleParametersButton.addActionListener(e -> toggleParameterPanelVisibility());
        parameterHeaderPanel.add(toggleParametersButton, BorderLayout.EAST);
        
        controlPanel.add(parameterHeaderPanel, gbc);
        gbc.gridy++;
        
        // Add parameter panel (initially empty)
        parameterPanel = new JPanel();
        parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
        controlPanel.add(parameterPanel, gbc);
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
            createBasicParticle(x, y));

        registerParticleType("Gravity Particle", (x, y) ->
            createGravityParticle(x, y, true));
            
        registerParticleType("Anti-Gravity Particle", (x, y) ->
            createGravityParticle(x, y, false));

        registerParticleType("Demo Particle", (x, y) ->
            new DemoParticle(world, x, y, 0, 0));
            
        // Fix the case to match the parameter map key
        registerParticleType("Ghost Particle", (x, y) ->
            createGhostParticle(x, y));
        // Create buttons for each particle type
        for (String particleType : particleParameters.keySet()) {
            JButton button = new JButton(particleType);
            button.addActionListener(e -> {
                selectedParticleType = particleType;
                selectedFactory = particleFactories.get(particleType);
                
                // Update button highlights
                for (Component c : particleButtonsPanel.getComponents()) {
                    if (c instanceof JButton) {
                        c.setBackground(null);
                    }
                }
                button.setBackground(Color.LIGHT_GRAY);
                
                // Update parameter panel for this particle type
                updateParameterPanel(particleType);
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

    /**
     * Initialize parameter map with default values for each particle type
     */
    private void initializeParameterMap() {
        particleParameters = new HashMap<>();
        
        // Basic Particle parameters
        Map<String, Double> basicParams = new HashMap<>();
        basicParams.put("velocityX", 0.0);
        basicParams.put("velocityY", 0.0);
        basicParams.put("mass", 1.0);
        basicParams.put("radius", 0.5);
        particleParameters.put("Basic Particle", basicParams);
        
        // Gravity Particle parameters
        Map<String, Double> gravityParams = new HashMap<>();
        gravityParams.put("velocityX", 0.0);
        gravityParams.put("velocityY", 0.0);
        gravityParams.put("mass", 1.0);
        gravityParams.put("radius", 0.5);
        gravityParams.put("range", 100.0);
        gravityParams.put("strength", 1.0);
        particleParameters.put("Gravity Particle", gravityParams);
        
        // Anti-Gravity Particle parameters
        Map<String, Double> antiGravityParams = new HashMap<>();
        antiGravityParams.put("velocityX", 0.0);
        antiGravityParams.put("velocityY", 0.0);
        antiGravityParams.put("mass", 1.0);
        antiGravityParams.put("radius", 0.5);
        antiGravityParams.put("range", 100.0);
        antiGravityParams.put("strength", -1.0); // Negative for repulsion
        particleParameters.put("Anti-Gravity Particle", antiGravityParams);
        
        // Ghost Particle parameters
        Map<String, Double> ghostParams = new HashMap<>();
        ghostParams.put("velocityX", 0.0);
        ghostParams.put("velocityY", 0.0);
        ghostParams.put("mass", 1.0);
        ghostParams.put("radius", 0.5);
        particleParameters.put("Ghost Particle", ghostParams);
    }
    
    /**
     * Update parameter panel with sliders for the selected particle type
     */
    private void updateParameterPanel(String particleType) {
        parameterPanel.removeAll();
        
        // Don't populate if panel is collapsed
        if (!parameterPanelExpanded) {
            return;
        }
        
        Map<String, Double> params = particleParameters.get(particleType);
        if (params == null) return;
        
        // Common parameters for all particle types
        addSlider(parameterPanel, "Velocity X", params, "velocityX", -10.0, 10.0);
        addSlider(parameterPanel, "Velocity Y", params, "velocityY", -10.0, 10.0);
        addSlider(parameterPanel, "Mass", params, "mass", 0.1, 10.0);
        addSlider(parameterPanel, "Radius", params, "radius", 0.1, 5.0);
        
        // Specific parameters for gravity particles
        if (particleType.contains("Gravity")) {
            addSlider(parameterPanel, "Range", params, "range", 10.0, 300.0);
            
            // Different range for regular vs anti-gravity
            double strengthMin = particleType.contains("Anti") ? -10.0 : 0.1;
            double strengthMax = particleType.contains("Anti") ? -0.1 : 10.0;
            addSlider(parameterPanel, "Strength", params, "strength", strengthMin, strengthMax);
        }
        
        parameterPanel.revalidate();
        parameterPanel.repaint();
    }
    
    /**
     * Helper to add a labeled slider for a parameter
     */
    private void addSlider(JPanel panel, String label, Map<String, Double> params, String paramName, 
                          double min, double max) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        JLabel nameLabel = new JLabel(label + ": ");
        
        // Convert parameter value to slider range (0-100)
        int value = (int)((params.get(paramName) - min) / (max - min) * 100);
        
        JSlider slider = new JSlider(0, 100, value);
        JLabel valueLabel = new JLabel(String.format("%.2f", params.get(paramName)));
        valueLabel.setPreferredSize(new Dimension(50, 20));
        
        slider.addChangeListener(e -> {
            // Convert slider value back to parameter range
            double newValue = min + (slider.getValue() / 100.0) * (max - min);
            params.put(paramName, newValue);
            valueLabel.setText(String.format("%.2f", newValue));
        });
        
        row.add(nameLabel, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(valueLabel, BorderLayout.EAST);
        panel.add(row);
    }
    
    /**
     * Create particles using the current parameter values
     */
    private BasicParticle createBasicParticle(double x, double y) {
        Map<String, Double> params = particleParameters.get("Basic Particle");
        BasicParticle particle = new BasicParticle(
            world,
            x,
            y, 
            params.get("velocityX"),
            params.get("velocityY")
        );
        particle.setMass(params.get("mass"));
        particle.setRadius(params.get("radius"));
        return particle;
    }
    
    private GravityParticle createGravityParticle(double x, double y, boolean isAttractive) {
        String type = isAttractive ? "Gravity Particle" : "Anti-Gravity Particle";
        Map<String, Double> params = particleParameters.get(type);
        GravityParticle particle = new GravityParticle(
            world,
            x,
            y, 
            params.get("velocityX"),
            params.get("velocityY"),
            params.get("range").floatValue(),
            params.get("strength").floatValue()
        );
        particle.setMass(params.get("mass"));
        particle.setRadius(params.get("radius"));
        return particle;
    }
    
    private GhostParticle createGhostParticle(double x, double y) {
        Map<String, Double> params = particleParameters.get("Ghost Particle");
        GhostParticle particle = new GhostParticle(
            world,
            x,
            y, 
            params.get("velocityX"),
            params.get("velocityY")
        );
        particle.setMass(params.get("mass"));
        particle.setRadius(params.get("radius"));
        return particle;
    }

    /**
     * Toggle the visibility of the parameter panel
     */
    private void toggleParameterPanelVisibility() {
        parameterPanelExpanded = !parameterPanelExpanded;
        
        if (parameterPanelExpanded) {
            // Expand panel
            parameterPanel.setVisible(true);
            toggleParametersButton.setText("▼"); // Down arrow
            toggleParametersButton.setToolTipText("Collapse parameters");
            
            // If a particle type is selected, update the panel
            if (selectedParticleType != null) {
                updateParameterPanel(selectedParticleType);
            }
        } else {
            // Collapse panel
            parameterPanel.setVisible(false);
            toggleParametersButton.setText("▲"); // Up arrow
            toggleParametersButton.setToolTipText("Expand parameters");
        }
        
        // Force layout update
        revalidate();
        repaint();
    }

    // Clean up resources
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        ParticleThreadPool.shutdown();
    }
}
