package cellularfractals.particles.effects;

import cellularfractals.engine.Force;
import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;

/**
 * An effect that applies gravity or anti-gravity from a specified point (mouse position)
 */
public class MouseGravityEffect extends Effect {
    private double sourceX;
    private double sourceY;
    private float range;
    private float strength; // Positive for attraction, negative for repulsion
    private boolean enabled = true;

    public void apply(Particle p, double deltaTime) {
        if (!enabled) return;
        
        // Skip if position hasn't been set yet
        if (sourceX == 0 && sourceY == 0) return;
        
        // Calculate direction vector from particle to mouse
        double dx = sourceX - p.getX();
        double dy = sourceY - p.getY();
        double distanceSquared = dx * dx + dy * dy;
        
        // Skip if out of range
        if (distanceSquared > range * range) return;
        
        // Avoid division by zero
        if (distanceSquared < 0.000001) return;
        
        double distance = Math.sqrt(distanceSquared);
        
        // Calculate force - stronger when closer
        double force = strength / distance;
        
        // Direction depends on whether it's attraction or repulsion
        double dirX = dx / distance;
        double dirY = dy / distance;
        
        // Apply force
        p.addForce(new Force(force * dirX, force * dirY));
    }

    public MouseGravityEffect(float range, float strength) {
        this.range = range;
        this.strength = strength;
    }
    
    public void setPosition(double x, double y) {
        this.sourceX = x;
        this.sourceY = y;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setStrength(float strength) {
        this.strength = strength;
    }
    
    public float getStrength() {
        return strength;
    }
    
    public void setRange(float range) {
        this.range = range;
    }
    
    public float getRange() {
        return range;
    }
}
