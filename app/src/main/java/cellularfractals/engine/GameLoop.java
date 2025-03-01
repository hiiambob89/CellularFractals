package cellularfractals.engine;



import cellularfractals.particles.particles.BasicParticle;
import cellularfractals.particles.particles.GravityParticle;

public class GameLoop {
    private World world;
    public GameLoop() {
        this.world = new World(100, 100, 10);
        BasicParticle particle = new BasicParticle(world, 1, 1, 0, 0);
        world.addParticle(particle);
        GravityParticle gravity = new GravityParticle(world, 0, 0, 0, 0);
        world.addParticle(gravity);
        world.update(.01);
    }

    public void run() {
        while (true) {
            world.update(.01);
            // System.out.println("running");
        }
    }
}
