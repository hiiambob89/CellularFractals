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
        BasicParticle particle = new BasicParticle(world, 25, 25, 0, 0);

        for (int i = 0; i < 100; i++) {
        for (int i = 0; i < 100; i++) {
            GravityParticle gravity = new GravityParticle(
                world,
                Math.random() * world.getWidth(),
                Math.random() * world.getHeight(),
<<<<<<< HEAD
                Math.random() * 20 - 10,
                Math.random() * 20 - 10, 
=======
                Math.random() * 2 - 1,
                Math.random() * 2 - 1,
>>>>>>> origin/main
                100f,
                1f
            );
            GravityParticle antigravity = new GravityParticle(
                world,
                Math.random() * world.getWidth(),
                Math.random() * world.getHeight(),
                Math.random() * 2 - 1,
                Math.random() * 2 - 1,
                100f,
                -1f
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
