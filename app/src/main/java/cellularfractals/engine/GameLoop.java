package cellularfractals.engine;

import cellularfractals.GUI.MainFrame;
import cellularfractals.particles.particles.BasicParticle;
import cellularfractals.particles.particles.GravityParticle;

public class GameLoop {
    private World world;
    private MainFrame frame;

    public GameLoop(World world) {
        this.world = world;
        initializeParticles();

        // Create and show the GUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            frame = new MainFrame(world);
        });
    }

    private void initializeParticles() {
        new GravityParticle(world, 25, 25, 0, 0, 100f, 1f);
        new GravityParticle(world, 50, 25, 0, 0, 100f, 1f);

        for (int i = 0; i < 1000; i++) {
            new GravityParticle(
                world,
                Math.random() * world.getWidth(),
                Math.random() * world.getHeight(),
                Math.random() * 2 - 1,
                Math.random() * 2 - 1,
                5f,
                .01f
            );
            new GravityParticle(
                world,
                Math.random() * world.getWidth(),
                Math.random() * world.getHeight(),
                Math.random() * 2 - 1,
                Math.random() * 2 - 1,
                5f,
                -.001f
            );
        }
    }

    public void run() {
        // Create update thread
        Thread updateThread = new Thread(() -> {
            while (true) {
                world.update(0.016); // ~60 FPS
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.start();
    }
}
