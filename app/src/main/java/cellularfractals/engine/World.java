package cellularfractals.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;

import cellularfractals.particles.Particle;

public class World {
    public final double width;
    public final double height;
    public final Grid grid;
    public final EffectModifierIndex effectModifierIndex;
    public final Set<Particle> particles; // Changed to Set
    private static final int PHYSICS_SUBSTEPS = 4; // Adjust based on needed precision
    private double BOUNDARY_RESTITUTION = 1; // Default boundary restitution
    private static final double LOW_SPEED_THRESHOLD = 1; // Threshold for low speed
    private static final double LOW_SPEED_REPULSION = .07; // Strength of repulsion at low speeds

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
            // Also clean up from effect modifier index
            effectModifierIndex.removeParticle(particle);
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
        ExecutorService executor = ParticleThreadPool.getExecutor();
        List<Particle> particleList = new ArrayList<>(particles);

        // Clear forces and reset effect flags first
        for (Particle particle : particleList) {
            particle.clearForces();
        }

        // Apply global effects first
        effectModifierIndex.applyGlobalEffects(deltaTime);

        int particlesPerThread = Math.max(1, particleList.size() / ParticleThreadPool.THREAD_COUNT);
        final CountDownLatch latch1 = new CountDownLatch(ParticleThreadPool.THREAD_COUNT);

        // Apply effects in parallel
        for (int i = 0; i < ParticleThreadPool.THREAD_COUNT; i++) {
            final int start = i * particlesPerThread;
            final int end = (i == ParticleThreadPool.THREAD_COUNT - 1) ?
                           particleList.size() : (i + 1) * particlesPerThread;

            executor.submit(() -> {
                try {
                    for (int j = start; j < end; j++) {
                        particleList.get(j).applyEffects(deltaTime);
                    }
                } finally {
                    latch1.countDown();
                }
            });
        }

        try {
            latch1.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Physics movement substeps
        double subDelta = deltaTime / PHYSICS_SUBSTEPS;
        for (int i = 0; i < PHYSICS_SUBSTEPS; i++) {
            this.movementStep(subDelta);
        }
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

        // Normalize collision vector
        double nx = dx / dist;
        double ny = dy / dist;

        // Get current velocities
        double v1x = p1.getDx();
        double v1y = p1.getDy();
        double v2x = p2.getDx();
        double v2y = p2.getDy();

        // Relative velocity
        double rvx = v2x - v1x;
        double rvy = v2y - v1y;
        double relativeSpeed = Math.sqrt(rvx * rvx + rvy * rvy);
        double velAlongNormal = rvx * nx + rvy * ny;

        if (velAlongNormal > 0) return; // Objects separating

        // Add low-speed repulsion
        if (relativeSpeed < LOW_SPEED_THRESHOLD) {
            double repulsionStrength = (1.0 - relativeSpeed / LOW_SPEED_THRESHOLD) * LOW_SPEED_REPULSION;
            Force f1 = new Force(-nx * repulsionStrength, -ny * repulsionStrength);
            Force f2 = new Force(nx * repulsionStrength, ny * repulsionStrength);
            p1.addForce(f1);
            p2.addForce(f2);
            return;
        }

        // Calculate impulse
        double restitution = Math.min(p1.getRestitution(), p2.getRestitution());
        double j = -(1 + restitution) * velAlongNormal;
        double impulse = j / (1/p1.getMass() + 1/p2.getMass());

        // Apply impulse along normal
        double impulsex = impulse * nx;
        double impulsey = impulse * ny;

        // Calculate new velocities
        double newV1x = v1x - (impulsex / p1.getMass());
        double newV1y = v1y - (impulsey / p1.getMass());
        double newV2x = v2x + (impulsex / p2.getMass());
        double newV2y = v2y + (impulsey / p2.getMass());

        // Set new velocities
        p1.setVelocity(newV1x, newV1y);
        p2.setVelocity(newV2x, newV2y);

        // Apply friction (tangential impulse)
        double friction = Math.min(p1.getFriction(), p2.getFriction());
        double tx = -ny;
        double ty = nx;
        double velAlongTangent = rvx * tx + rvy * ty;
        double jt = -friction * velAlongTangent / (1/p1.getMass() + 1/p2.getMass());

        // Apply tangential forces
        Force f1 = new Force(-jt * tx / p1.getMass(), -jt * ty / p1.getMass());
        Force f2 = new Force(jt * tx / p2.getMass(), jt * ty / p2.getMass());

        p1.addForce(f1);
        p2.addForce(f2);
    }

