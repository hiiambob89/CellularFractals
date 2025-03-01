package cellularfractals.particles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

  public Particle(World world, double x, double y, double dx, double dy) {
    this.x = x;
    this.y = y;
    this.baseVelocityX = dx;
    this.baseVelocityY = dy;
    this.world = world;
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

  public CosmeticSettings cosmeticSettings;

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

  public void applyEffects() {
    for (Effect effect : effects) {
      effect.apply(this);
    }
  }

  private Set<String> defaultEffectModifiers = ConcurrentHashMap.newKeySet();
  private Set<String> effectModifiers = ConcurrentHashMap.newKeySet();
  {
    effectModifiers.addAll(defaultEffectModifiers);
  }

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

  public void addForce(Force force) {
    forces.add(force);
  }

  public void clearForces() {
    forces.clear();
  }

  public double getMass() {
    return mass;
  }

  public void setMass(double mass) {
    this.mass = mass;
  }

  public synchronized void moveStep(double deltaTime) {
    double oldX = x;
    double oldY = y;
    x += getDx() * deltaTime;
    y += getDy() * deltaTime;
    this.world.grid.updateParticlePosition(this, oldX, oldY);
  }
}
