package cellularfractals.particles.effects;

import cellularfractals.engine.World;
import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;
import cellularfractals.particles.particles.BasicParticle;

public class ExplodeEffect extends Effect {
  private int explosionRadius;

  public ExplodeEffect(int radius) {
    this.explosionRadius = radius;
  }

  public int getExplosionRadius() {
    return explosionRadius;
  }

  public void setExplosionRadius(int radius) {
    this.explosionRadius = radius;
  }

  public void apply(Particle p, double deltaTime) {
    World world = p.getWorld();
    // if collission with other particle, explode
    if (world.grid.getParticlesInRange(p.getX(), p.getY(), explosionRadius).size() > 1) {
      world.grid.removeParticle(p);
      for (int i = 0; i < 10; i++) {
        Particle p2 = new BasicParticle(world, p.getX(), p.getY(), Math.random() * 2 - 1, Math.random() * 2 - 1);
        p2.setRadius(p.getRadius() / 2);
        p2.setMass(p.getMass() / 2);
      }
      p.delete();
    }
  }
}
