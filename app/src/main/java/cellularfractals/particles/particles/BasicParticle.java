package cellularfractals.particles.particles;

import java.awt.Color;

import cellularfractals.engine.World;
import cellularfractals.particles.Particle;
import cellularfractals.particles.effects.GroundGravityEffect;

public class BasicParticle extends Particle {
  public BasicParticle(World world, double x, double y, double dx, double dy) {
    super(world, x, y, dx, dy);
    this.cosmeticSettings.color = new Color(255,255,255);
    this.addEffect(new GroundGravityEffect(.0007f));
    this.setRestitution(1);
  }
}
