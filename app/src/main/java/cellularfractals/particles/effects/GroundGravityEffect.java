package cellularfractals.particles.effects;

import cellularfractals.engine.Force;
import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;

public class GroundGravityEffect extends Effect {
  private Float strength;

  public void apply(Particle p) {
    p.addForce(new Force(0, strength));
  }

  public GroundGravityEffect(Float strength) {
    this.strength = strength;
  }
}
