package cellularfractals.particles.effects;

import java.util.List;

import cellularfractals.engine.Force;
import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;

/**
 * A repulsive force effect that pushes particles away from the source.
 */
public class NegativeGravityEffect extends Effect {
  private Float range;
  private Float strength; // This should be a positive value internally

  public void apply(Particle p, double deltaTime) {
    List<Particle> particlesInRange = p.getWorld().grid.getParticlesInRange(p.getX(), p.getY(), range);
    for (Particle target : particlesInRange) {
      if (target == p) {
        continue;
      }

      // Vector from source to target (opposite of normal gravity)
      double dx = target.getX() - p.getX();
      double dy = target.getY() - p.getY();
      double distanceSquared = dx * dx + dy * dy;

      // Avoid division by zero
      if (distanceSquared < 0.000001) {
        continue;
      }

      double distance = Math.sqrt(distanceSquared);

      // Calculate repulsive force - stronger when closer
      // We use positive strength here since this is inherently repulsive
      double force = strength / distance;

      // Direction is already from source to target (pushing away)
      double forceX = force * (dx / distance);
      double forceY = force * (dy / distance);

      // Apply repulsive force
      target.addForce(new Force(forceX, forceY));
    }
  }

  public NegativeGravityEffect(Float range, Float strength) {
    this.range = range;
    // Store absolute value since this effect is inherently repulsive
    this.strength = Math.abs(strength);
  }

  public Float getRange() {
    return range;
  }

  public Float getStrength() {
    return strength;
  }

  public void setRange(Float range) {
    this.range = range;
  }

  public void setStrength(Float strength) {
    this.strength = Math.abs(strength);
  }
}
