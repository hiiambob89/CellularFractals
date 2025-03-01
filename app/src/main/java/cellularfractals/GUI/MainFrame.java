package cellularfractals.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

import cellularfractals.engine.World;

public class MainFrame extends JFrame {
    private MyPanel customPanel;
    private World world;
    private volatile boolean running = false;
    private Thread gameThread;
    private static final int TARGET_FPS = 60;
    private static final long OPTIMAL_TIME = 1000000000 / TARGET_FPS;

    public MainFrame(World world) {
        this.world = world;
        setTitle("Particle Interaction Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        customPanel = new MyPanel(world);
        add(customPanel);

        setSize(1000, 1000);
        setLocationRelativeTo(null);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                stopGameLoop();
            }
        });
        
        setVisible(true);
        startGameLoop();
    }

    private void startGameLoop() {
        if (gameThread != null) return;
        
        running = true;
        gameThread = new Thread(() -> {
            long lastUpdateTime = System.nanoTime();
            double acc = 0;
            final double FIXED_TIME_STEP = 1.0 / 60.0;
            
            while (running) {
                long currentTime = System.nanoTime();
                // Apply velocity multiplier to deltaTime to affect simulation speed
                double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0 
                                 * MyPanel.getVelocityMultiplier();
                lastUpdateTime = currentTime;
                
                acc += deltaTime;
                
                // Fixed time step updates
                while (acc >= FIXED_TIME_STEP) {
                    world.update(FIXED_TIME_STEP);
                    acc -= FIXED_TIME_STEP;
                }
                
                // Render at display refresh rate
                SwingUtilities.invokeLater(() -> customPanel.repaint());
                
                // Sleep to limit CPU usage
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    if (!running) break;
                    Thread.currentThread().interrupt();
                }
            }
        }, "GameLoop");
        
        gameThread.setPriority(Thread.MAX_PRIORITY);
        gameThread.start();
    }

    private void stopGameLoop() {
        running = false;
        if (gameThread != null) {
            gameThread.interrupt();
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gameThread = null;
        }
    }

    @Override
    public void dispose() {
        stopGameLoop();
        if (customPanel != null) {
            customPanel.dispose();
        }
        super.dispose();
    }
}
