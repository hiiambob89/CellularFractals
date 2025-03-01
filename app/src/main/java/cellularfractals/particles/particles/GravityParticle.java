package cellularfractals.particles.particles;

import java.awt.Color;

import cellularfractals.engine.World;
import cellularfractals.particles.effects.GravityEffect;

public class GravityParticle extends BasicParticle {
  public GravityParticle(World world, double x, double y, double dx, double dy, Float gravitRange, Float gravityStrength) {
    super(world, x, y, dx, dy);
    this.addEffect(new GravityEffect(gravitRange, gravityStrength));
    this.cosmeticSettings.color = new Color(255,0,0);
  }
}
