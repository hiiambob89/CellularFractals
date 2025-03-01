package cellularfractals.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import cellularfractals.particles.Particle;
import cellularfractals.particles.Effect;

public class World {
    private final double width;
    private final double height;
    private final Grid grid;
    private final Set<Particle> particles; // Changed to Set
    private final double effectRange; // Default range for particle effects
    
    /**
     * Creates a new simulation world with the specified dimensions.
     * @param width Width of the world
     * @param height Height of the world
     * @param cellSize Size of each grid cell for spatial partitioning
     * @param effectRange Default range for particle effects
     */
    public World(double width, double height, double cellSize, double effectRange) {
        this.width = width;
        this.height = height;
        this.grid = new Grid(Math.max(width, height), cellSize);
        // Using ConcurrentHashMap.newKeySet() for thread-safe Set
        this.particles = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.effectRange = effectRange;
    }
    
    /**
     * Adds a particle to the world.
     * @param particle The particle to add
     * @return true if the particle was added, false if it was already in the world
     */
    public boolean addParticle(Particle particle) {
        boolean added = particles.add(particle);
        if (added) {
            grid.addParticle(particle);
        }
        return added;
    }
    
    /**
     * Removes a particle from the world.
     * @param particle The particle to remove
     * @return true if the particle was removed, false if it wasn't in the world
     */
    public boolean removeParticle(Particle particle) {
        boolean removed = particles.remove(particle);
        if (removed) {
            grid.removeParticle(particle);
        }
        return removed;
    }
    
    /**
     * Checks if a particle is in the world.
     * @param particle The particle to check
     * @return true if the particle is in the world, false otherwise
     */
    public boolean containsParticle(Particle particle) {
        return particles.contains(particle);
    }
    
    /**
     * Updates the state of all particles in the world.
     * @param deltaTime Time elapsed since last update
     */
    public void update(double deltaTime) {
        // First, apply effects between particles
        applyParticleEffects();
        
        // Then update positions and handle any movement-related logic
        for (Particle particle : particles) {
            // Store previous position
            double oldX = particle.getX();
            double oldY = particle.getY();
            
            // Apply effects to update particle state
            particle.applyEffects();
            
            // Update particle's position in the grid
            grid.updateParticlePosition(particle, oldX, oldY);
            
            // Additional update logic could go here
            // For example: bounds checking, collision handling, etc.
        }
    }
    
    /**
     * Apply effects between particles based on proximity.
     */
    private void applyParticleEffects() {
        // For each particle, find nearby particles and apply relevant effects
        for (Particle source : particles) {
            // Get nearby particles using the grid for efficient lookup
            List<Particle> nearbyParticles = grid.getParticlesInRange(
                    source.getX(), source.getY(), effectRange);
            
            // Apply effects from this particle to nearby particles
            for (Particle target : nearbyParticles) {
                if (source != target) { // Don't apply effects to self
                    for (Effect effect : source.listEffects()) {
                        if (effect.isInRange(source, target)) {
                            target.addEffect(effect);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Clears all particles from the world.
     */
    public void clear() {
        particles.clear();
        grid.clear();
    }
    
    /**
     * Gets all particles in the world.
     * @return List of all particles
     */
    public List<Particle> getParticles() {
        return new ArrayList<>(particles);
    }
    
    /**
     * Gets the count of particles in the world.
     * @return Number of particles
     */
    public int getParticleCount() {
        return particles.size();
    }
    
    /**
     * Gets particles near a specific point.
     * @param x X coordinate
     * @param y Y coordinate
     * @param radius Search radius
     * @return List of particles within the specified radius
     */
    public List<Particle> getParticlesNear(double x, double y, double radius) {
        return grid.getParticlesInRange(x, y, radius);
    }
    
    /**
     * Gets the width of the world.
     * @return World width
     */
    public double getWidth() {
        return width;
    }
    
    /**
     * Gets the height of the world.
     * @return World height
     */
    public double getHeight() {
        return height;
    }
}
