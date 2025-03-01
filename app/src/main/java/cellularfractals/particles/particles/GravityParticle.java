package cellularfractals.particles.particles;

import cellularfractals.engine.World;
import cellularfractals.particles.effects.GravityEffect;

public class GravityParticle extends BasicParticle {
  public GravityParticle(World world, double x, double y, double dx, double dy) {
    super(world, x, y, dx, dy);
    this.addEffect(new GravityEffect());
  }
}
