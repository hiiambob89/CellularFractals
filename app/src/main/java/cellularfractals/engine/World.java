package cellularfractals.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cellularfractals.particles.Particle;

public class World {
    public final double width;
    public final double height;
    public final Grid grid;
    public final EffectModifierIndex effectModifierIndex;
    public final Set<Particle> particles; // Changed to Set

    /**
     * Creates a new simulation world with the specified dimensions.
     * @param width Width of the world
     * @param height Height of the world
     * @param cellSize Size of each grid cell for spatial partitioning
     * @param effectRange Default range for particle effects
     */
    public World(double width, double height, double cellSize) {
        this.width = width;
        this.height = height;
        this.grid = new Grid(Math.max(width, height), cellSize);
        this.effectModifierIndex = new EffectModifierIndex(this);
        // Using ConcurrentHashMap.newKeySet() for thread-safe Set
        this.particles = Collections.newSetFromMap(new ConcurrentHashMap<>());
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

        // Finally apply accumulated effects
        for (Particle particle : particles) {
            particle.applyEffects();
            System.out.println(particle.getX()+" "+ particle.getY());
        }
        this.movementStep(deltaTime);
    }

    /**
     * Handles collision between two particles by updating their velocities.
     * Implements simple elastic collision.
     */
    private void handleCollision(Particle p1, Particle p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist == 0) return;

        // Normalize the collision vector
        double nx = dx / dist;
        double ny = dy / dist;

        // Calculate relative velocity
        double rvx = p2.getDx() - p1.getDx();
        double rvy = p2.getDy() - p1.getDy();
        double velAlongNormal = rvx * nx + rvy * ny;

        // Don't resolve if objects are separating
        if (velAlongNormal > 0) return;

        // Calculate restitution (bounciness)
        double restitution = 0.8;

        // Calculate impulse scalar
        double j = -(1 + restitution) * velAlongNormal;
        j /= 1/p1.getMass() + 1/p2.getMass();

        // Apply impulse
        double impulseX = j * nx;
        double impulseY = j * ny;

        p1.setVelocity(
            p1.getDx() - (impulseX / p1.getMass()),
            p1.getDy() - (impulseY / p1.getMass())
        );

        p2.setVelocity(
            p2.getDx() + (impulseX / p2.getMass()),
            p2.getDy() + (impulseY / p2.getMass())
        );
    }

    /**
     * Performs a movement step for all particles, handling collisions.
     */
    public void movementStep(double deltaTime) {
        // Clear forces from previous step
        for (Particle particle : particles) {
            particle.clearForces();
        }

        // First move all particles
        for (Particle particle : particles) {
            particle.moveStep(deltaTime);

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
