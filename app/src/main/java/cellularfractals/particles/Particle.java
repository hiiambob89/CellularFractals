package cellularfractals.particles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Particle {
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
}
