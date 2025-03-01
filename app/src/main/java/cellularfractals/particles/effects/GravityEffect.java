package cellularfractals.particles.effects;

import java.util.List;

import cellularfractals.engine.Force;
import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;

public class GravityEffect extends Effect {
  private Float range;
  private Float strength;
  public void apply(Particle p) {
    List<Particle> particlesInRange = p.getWorld().grid.getParticlesInRange(p.getX(), p.getY(), range);
    for (Particle target : particlesInRange) {
      if (target == p) {
        continue;
      }
      double dx = p.getX() - target.getX();
      double dy = p.getY() - target.getY();
      double distance = Math.sqrt(dx * dx + dy * dy);
      double force = strength / distance;
      double angle = Math.atan2(dy, dx);

      target.addForce(new Force(
        force * Math.cos(angle),
        force * Math.sin(angle)
      ));
    }
  }
  public GravityEffect(Float range, Float strength) {
    this.range = range;
    this.strength = strength;
  }
}
