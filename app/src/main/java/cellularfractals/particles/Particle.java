package cellularfractals.particles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.awt.Color;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cellularfractals.engine.World;
import cellularfractals.engine.Force;

public abstract class Particle {
  private World world;
  private double x;
  private double y;
  private double baseVelocityX;
  private double baseVelocityY;
  private List<Force> forces = new CopyOnWriteArrayList<>();
  private double mass = 1.0; // Default mass
  public CosmeticSettings cosmeticSettings;
  private double radius = .5; // Default radius
  private double restitution = .8; // Default elasticity (1.0 = perfect elastic, 0.0 = perfect inelastic)
  private double friction = 0.1; // Default friction coefficient for collisions
  private volatile boolean effectsApplied = false;

  public Particle(World world, double x, double y, double dx, double dy) {
    this.x = x;
    this.y = y;
    this.baseVelocityX = dx;
    this.baseVelocityY = dy;
    this.world = world;
    this.cosmeticSettings = new CosmeticSettings(); // Initialize with default constructor
    world.addParticle(this);
  }

  public World getWorld() {
    return world;
  }
  public double getX() {
    return x;
  }
  public double getY() {
    return y;
  }

  public double getDx() {
    double totalAx = forces.stream().mapToDouble(f -> f.ax).sum();
    return baseVelocityX + totalAx;
  }

  public double getDy() {
    double totalAy = forces.stream().mapToDouble(f -> f.ay).sum();
    return baseVelocityY + totalAy;
  }

  public synchronized void setVelocity(double dx, double dy) {
    this.baseVelocityX = dx;
    this.baseVelocityY = dy;
  }

  public synchronized void setPos(double x, double y) {
    this.x = x;
    this.y = y;
  }

  private Set<Effect> effects = ConcurrentHashMap.newKeySet();

  public List<Effect> listEffects() {
    return new ArrayList<>(effects);
  }

  public void addEffect(Effect effect) {
    effects.add(effect);
  }

  public void removeEffect(Effect effect) {
    effects.remove(effect);
  }

  public synchronized void applyEffects(double deltaTime) {
    if (effectsApplied) return;
    for (Effect effect : effects) {
      effect.apply(this, deltaTime); // Assuming effect.apply() method will be updated accordingly
    }
    effectsApplied = true;
  }

  private Set<String> effectModifiers = ConcurrentHashMap.newKeySet();
public Object type;

  public List<String> listEffectModifiers() {
    return new ArrayList<>(effectModifiers);
  }

  public void addEffectModifier(String modifier) {
    effectModifiers.add(modifier);
    this.world.effectModifierIndex.addParticleEffectModifier(this, modifier);
  }

  public void removeEffectModifier(String modifier) {
    effectModifiers.remove(modifier);
    this.world.effectModifierIndex.removeParticleEffectModifier(this, modifier);
  }

  public boolean hasEffectModifier(String modifier) {
    return effectModifiers.contains(modifier);
  }

  public List<Force> listForces() {
    return new ArrayList<>(forces);
  }

  public void addForce(Force force) {
    forces.add(force);
  }

  public void clearForces() {
    forces.clear();
    effectsApplied = false;
  }

  public double getMass() {
    return mass;
  }

  public void setMass(double mass) {
    this.mass = mass;
  }

  public double getRadius() {
    return radius;
  }

  public void setRadius(double radius) {
    this.radius = radius;
  }

  public double getRestitution() {
    return restitution;
  }

  public void setRestitution(double restitution) {
    this.restitution = Math.max(0.0, Math.min(1.0, restitution));
  }

  public double getFriction() {
    return friction;
  }

  public void setFriction(double friction) {
    this.friction = Math.max(0.0, friction);
  }

  public synchronized void moveStep(double deltaTime) {
    double oldX = x;
    double oldY = y;
    x += getDx() * deltaTime;
    y += getDy() * deltaTime;
    this.world.grid.updateParticlePosition(this, oldX, oldY);
  }

  /**
   * Indicates whether this particle can collide with other particles.
   * Override this in subclasses to modify collision behavior.
   *
   * @return true if this particle should collide with other particles
   */
  public boolean canCollideWithParticles() {
    return true; // Default behavior: particles collide with each other
  }

  // Cosmetic settings for particle display
  public static class CosmeticSettings {
    public Color color = Color.WHITE;
    public Color trailColor = Color.CYAN;

    // Default constructor
    public CosmeticSettings() {
      // Default values already set in field declarations
    }

    // Constructor with color parameter
    public CosmeticSettings(Color color) {
      this.color = color;
    }

    // Constructor with both color parameters
    public CosmeticSettings(Color color, Color trailColor) {
      this.color = color;
      this.trailColor = trailColor;
    }
  }

  public void delete() {
    this.world.removeParticle(this);
  }
}
