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
import cellularfractals.particles.effects.MouseGravityEffect;
import cellularfractals.particles.effects.GroundGravityEffect;

public class MyPanel extends JPanel {
    // Existing fields
    private World world;
    private CustomCanvas canvas;
    private Timer updateTimer;
    private Map<String, BiFunction<Double, Double, Particle>> particleFactories = new HashMap<>();
    private BiFunction<Double, Double, Particle> selectedFactory = null;
    private static final long SPAWN_COOLDOWN = 100;
    private long lastSpawnTime = 0;
    private boolean showVectorArrows = true;
    private JPanel parameterPanel;
    private Map<String, Map<String, Double>> particleParameters;
    private String selectedParticleType = null;
    private boolean parameterPanelExpanded = true;
    private JButton toggleParametersButton;

    // Fields for visibility control
    private Map<String, Boolean> particleTypeVisibility = new HashMap<>();
    private JPanel visibilityPanel;
    private boolean visibilityPanelExpanded = true;
    
    // Mouse gravity effect fields
    private MouseGravityEffect mouseGravityEffect;
    private boolean mouseGravityEnabled = false;
    private boolean mouseGravityAttractive = true;
    private float mouseGravityStrength = 1.0f;
    private float mouseGravityRange = 100f;
    
    // Ground gravity effect
    private GroundGravityEffect groundGravityEffect;
    private boolean groundGravityEnabled = false;
    private float groundGravityStrength = 0.1f;

    // Add these fields at the top with other fields
    private static volatile double velocityMultiplier = 1.0;
    private static final double MIN_VEL_MULT = 0.1;
    private static final double MAX_VEL_MULT = 10.0;

    // Add a static getter for the velocity multiplier
    public static double getVelocityMultiplier() {
        return velocityMultiplier;
    }

