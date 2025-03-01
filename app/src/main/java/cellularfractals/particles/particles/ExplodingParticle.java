package cellularfractals.particles.particles;

import cellularfractals.engine.World;
import cellularfractals.particles.Particle;
import cellularfractals.particles.effects.ExplodeEffect;

public class ExplodingParticle extends Particle {
  public ExplodingParticle(World world, double x, double y, double dx, double dy, int explosionRadius) {
    super(world, x, y, dx, dy);
    this.addEffect(new ExplodeEffect(explosionRadius));
  }
}
