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

        // Set initial size maintaining square aspect ratio
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int size = Math.min(1000, Math.min(screenSize.width - 400, screenSize.height - 100));
        setSize(size + 400, size); // Add 400 for control panel width

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
            long lastLoopTime = System.nanoTime();

            while (running) {
                long now = System.nanoTime();
                long updateLength = now - lastLoopTime;
                lastLoopTime = now;
                double delta = updateLength / ((double)OPTIMAL_TIME);

                // Update game state
                world.update(delta);

                // Render
                customPanel.repaint();

                // Sleep to maintain frame rate
                try {
                    long gameTime = System.nanoTime() - lastLoopTime;
                    long sleepTime = (OPTIMAL_TIME - gameTime) / 1000000;

                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                } catch (InterruptedException e) {
                    running = false;
                    throw new RuntimeException("Game loop interrupted", e);
                }
            }
        });

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
