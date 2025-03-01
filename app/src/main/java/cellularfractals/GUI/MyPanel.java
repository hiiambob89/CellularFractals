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
    private long spawnCooldown = 100;
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

    // Spawn on drag field
    private boolean spawnOnDrag = false;

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
            @Override public void mouseDragged(MouseEvent e) {
                if (spawnOnDrag) {
                    handleMouseClick(e);
                }
            }
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
        JSlider groundStrengthSlider = new JSlider(0, 10, (int)(groundGravityStrength));
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
        registerParticleType("Exploding Particle", (x, y) -> createExplodingParticle(x, y));

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

        // Slider for controlling spawn cooldown
        JPanel spawnRatePanel = new JPanel(new BorderLayout(5,0));
        spawnRatePanel.setBorder(BorderFactory.createTitledBorder("Particle Spawn Rate"));
        JSlider spawnRateSlider = new JSlider(JSlider.HORIZONTAL, 10, 1000, (int)spawnCooldown);
        JLabel spawnRateLabel = new JLabel(String.format("%d ms", spawnCooldown));
        spawnRateSlider.addChangeListener(e -> {
            spawnCooldown = spawnRateSlider.getValue();
            spawnRateLabel.setText(String.format("%d ms", spawnCooldown));
        });
        spawnRatePanel.add(spawnRateSlider, BorderLayout.CENTER);
        spawnRatePanel.add(spawnRateLabel, BorderLayout.EAST);
        controlPanel.add(spawnRatePanel, gbc);
        gbc.gridy++;

        // Spawn on drag toggle
        JCheckBox spawnOnDragToggle = new JCheckBox("Spawn on Drag", spawnOnDrag);
        spawnOnDragToggle.addActionListener(e -> {
            spawnOnDrag = spawnOnDragToggle.isSelected();
        });
        controlPanel.add(spawnOnDragToggle, gbc);
        gbc.gridy++;

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
        updateTimer = new Timer(16, e -> {
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

        // Calculate size and offsets for square aspect ratio
        int size = Math.min(canvas.getWidth(), canvas.getHeight());
        int xOffset = (canvas.getWidth() - size) / 2;
        int yOffset = (canvas.getHeight() - size) / 2;

        // Convert screen coordinates to world coordinates
        double worldX = Math.max(0, Math.min(world.getWidth(),
            ((e.getX() - xOffset) * world.getWidth()) / size));
        double worldY = Math.max(0, Math.min(world.getHeight(),
            ((e.getY() - yOffset) * world.getHeight()) / size));

        mouseGravityEffect.setPosition(worldX, worldY);
    }

    // Visibility methods
    private void initializeVisibilityMap() {
        particleTypeVisibility.put("Basic Particle", true);
        particleTypeVisibility.put("Gravity Particle", true);
        particleTypeVisibility.put("Anti-Gravity Particle", true);
        particleTypeVisibility.put("Demo Particle", true);
        particleTypeVisibility.put("Ghost Particle", true);
        particleTypeVisibility.put("Exploding Particle", true);
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
            addSlider(parameterPanel, "Range", params, "range", 1.0, 300.0);
            double strengthMin = particleType.contains("Anti") ? -1.0 : 0.001;
            double strengthMax = particleType.contains("Anti") ? -0.001 : 1;
            addSlider(parameterPanel, "Strength", params, "strength", strengthMin, strengthMax);
        } else if (particleType.equals("Exploding Particle")) {
            addSlider(parameterPanel, "Explosion Radius", params, "explosionRadius", 1.0, 50.0);
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
        BasicParticle p = new BasicParticle(world, x, y, params.get("velocityX"), params.get("velocityY"));
        p.setMass(params.get("mass")); p.setRadius(params.get("radius"));
        return p;
    }

    private GravityParticle createGravityParticle(double x, double y, boolean isAttractive) {
        String type = isAttractive ? "Gravity Particle" : "Anti-Gravity Particle";
        Map<String, Double> params = particleParameters.get(type);
        GravityParticle p = new GravityParticle(world, x, y, params.get("velocityX"), params.get("velocityY"), params.get("range").floatValue(), params.get("strength").floatValue());
        p.setMass(params.get("mass")); p.setRadius(params.get("radius"));
        return p;
    }

    private GhostParticle createGhostParticle(double x, double y) {
        Map<String, Double> params = particleParameters.get("Ghost Particle");
        GhostParticle p = new GhostParticle(world, x, y, params.get("velocityX"), params.get("velocityY"));
        p.setMass(params.get("mass")); p.setRadius(params.get("radius"));
        return p;
    }

    private ExplodingParticle createExplodingParticle(double x, double y) {
        Map<String, Double> params = particleParameters.get("Exploding Particle");
        ExplodingParticle p = new ExplodingParticle(
            world,
            x,
            y,
            params.get("velocityX"),
            params.get("velocityY"),
            params.get("explosionRadius").intValue()
        );
        p.setMass(params.get("mass"));
        p.setRadius(params.get("radius"));
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
        gravityParams.put("range", 100.0); gravityParams.put("strength", .001);
        particleParameters.put("Gravity Particle", gravityParams);

        Map<String, Double> antiGravityParams = new HashMap<>();
        antiGravityParams.put("velocityX", 0.0); antiGravityParams.put("velocityY", 0.0);
        antiGravityParams.put("mass", 1.0); antiGravityParams.put("radius", 0.5);
        antiGravityParams.put("range", 100.0); antiGravityParams.put("strength", -0.001);
        particleParameters.put("Anti-Gravity Particle", antiGravityParams);

        Map<String, Double> ghostParams = new HashMap<>();
        ghostParams.put("velocityX", 0.0); ghostParams.put("velocityY", 0.0);
        ghostParams.put("mass", 1.0); ghostParams.put("radius", 0.5);
        particleParameters.put("Ghost Particle", ghostParams);

        Map<String, Double> demoParams = new HashMap<>();
        demoParams.put("velocityX", 0.0); demoParams.put("velocityY", 0.0);
        demoParams.put("mass", 1.0); demoParams.put("radius", 0.5);
        particleParameters.put("Demo Particle", demoParams);

        // Add exploding particle parameters
        Map<String, Double> explodingParams = new HashMap<>();
        explodingParams.put("velocityX", 0.0);
        explodingParams.put("velocityY", 0.0);
        explodingParams.put("mass", 1.0);
        explodingParams.put("radius", 0.5);
        explodingParams.put("explosionRadius", 10.0);
        particleParameters.put("Exploding Particle", explodingParams);
    }

    // Custom canvas with proper visibility filtering
    private class CustomCanvas extends JPanel {
        public CustomCanvas() {
            setBackground(Color.BLACK);
        }

        @Override
        public Dimension getPreferredSize() {
            // Get available height
            Container parent = getParent();
            int availableHeight = parent != null ? parent.getHeight() : 400;
            // Make it square based on height
            return new Dimension(availableHeight, availableHeight);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Ensure square rendering area
            int size = Math.min(getWidth(), getHeight());
            int xOffset = (getWidth() - size) / 2;
            int yOffset = (getHeight() - size) / 2;

            // Draw grid lines
            g2d.setColor(new Color(30,30,30));
            for (double x = 0; x < world.getWidth(); x += 10.0) {
                int xPos = (int)(x*size/world.getWidth()) + xOffset;
                g2d.drawLine(xPos, yOffset, xPos, size + yOffset);
            }
            for (double y = 0; y < world.getHeight(); y += 10.0) {
                int yPos = (int)(y*size/world.getHeight()) + yOffset;
                g2d.drawLine(xOffset, yPos, size + xOffset, yPos);
            }

            // Draw particles with adjusted coordinates
            for (Particle particle : world.getParticles()) {
                String typeForVisibility;
                if (particle instanceof GravityParticle) {
                    typeForVisibility = ((GravityParticle) particle).getType();
                } else {
                    typeForVisibility = particle.getClass().getSimpleName().replaceAll("([A-Z])", " $1").trim();
                }
                Boolean vis = particleTypeVisibility.get(typeForVisibility);
                if (vis != null && !vis) continue;

                int screenX = (int)(particle.getX()*size/world.getWidth()) + xOffset;
                int screenY = (int)(particle.getY()*size/world.getHeight()) + yOffset;
                int screenRadius = (int)(particle.getRadius()*size/world.getWidth());
                g2d.setColor(particle.cosmeticSettings != null ? particle.cosmeticSettings.color : Color.WHITE);
                g2d.fillOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2, screenRadius * 2);
                if (showVectorArrows) {
                    g2d.setColor(particle.cosmeticSettings != null ? particle.cosmeticSettings.trailColor : Color.CYAN);
                    int velX = (int)(particle.getDx()*20), velY = (int)(particle.getDy()*20);
                    g2d.drawLine(screenX, screenY, screenX+velX, screenY+velY);
                }
            }

            // Draw mouse gravity indicator when enabled
            if (mouseGravityEnabled) {
                Point mousePoint = getMousePosition();
                if (mousePoint != null) {
                    int indicatorSize = (int)(mouseGravityRange * size / world.getWidth());
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
        if (currentTime - lastSpawnTime < spawnCooldown) {
            return;  // Still in cooldown period
        }

        if (selectedFactory != null && canvas.getBounds().contains(e.getPoint())) {
            Point canvasPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), canvas);

            // Calculate size and offsets for square aspect ratio
            int size = Math.min(canvas.getWidth(), canvas.getHeight());
            int xOffset = (canvas.getWidth() - size) / 2;
            int yOffset = (canvas.getHeight() - size) / 2;

            // Convert screen coordinates to world coordinates with bounds checking
            double worldX = Math.max(0, Math.min(world.getWidth(),
                ((canvasPoint.x - xOffset) * world.getWidth()) / size));
            double worldY = Math.max(0, Math.min(world.getHeight(),
                ((canvasPoint.y - yOffset) * world.getHeight()) / size));

            // Create and add the particle
            selectedFactory.apply(worldX, worldY);
            lastSpawnTime = currentTime;  // Update last spawn time
            System.out.println("Particle added at: " + worldX + "," + worldY);
        }
    }
}
