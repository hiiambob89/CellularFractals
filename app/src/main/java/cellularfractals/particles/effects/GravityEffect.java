package cellularfractals.particles.effects;

import java.util.List;

import cellularfractals.engine.Force;
import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;

public class GravityEffect extends Effect {
  private Float range = 10.0f;
  private Float strength = 1.0f;
  public void apply(Particle p) {
    List<Particle> particlesInRange = p.getWorld().grid.getParticlesInRange(p.getX(), p.getY(), range);
    for (Particle particle : particlesInRange) {
      double dx = particle.getX() - p.getX();
      double dy = particle.getY() - p.getY();
      double distance = Math.sqrt(dx * dx + dy * dy);
      double force = strength / distance;
      double angle = Math.atan2(dy, dx);

      p.addForce(new Force(
        force * Math.cos(angle),
        force * Math.sin(angle)
      ));
    }
  }
}
