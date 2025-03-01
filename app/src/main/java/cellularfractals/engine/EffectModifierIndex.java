package cellularfractals.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cellularfractals.particles.Particle;

public class EffectModifierIndex {
    private World world;
    private Map<String, Set<Particle>> index = new HashMap<>();
    EffectModifierIndex(World world) {
        this.world = world;
    }
    public void initializeIndex(){
        for (Particle particle : world.getParticles()) {
            for (String effectModifier : particle.listEffectModifiers()) {
                if (!index.containsKey(effectModifier)) {
                    index.put(effectModifier, new HashSet<>());
                }
                index.get(effectModifier).add(particle);
            }
        }
    }
    public Set<Particle> getParticlesWithEffectModifier(String effectModifier) {
        return index.get(effectModifier);
    }
    public void addParticleEffectModifier(Particle particle, String effectModifier) {
        if (!index.containsKey(effectModifier)) {
            index.put(effectModifier, new HashSet<>());
        }
        index.get(effectModifier).add(particle);
    }
    public void removeParticleEffectModifier(Particle particle, String effectModifier) {
        if (index.containsKey(effectModifier)) {
            index.get(effectModifier).remove(particle);
        }
    }
}
