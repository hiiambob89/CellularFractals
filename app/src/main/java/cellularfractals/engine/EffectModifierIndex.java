package cellularfractals.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cellularfractals.particles.Effect;
import cellularfractals.particles.Particle;

/**
 * Manages global effects that apply to all particles in the world
 * and particle-specific effect modifiers.
 */
public class EffectModifierIndex {
    private final World world;
    private final List<Effect> globalEffects = new ArrayList<>();
    
    // Track particle effect modifiers - maps particles to their modifiers
    private final Map<Particle, Map<String, Boolean>> particleEffectModifiers = new ConcurrentHashMap<>();
    
    public EffectModifierIndex(World world) {
        this.world = world;
    }
    
    /**
     * Adds a global effect that will be applied to all particles
     * @param effect The effect to add
     */
    public void addGlobalEffect(Effect effect) {
        if (!globalEffects.contains(effect)) {
            globalEffects.add(effect);
        }
    }
    
    /**
     * Removes a global effect
     * @param effect The effect to remove
     * @return true if successfully removed
     */
    public boolean removeGlobalEffect(Effect effect) {
        return globalEffects.remove(effect);
    }
    
    /**
     * Applies all global effects to all particles
     * @param deltaTime Time elapsed since last update
     */
    public void applyGlobalEffects(double deltaTime) {
        for (Effect effect : globalEffects) {
            for (Particle particle : world.getParticles()) {
                effect.apply(particle, deltaTime);
            }
        }
    }
    
    /**
     * Clears all global effects
     */
    public void clearGlobalEffects() {
        globalEffects.clear();
    }
    
    /**
     * Adds a particle-specific effect modifier
     * @param particle The particle to modify
     * @param modifier The name of the modifier
     */
    public void addParticleEffectModifier(Particle particle, String modifier) {
        particleEffectModifiers.computeIfAbsent(particle, k -> new HashMap<>())
                               .put(modifier, true);
    }
    
    /**
     * Removes a particle-specific effect modifier
     * @param particle The particle to modify
     * @param modifier The name of the modifier to remove
     */
    public void removeParticleEffectModifier(Particle particle, String modifier) {
        Map<String, Boolean> modifiers = particleEffectModifiers.get(particle);
        if (modifiers != null) {
            modifiers.remove(modifier);
            // Clean up if this was the last modifier for this particle
            if (modifiers.isEmpty()) {
                particleEffectModifiers.remove(particle);
            }
        }
    }
    
    /**
     * Checks if a particle has a specific effect modifier
     * @param particle The particle to check
     * @param modifier The name of the modifier to check
     * @return true if the particle has the modifier, false otherwise
     */
    public boolean hasEffectModifier(Particle particle, String modifier) {
        Map<String, Boolean> modifiers = particleEffectModifiers.get(particle);
        return modifiers != null && modifiers.containsKey(modifier);
    }
    
    /**
     * Gets all effect modifiers for a particle
     * @param particle The particle
     * @return List of modifier names for the particle
     */
    public List<String> getEffectModifiers(Particle particle) {
        Map<String, Boolean> modifiers = particleEffectModifiers.get(particle);
        if (modifiers == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(modifiers.keySet());
    }
    
    /**
     * Clears all effect modifiers for a particle
     * @param particle The particle to clear modifiers for
     */
    public void clearParticleEffectModifiers(Particle particle) {
        particleEffectModifiers.remove(particle);
    }
    
    /**
     * Removes a particle and all its effect modifiers
     * Should be called when a particle is removed from the world
     * @param particle The particle to remove
     */
    public void removeParticle(Particle particle) {
        particleEffectModifiers.remove(particle);
    }
}
