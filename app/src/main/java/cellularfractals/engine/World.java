package cellularfractals.engine;

import java.util.ArrayList;
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
        // First move particles and handle collisions
        movementStep();

        // Then apply particle effects
        applyParticleEffects();

        // Finally apply accumulated effects
        for (Particle particle : particles) {
            particle.applyEffects();
        }
    }

    /**
     * Apply effects between particles based on proximity.
     */
    private void applyParticleEffects() {
        // For each particle, find nearby particles and apply relevant effects
        for (Particle source : particles) {
            // Get nearby particles using the grid for efficient lookup
            // This already handles the range checking for us
            List<Particle> nearbyParticles = grid.getParticlesInRange(
                    source.getX(), source.getY(), effectRange);

            // Apply effects from this particle to nearby particles
            for (Particle target : nearbyParticles) {
                if (source != target) { // Don't apply effects to self
                    // No need to check range again - the grid already did that
                    for (Effect effect : source.listEffects()) {
                        target.addEffect(effect);
                    }
                }
            }
        }
    }

    /**
     * Handles collision between two particles by updating their velocities.
     * Implements simple elastic collision.
     */
    private void handleCollision(Particle p1, Particle p2) {
        // Calculate collision normal
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist == 0) return; // Avoid division by zero

        // Normalize the collision vector
        dx /= dist;
        dy /= dist;

        // Project velocities onto collision normal
        double p1Proj = p1.getDx() * dx + p1.getDy() * dy;
        double p2Proj = p2.getDx() * dx + p2.getDy() * dy;

        // Calculate new velocities (simple elastic collision)
        p1.setVelocity(
            p1.getDx() + dx * (p2Proj - p1Proj),
            p1.getDy() + dy * (p2Proj - p1Proj)
        );

        p2.setVelocity(
            p2.getDx() + dx * (p1Proj - p2Proj),
            p2.getDy() + dy * (p1Proj - p2Proj)
        );
    }

    /**
     * Performs a movement step for all particles, handling collisions.
     */
    public void movementStep() {
        // First move all particles
        for (Particle particle : particles) {
            particle.move();

            // Bounce off world boundaries
            if (particle.getX() < 0 || particle.getX() > width) {
                particle.setVelocity(-particle.getDx(), particle.getDy());
            }
            if (particle.getY() < 0 || particle.getY() > height) {
                particle.setVelocity(particle.getDx(), -particle.getDy());
            }
        }

        // Check for collisions
        for (Particle p1 : particles) {
            List<Particle> nearby = grid.getParticlesInRange(p1.getX(), p1.getY(), 2.0); // Assuming particle size ~1.0
            for (Particle p2 : nearby) {
                if (p1 == p2) continue;

                // Simple collision detection using distance
                double dx = p2.getX() - p1.getX();
                double dy = p2.getY() - p1.getY();
                double distSquared = dx * dx + dy * dy;

                if (distSquared < 1.0) { // Assuming particle radius of 0.5
                    handleCollision(p1, p2);
                }
            }

            // Update particle position in grid after potential collision
            grid.updateParticlePosition(p1, p1.getX(), p1.getY());
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