    public MyPanel(World world) {
        this.world = world;
        setLayout(new BorderLayout());

        // Create mouse gravity effect
        mouseGravityEffect = new MouseGravityEffect(mouseGravityRange, mouseGravityStrength);
        mouseGravityEffect.setEnabled(false);
        world.effectModifierIndex.addGlobalEffect(mouseGravityEffect);
        
        // Create ground gravity effect
        groundGravityEffect = new GroundGravityEffect(groundGravityStrength);
        groundGravityEffect.setEnabled(false);
        world.effectModifierIndex.addGlobalEffect(groundGravityEffect);

        // Create custom canvas
        canvas = new CustomCanvas();
        canvas.setPreferredSize(new Dimension(400, 400));
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleMouseClick(e); }
            @Override public void mouseReleased(MouseEvent e) { handleMouseClick(e); }
            @Override public void mouseClicked(MouseEvent e) { handleMouseClick(e); }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) { handleMouseClick(e); }
            @Override public void mouseMoved(MouseEvent e) { updateMousePosition(e); }
        });
        add(canvas, BorderLayout.CENTER);

        // Initialize maps
        initializeParameterMap();
        initializeVisibilityMap();

        // Build control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2,2,2,2); gbc.weightx = 1.0;

        controlPanel.add(new JLabel("Grid Size: " + world.getWidth() + " x " + world.getHeight()), gbc);
        gbc.gridy++;
        JLabel particleCountLabel = new JLabel("Particles: " + world.getParticleCount());
        controlPanel.add(particleCountLabel, gbc);
        gbc.gridy++;

        // Vector arrows
        JCheckBox vectorArrowsToggle = new JCheckBox("Show Vector Arrows", showVectorArrows);
        vectorArrowsToggle.addActionListener(e -> { showVectorArrows = vectorArrowsToggle.isSelected(); canvas.repaint(); });
        controlPanel.add(vectorArrowsToggle, gbc);
        gbc.gridy++;
        
        // Mouse gravity options panel
        JPanel mouseGravityPanel = new JPanel();
        mouseGravityPanel.setBorder(BorderFactory.createTitledBorder("Mouse Gravity"));
        mouseGravityPanel.setLayout(new BoxLayout(mouseGravityPanel, BoxLayout.Y_AXIS));
        
        // Mouse gravity toggle
        JCheckBox mouseGravityToggle = new JCheckBox("Enable Mouse Gravity", mouseGravityEnabled);
        mouseGravityToggle.addActionListener(e -> {
            mouseGravityEnabled = mouseGravityToggle.isSelected();
            mouseGravityEffect.setEnabled(mouseGravityEnabled);
        });
        mouseGravityPanel.add(mouseGravityToggle);
        
        // Mouse gravity type radio buttons
        JPanel mouseTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup mouseTypeGroup = new ButtonGroup();
        JRadioButton attractiveButton = new JRadioButton("Attractive", mouseGravityAttractive);
        JRadioButton repulsiveButton = new JRadioButton("Repulsive", !mouseGravityAttractive);
        
        attractiveButton.addActionListener(e -> {
            mouseGravityAttractive = true;
            mouseGravityEffect.setStrength(Math.abs(mouseGravityStrength));
        });
        repulsiveButton.addActionListener(e -> {
            mouseGravityAttractive = false;
            mouseGravityEffect.setStrength(-Math.abs(mouseGravityStrength));
        });
        
        mouseTypeGroup.add(attractiveButton);
        mouseTypeGroup.add(repulsiveButton);
        mouseTypePanel.add(attractiveButton);
        mouseTypePanel.add(repulsiveButton);
        mouseGravityPanel.add(mouseTypePanel);
        
        // Mouse gravity strength slider
        JPanel mouseStrengthPanel = new JPanel(new BorderLayout(5, 0));
        mouseStrengthPanel.add(new JLabel("Strength:"), BorderLayout.WEST);
        JSlider mouseStrengthSlider = new JSlider(1, 200, (int)(mouseGravityStrength * 100));
        JLabel mouseStrengthValueLabel = new JLabel(String.format("%.2f", mouseGravityStrength));
        mouseStrengthSlider.addChangeListener(e -> {
            mouseGravityStrength = mouseStrengthSlider.getValue() / 100.0f;
            mouseStrengthValueLabel.setText(String.format("%.2f", mouseGravityStrength));
            mouseGravityEffect.setStrength(mouseGravityAttractive ? mouseGravityStrength : -mouseGravityStrength);
        });
        mouseStrengthPanel.add(mouseStrengthSlider, BorderLayout.CENTER);
        mouseStrengthPanel.add(mouseStrengthValueLabel, BorderLayout.EAST);
        mouseGravityPanel.add(mouseStrengthPanel);
        
        // Mouse gravity range slider
        JPanel mouseRangePanel = new JPanel(new BorderLayout(5, 0));
        mouseRangePanel.add(new JLabel("Range:"), BorderLayout.WEST);
        JSlider mouseRangeSlider = new JSlider(10, 300, (int)mouseGravityRange);
        JLabel mouseRangeValueLabel = new JLabel(String.format("%.0f", mouseGravityRange));
        mouseRangeSlider.addChangeListener(e -> {
            mouseGravityRange = mouseRangeSlider.getValue();
            mouseRangeValueLabel.setText(String.format("%.0f", mouseGravityRange));
            mouseGravityEffect.setRange(mouseGravityRange);
        });
        mouseRangePanel.add(mouseRangeSlider, BorderLayout.CENTER);
        mouseRangePanel.add(mouseRangeValueLabel, BorderLayout.EAST);
        mouseGravityPanel.add(mouseRangePanel);
        
        controlPanel.add(mouseGravityPanel, gbc);
        gbc.gridy++;
        
        // Ground gravity panel
        JPanel groundGravityPanel = new JPanel();
        groundGravityPanel.setBorder(BorderFactory.createTitledBorder("Ground Gravity"));
        groundGravityPanel.setLayout(new BoxLayout(groundGravityPanel, BoxLayout.Y_AXIS));
        
        // Ground gravity toggle
        JCheckBox groundGravityToggle = new JCheckBox("Enable Ground Gravity", groundGravityEnabled);
        groundGravityToggle.addActionListener(e -> {
            groundGravityEnabled = groundGravityToggle.isSelected();
            groundGravityEffect.setEnabled(groundGravityEnabled);
        });
        groundGravityPanel.add(groundGravityToggle);
        
        // Ground gravity strength slider
        JPanel groundStrengthPanel = new JPanel(new BorderLayout(5, 0));
        groundStrengthPanel.add(new JLabel("Strength:"), BorderLayout.WEST);
        JSlider groundStrengthSlider = new JSlider(1, 100, (int)(groundGravityStrength * 100));
        JLabel groundStrengthValueLabel = new JLabel(String.format("%.2f", groundGravityStrength));
        groundStrengthSlider.addChangeListener(e -> {
            groundGravityStrength = groundStrengthSlider.getValue() / 100.0f;
            groundStrengthValueLabel.setText(String.format("%.2f", groundGravityStrength));
            groundGravityEffect.setStrength(groundGravityStrength);
        });
        groundStrengthPanel.add(groundStrengthSlider, BorderLayout.CENTER);
        groundStrengthPanel.add(groundStrengthValueLabel, BorderLayout.EAST);
        groundGravityPanel.add(groundStrengthPanel);
        
        controlPanel.add(groundGravityPanel, gbc);
        gbc.gridy++;

        // Add velocity multiplier slider after the vector arrows toggle and before visibility panel
        JPanel velocityMultPanel = new JPanel(new BorderLayout());
        velocityMultPanel.setBorder(BorderFactory.createTitledBorder("Velocity Multiplier"));
        
        // Create logarithmic slider (0 to 100 represents log scale from 0.1 to 10)
        JSlider velocityMultSlider = new JSlider(0, 100, 50); // 50 = log(1.0) position
        JLabel velocityMultLabel = new JLabel(String.format("%.2fx", velocityMultiplier));
        velocityMultLabel.setPreferredSize(new Dimension(50, 20));
        
        velocityMultSlider.addChangeListener(e -> {
            // Convert linear slider value to logarithmic scale
            double sliderPct = velocityMultSlider.getValue() / 100.0;
            velocityMultiplier = MIN_VEL_MULT * Math.pow(MAX_VEL_MULT/MIN_VEL_MULT, sliderPct);
            velocityMultLabel.setText(String.format("%.2fx", velocityMultiplier));
            canvas.repaint();
        });
        
        velocityMultPanel.add(velocityMultSlider, BorderLayout.CENTER);
        velocityMultPanel.add(velocityMultLabel, BorderLayout.EAST);
        
        controlPanel.add(velocityMultPanel, gbc);
        gbc.gridy++;

        // Visibility panel header
        JPanel visibilityHeaderPanel = new JPanel(new BorderLayout());
        visibilityHeaderPanel.setBorder(BorderFactory.createTitledBorder("Particle Visibility"));
        JButton toggleVisibilityButton = new JButton("▼");
        toggleVisibilityButton.setToolTipText("Collapse visibility options");
        toggleVisibilityButton.setPreferredSize(new Dimension(50,20));
        toggleVisibilityButton.addActionListener(e -> toggleVisibilityPanel(toggleVisibilityButton));
        visibilityHeaderPanel.add(toggleVisibilityButton, BorderLayout.EAST);
        controlPanel.add(visibilityHeaderPanel, gbc);
        gbc.gridy++;

        // Visibility panel
        visibilityPanel = new JPanel();
        visibilityPanel.setLayout(new BoxLayout(visibilityPanel, BoxLayout.Y_AXIS));
        updateVisibilityPanel();
        controlPanel.add(visibilityPanel, gbc);
        gbc.gridy++;

        // Parameter panel header
        JPanel parameterHeaderPanel = new JPanel(new BorderLayout());
        parameterHeaderPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        toggleParametersButton = new JButton("▼");
        toggleParametersButton.setToolTipText("Collapse parameters");
        toggleParametersButton.setPreferredSize(new Dimension(50,20));
        toggleParametersButton.addActionListener(e -> toggleParameterPanelVisibility());
        parameterHeaderPanel.add(toggleParametersButton, BorderLayout.EAST);
        controlPanel.add(parameterHeaderPanel, gbc);
        gbc.gridy++;

        // Parameter panel
        parameterPanel = new JPanel();
        parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
        controlPanel.add(parameterPanel, gbc);
        gbc.gridy++;

        // Particle spawn buttons
        JPanel particleButtonsPanel = new JPanel(new GridLayout(0, 1, 2, 2));
        particleButtonsPanel.setBorder(BorderFactory.createTitledBorder("Spawn Particles"));
        controlPanel.add(particleButtonsPanel, gbc);
        gbc.gridy++;

        // Register particle types
        registerParticleType("Basic Particle", (x, y) -> createBasicParticle(x, y));
        registerParticleType("Gravity Particle", (x, y) -> createGravityParticle(x, y, true));
        registerParticleType("Anti-Gravity Particle", (x, y) -> createGravityParticle(x, y, false));
        registerParticleType("Demo Particle", (x, y) -> new DemoParticle(world, x, y, 0, 0));
        registerParticleType("Ghost Particle", (x, y) -> createGhostParticle(x, y));

        for (String type : particleParameters.keySet()) {
            JButton btn = new JButton(type);
            btn.addActionListener(e -> {
                selectedParticleType = type;
                selectedFactory = particleFactories.get(type);
                for (Component c : particleButtonsPanel.getComponents()) {
                    if (c instanceof JButton) { c.setBackground(null); }
                }
                btn.setBackground(Color.LIGHT_GRAY);
                updateParameterPanel(type);
            });
            particleButtonsPanel.add(btn);
        }

        // Reset button
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            world.clear();
            selectedFactory = null;
            for (Component c : particleButtonsPanel.getComponents()) {
                if (c instanceof JButton) { c.setBackground(null); }
            }
            canvas.repaint();
        });
        controlPanel.add(resetButton, gbc);

        add(controlPanel, BorderLayout.EAST);

        // Timer for animation
        updateTimer = new Timer(100, e -> { // Update UI every 100ms instead of 16ms
            particleCountLabel.setText("Particles: " + world.getParticleCount());
            canvas.repaint();
        });
        updateTimer.start();
    }

    /**
     * Update the mouse position for the mouse gravity effect
     */
    private void updateMousePosition(MouseEvent e) {
        if (!mouseGravityEnabled) return;
        
        // Convert screen coordinates to world coordinates
        double worldX = Math.max(0, Math.min(world.getWidth(),
            (e.getX() * world.getWidth()) / canvas.getWidth()));
        double worldY = Math.max(0, Math.min(world.getHeight(),
            (e.getY() * world.getHeight()) / canvas.getHeight()));
            
        mouseGravityEffect.setPosition(worldX, worldY);
    }

    // Visibility methods
    private void initializeVisibilityMap() {
        particleTypeVisibility.put("Basic Particle", true);
        particleTypeVisibility.put("Gravity Particle", true);
        particleTypeVisibility.put("Anti-Gravity Particle", true);
        particleTypeVisibility.put("Demo Particle", true);
        particleTypeVisibility.put("Ghost Particle", true);
    }

    private void updateVisibilityPanel() {
        visibilityPanel.removeAll();
        for (String type : particleTypeVisibility.keySet()) {
            JCheckBox cb = new JCheckBox(type, particleTypeVisibility.get(type));
            cb.addActionListener(e -> { particleTypeVisibility.put(type, cb.isSelected()); canvas.repaint(); });
            visibilityPanel.add(cb);
        }
        visibilityPanel.revalidate();
        visibilityPanel.repaint();
    }

    private void toggleVisibilityPanel(JButton toggleButton) {
        visibilityPanelExpanded = !visibilityPanelExpanded;
        visibilityPanel.setVisible(visibilityPanelExpanded);
        toggleButton.setText(visibilityPanelExpanded ? "▼" : "▲");
        toggleButton.setToolTipText(visibilityPanelExpanded ? "Collapse visibility options" : "Expand visibility options");
        revalidate(); repaint();
    }

    // Parameter panel methods
    private void toggleParameterPanelVisibility() {
        parameterPanelExpanded = !parameterPanelExpanded;
        parameterPanel.setVisible(parameterPanelExpanded);
        toggleParametersButton.setText(parameterPanelExpanded ? "▼" : "▲");
        toggleParametersButton.setToolTipText(parameterPanelExpanded ? "Collapse parameters" : "Expand parameters");
        if (parameterPanelExpanded && selectedParticleType != null) {
            updateParameterPanel(selectedParticleType);
        }
        revalidate(); repaint();
    }

    private void updateParameterPanel(String particleType) {
        parameterPanel.removeAll();
        if (!parameterPanelExpanded) return;
        Map<String, Double> params = particleParameters.get(particleType);
        if (params == null) return;
        addSlider(parameterPanel, "Velocity X", params, "velocityX", -10.0, 10.0);
        addSlider(parameterPanel, "Velocity Y", params, "velocityY", -10.0, 10.0);
        addSlider(parameterPanel, "Mass", params, "mass", 0.1, 10.0);
        addSlider(parameterPanel, "Radius", params, "radius", 0.1, 5.0);
        if (particleType.contains("Gravity")) {
            addSlider(parameterPanel, "Range", params, "range", 10.0, 300.0);
            double strengthMin = particleType.contains("Anti") ? -1.0 : 0.001;
            double strengthMax = particleType.contains("Anti") ? -0.001 : 1;
            addSlider(parameterPanel, "Strength", params, "strength", strengthMin, strengthMax);
        }
        parameterPanel.revalidate();
        parameterPanel.repaint();
    }

    private void addSlider(JPanel panel, String label, Map<String, Double> params, String paramName, double min, double max) {
        JPanel row = new JPanel(new BorderLayout(5,0));
        JLabel nameLabel = new JLabel(label + ": ");
        int value = (int)((params.get(paramName) - min) / (max - min) * 100);
        JSlider slider = new JSlider(0, 100, value);
        JLabel valueLabel = new JLabel(String.format("%.2f", params.get(paramName)));
        valueLabel.setPreferredSize(new Dimension(50,20));
        slider.addChangeListener(e -> {
            double newValue = min + (slider.getValue()/100.0)*(max-min);
            params.put(paramName, newValue);
            valueLabel.setText(String.format("%.2f", newValue));
        });
        row.add(nameLabel, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(valueLabel, BorderLayout.EAST);
        panel.add(row);
    }

    // Particle factories and creation
    public void registerParticleType(String name, BiFunction<Double, Double, Particle> factory) {
        particleFactories.put(name, factory);
    }

    private BasicParticle createBasicParticle(double x, double y) {
        Map<String, Double> params = particleParameters.get("Basic Particle");
        BasicParticle p = new BasicParticle(world, x, y, 
            params.get("velocityX"), 
            params.get("velocityY"));
        p.setMass(params.get("mass")); p.setRadius(params.get("radius"));
        return p;
    }

    private GravityParticle createGravityParticle(double x, double y, boolean isAttractive) {
        String type = isAttractive ? "Gravity Particle" : "Anti-Gravity Particle";
        Map<String, Double> params = particleParameters.get(type);
        GravityParticle p = new GravityParticle(world, x, y, 
            params.get("velocityX"), 
            params.get("velocityY"),
            params.get("range").floatValue(), 
            params.get("strength").floatValue());
        p.setMass(params.get("mass")); p.setRadius(params.get("radius"));
        return p;
    }

    private GhostParticle createGhostParticle(double x, double y) {
        Map<String, Double> params = particleParameters.get("Ghost Particle");
        GhostParticle p = new GhostParticle(world, x, y, 
            params.get("velocityX"), 
            params.get("velocityY"));
        p.setMass(params.get("mass")); p.setRadius(params.get("radius"));
        return p;
    }

    // Parameter map initialization
    private void initializeParameterMap() {
        particleParameters = new HashMap<>();
        Map<String, Double> basicParams = new HashMap<>();
        basicParams.put("velocityX", 0.0); basicParams.put("velocityY", 0.0);
        basicParams.put("mass", 1.0); basicParams.put("radius", 0.5);
        particleParameters.put("Basic Particle", basicParams);

        Map<String, Double> gravityParams = new HashMap<>();
        gravityParams.put("velocityX", 0.0); gravityParams.put("velocityY", 0.0);
        gravityParams.put("mass", 1.0); gravityParams.put("radius", 0.5);
        gravityParams.put("range", 100.0); gravityParams.put("strength", 1.0);
        particleParameters.put("Gravity Particle", gravityParams);

        Map<String, Double> antiGravityParams = new HashMap<>();
        antiGravityParams.put("velocityX", 0.0); antiGravityParams.put("velocityY", 0.0);
        antiGravityParams.put("mass", 1.0); antiGravityParams.put("radius", 0.5);
        antiGravityParams.put("range", 100.0); antiGravityParams.put("strength", -1.0);
        particleParameters.put("Anti-Gravity Particle", antiGravityParams);

        Map<String, Double> ghostParams = new HashMap<>();
        ghostParams.put("velocityX", 0.0); ghostParams.put("velocityY", 0.0);
        ghostParams.put("mass", 1.0); ghostParams.put("radius", 0.5);
        particleParameters.put("Ghost Particle", ghostParams);

        Map<String, Double> demoParams = new HashMap<>();
        demoParams.put("velocityX", 0.0); demoParams.put("velocityY", 0.0);
        demoParams.put("mass", 1.0); demoParams.put("radius", 0.5);
        particleParameters.put("Demo Particle", demoParams);
    }

    // Custom canvas with proper visibility filtering
    private class CustomCanvas extends JPanel {
        public CustomCanvas() { setBackground(Color.BLACK); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth(), height = getHeight();
            // Draw grid lines
            g2d.setColor(new Color(30,30,30));
            for (double x = 0; x < world.getWidth(); x += 10.0) {
                g2d.drawLine((int)(x*width/world.getWidth()), 0, (int)(x*width/world.getWidth()), height);
            }
            for (double y = 0; y < world.getHeight(); y += 10.0) {
                g2d.drawLine(0, (int)(y*height/world.getHeight()), width, (int)(y*height/world.getHeight()));
            }
            // Draw particles using effective type-checking
            Rectangle clipBounds = g2d.getClipBounds();
            for (Particle particle : world.getParticles()) {
                String typeForVisibility;
                if (particle instanceof GravityParticle) {
                    typeForVisibility = ((GravityParticle) particle).getType();
                } else {
                    typeForVisibility = particle.getClass().getSimpleName().replaceAll("([A-Z])", " $1").trim();
                }
                Boolean vis = particleTypeVisibility.get(typeForVisibility);
                if (vis != null && !vis) continue;

                int screenX = (int)(particle.getX()*width/world.getWidth());
                int screenY = (int)(particle.getY()*height/world.getHeight());
                if (screenX < clipBounds.x - 50 || screenX > clipBounds.x + clipBounds.width + 50 ||
                    screenY < clipBounds.y - 50 || screenY > clipBounds.y + clipBounds.height + 50) {
                    continue;
                }
                int screenRadius = (int)(particle.getRadius()*width/world.getWidth());
                g2d.setColor(particle.cosmeticSettings != null ? particle.cosmeticSettings.color : Color.WHITE);
                g2d.fillOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2, screenRadius * 2);
                if (showVectorArrows) {
                    g2d.setColor(particle.cosmeticSettings != null ? particle.cosmeticSettings.trailColor : Color.CYAN);
                    int velX = (int)(particle.getDx() * 20 * velocityMultiplier);
                    int velY = (int)(particle.getDy() * 20 * velocityMultiplier);
                    g2d.drawLine(screenX, screenY, screenX + velX, screenY + velY);
                }
            }
            
            // Draw mouse gravity indicator when enabled
            if (mouseGravityEnabled) {
                Point mousePoint = getMousePosition();
                if (mousePoint != null) {
                    int indicatorSize = (int)(mouseGravityRange * width / world.getWidth());
                    g2d.setColor(mouseGravityAttractive ? 
                        new Color(175, 0, 255, 50) : new Color(255, 0, 175, 50));
                    g2d.fillOval(
                        mousePoint.x - indicatorSize/2, 
                        mousePoint.y - indicatorSize/2, 
                        indicatorSize, 
                        indicatorSize
                    );
                    
                    // Draw a small center point
                    g2d.setColor(mouseGravityAttractive ? 
                        new Color(175, 0, 255) : new Color(255, 0, 175));
                    g2d.fillOval(
                        mousePoint.x - 5, 
                        mousePoint.y - 5, 
                        10, 
                        10
                    );
                }
            }
        }
    }

    // Dispose resources
    public void dispose() {
        if (updateTimer != null) { updateTimer.stop(); }
        ParticleThreadPool.shutdown();
    }

    /**
     * Handle mouse click events to spawn new particles
     */
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
}
