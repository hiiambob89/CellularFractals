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

    public MyPanel(World world) {
        this.world = world;
        setLayout(new BorderLayout());

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
        updateTimer = new Timer(16, e -> {
            particleCountLabel.setText("Particles: " + world.getParticleCount());
            canvas.repaint();
        });
        updateTimer.start();
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
                int screenRadius = (int)(particle.getRadius()*width/world.getWidth());
                g2d.setColor(particle.cosmeticSettings != null ? particle.cosmeticSettings.color : Color.WHITE);
                g2d.fillOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2, screenRadius * 2);
                if (showVectorArrows) {
                    g2d.setColor(particle.cosmeticSettings != null ? particle.cosmeticSettings.trailColor : Color.CYAN);
                    int velX = (int)(particle.getDx()*20), velY = (int)(particle.getDy()*20);
                    g2d.drawLine(screenX, screenY, screenX+velX, screenY+velY);
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
