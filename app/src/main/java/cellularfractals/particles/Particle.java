package cellularfractals.particles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cellularfractals.engine.World;

public abstract class Particle {
  private World world;
  private double x;
  private double y;
  private double dx;
  private double dy;

  public Particle(World world, double x, double y, double dx, double dy) {
    this.x = x;
    this.y = y;
    this.dx = dx;
    this.dy = dy;
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
    return dx;
  }
  public double getDy() {
    return dy;
  }
  public void setWorld(World world) {
    this.world = world;
  }

  public void move() {
    x += dx;
    y += dy;
  }

  public void setVelocity(double dx, double dy) {
    this.dx = dx;
    this.dy = dy;
  }

  public void setPos(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public CosmeticSettings cosmeticSettings;

  private Set<Effect> effects = new HashSet<Effect>();

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

  private Set<String> defaultEffectModifiers = new HashSet<String>();
  private Set<String> effectModifiers = new HashSet<String>(defaultEffectModifiers);

  public void addEffectModifier(String modifier) {
    effectModifiers.add(modifier);
  }

  public void removeEffectModifier(String modifier) {
    effectModifiers.remove(modifier);
  }

  public boolean hasEffectModifier(String modifier) {
    return effectModifiers.contains(modifier);
  }
}
