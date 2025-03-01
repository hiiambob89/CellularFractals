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
    }

    public void run() {
        for (int i = 0; i < 20; i++) {
            world.update(.2);
        }
    }
}