    /**
     * Performs a movement step for all particles, handling collisions.
     */
    public void movementStep(double deltaTime) {
        ExecutorService executor = ParticleThreadPool.getExecutor();
        List<Particle> particleList = new ArrayList<>(particles);
        int particlesPerThread = Math.max(1, particleList.size() / ParticleThreadPool.THREAD_COUNT);
        CountDownLatch collisionLatch = new CountDownLatch(ParticleThreadPool.THREAD_COUNT);

        // Check collisions in parallel
        for (int i = 0; i < ParticleThreadPool.THREAD_COUNT; i++) {
            final int start = i * particlesPerThread;
            final int end = (i == ParticleThreadPool.THREAD_COUNT - 1) ?
                           particleList.size() : (i + 1) * particlesPerThread;

            executor.submit(() -> {
                try {
                    for (int j = start; j < end; j++) {
                        Particle p1 = particleList.get(j);
                        // Skip collision detection for particles that don't collide with others
                        if (!p1.canCollideWithParticles()) {
                            continue;
                        }
                        double searchRadius = p1.getRadius() * 4 +
                            Math.sqrt(p1.getDx() * p1.getDx() + p1.getDy() * p1.getDy()) * deltaTime;
                        List<Particle> nearby = grid.getParticlesInRange(p1.getX(), p1.getY(), searchRadius);

                        for (Particle p2 : nearby) {
                            if (p1 == p2 || p1.hashCode() > p2.hashCode()) continue; // Prevent double processing
                            checkAndHandleCollision(p1, p2, deltaTime);
                        }
                    }
                } finally {
                    collisionLatch.countDown();
                }
            });
        }

        try {
            collisionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Move particles in parallel
        CountDownLatch movementLatch = new CountDownLatch(ParticleThreadPool.THREAD_COUNT);
        for (int i = 0; i < ParticleThreadPool.THREAD_COUNT; i++) {
            final int start = i * particlesPerThread;
            final int end = (i == ParticleThreadPool.THREAD_COUNT - 1) ?
                           particleList.size() : (i + 1) * particlesPerThread;

            executor.submit(() -> {
                try {
                    for (int j = start; j < end; j++) {
                        updateParticlePosition(particleList.get(j), deltaTime);
                    }
                } finally {
                    movementLatch.countDown();
                }
            });
        }

        try {
            movementLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void checkAndHandleCollision(Particle p1, Particle p2, double deltaTime) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double distSquared = dx * dx + dy * dy;
        double collisionDist = p1.getRadius() + p2.getRadius();

        if (distSquared <= collisionDist * collisionDist) {
            synchronized (p1.hashCode() < p2.hashCode() ? p1 : p2) {
                synchronized (p1.hashCode() < p2.hashCode() ? p2 : p1) {
                    handleCollision(p1, p2);
                }
            }
        }
    }

    private void updateParticlePosition(Particle particle, double deltaTime) {
        double oldX = particle.getX();
        double oldY = particle.getY();

        // Get current velocity
        double newDx = particle.getDx();
        double newDy = particle.getDy();

        // Update position based on current velocity
        double newX = oldX + newDx * deltaTime;
        double newY = oldY + newDy * deltaTime;
        double r = particle.getRadius();

        // Handle wall collisions after movement
        if (newX - r < 0) {
            newX = r;
            newDx = -newDx * BOUNDARY_RESTITUTION;
        } else if (newX + r > width) {
            newX = width - r;
            newDx = -newDx * BOUNDARY_RESTITUTION;
        }

        if (newY - r < 0) {
            newY = r;
            newDy = -newDy * BOUNDARY_RESTITUTION;
        } else if (newY + r > height) {
            newY = height - r;
            newDy = -newDy * BOUNDARY_RESTITUTION;
        }

        // Always update velocity and position
        particle.setVelocity(newDx, newDy);
        particle.setPos(newX, newY);
        grid.updateParticlePosition(particle, oldX, oldY);
    }

    /**
     * Clears all particles from the world.
     */
    public void clear() {
        particles.clear();
        grid.clear();
        // Also clean up the effect modifier index
        for (Particle p : getParticles()) {
            effectModifierIndex.removeParticle(p);
        }
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
