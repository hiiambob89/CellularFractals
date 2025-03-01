package cellularfractals.particles.particles;

import java.awt.Color;

import cellularfractals.engine.World;
import cellularfractals.particles.effects.GravityEffect;
import cellularfractals.particles.effects.NegativeGravityEffect;

public class GravityParticle extends BasicParticle {
  public GravityParticle(World world, double x, double y, double dx, double dy, Float gravitRange, Float gravityStrength) {
    super(world, x, y, dx, dy);
    this.cosmeticSettings.color = new Color(255,0,255);

    // Simply choose which effect to add based on the sign of gravityStrength
    if (gravityStrength < 0) {
      this.addEffect(new NegativeGravityEffect(gravitRange, gravityStrength));
    } else {
      this.addEffect(new GravityEffect(gravitRange, gravityStrength));
    }
  }
}
