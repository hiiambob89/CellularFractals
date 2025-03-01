package cellularfractals.particles.effects;

import cellularfractals.engine.Force;
import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;

public class GroundGravityEffect extends Effect {
  private Float strength;
  private boolean enabled = false;

  public void apply(Particle p, double deltaTime) {
    if (enabled) {
      p.addForce(new Force(0, strength));
    }
  }

  public GroundGravityEffect(Float strength) {
    this.strength = strength;
  }
  
  public void setStrength(Float strength) {
    this.strength = strength;
  }
  
  public Float getStrength() {
    return strength;
  }
  
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  public boolean isEnabled() {
    return enabled;
  }
}
