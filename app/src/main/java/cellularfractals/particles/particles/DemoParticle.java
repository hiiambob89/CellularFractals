package cellularfractals.particles.particles;

import java.awt.Color;

import cellularfractals.engine.World;
import cellularfractals.particles.Particle;

public class DemoParticle extends Particle {
  public DemoParticle(World world, double x, double y, double dx, double dy) {
    super(world, x, y, dx, dy);
    this.cosmeticSettings.color = new Color(255,0,0);
  }
}
