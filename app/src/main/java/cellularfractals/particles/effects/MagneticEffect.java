package cellularfractals.particles.effects;

import java.util.List;

import cellularfractals.engine.Force;
import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;
import cellularfractals.particles.particles.MagneticParticle;

public class MagneticEffect extends Effect {
  private Float range;
  private Float strength;
  public void apply(Particle p, double deltaTime) {
    if (!(p instanceof MagneticParticle)) return;
    MagneticParticle mp = (MagneticParticle)p;
    
    List<Particle> particlesInRange = p.getWorld().grid.getParticlesInRange(p.getX(), p.getY(), range);
    for (Particle target : particlesInRange) {
      if (target == p) {
        continue;
      }

      if (target instanceof MagneticParticle) {
        MagneticParticle mt = (MagneticParticle)target;
        if (!mt.getType().equals(mp.getType())){
      
          double dx = p.getX() - target.getX();
          double dy = p.getY() - target.getY();
          double distance = Math.sqrt(dx * dx + dy * dy);
          double force = strength / distance;
          double angle = Math.atan2(dy, dx);

          target.addForce(new Force(
            force * Math.cos(angle),
            force * Math.sin(angle)
          ));

        } else {
          double dx = target.getX() - p.getX();
          double dy = target.getY() - p.getY();
          double distanceSquared = dx * dx + dy * dy;

          // Avoid division by zero
          if (distanceSquared < 0.000001) {
            continue;
          }

          double distance = Math.sqrt(distanceSquared);

          // Calculate repulsive force - stronger when closer
          double force = strength / distance;

          // Direction is already from source to target (pushing away)
          double forceX = force * (dx / distance);
          double forceY = force * (dy / distance);

          // Apply repulsive force
          target.addForce(new Force(forceX, forceY));
        }
      }
    }
  }
  
  public MagneticEffect(Float range, Float strength) {
    this.range = range;
    this.strength = strength;
  }
}