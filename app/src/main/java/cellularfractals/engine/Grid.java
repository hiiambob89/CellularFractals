package cellularfractals.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cellularfractals.particles.Particle;

public class Grid {
    private final double gridSize;
    private final double cellSize;
    private final ConcurrentHashMap<Point, List<Particle>> cells;

    /**
     * Creates a grid for spatial partitioning of particles.
     * @param gridSize The total size of the simulation space
     * @param cellSize The size of each cell in the grid
     */
    public Grid(double gridSize, double cellSize) {
        this.gridSize = gridSize;
        this.cellSize = cellSize;
        this.cells = new ConcurrentHashMap<>();
    }

    /**
     * Adds a particle to the grid.
     * @param particle The particle to add
     */
    public void addParticle(Particle particle) {
        Point cell = getCellForPosition(particle.getX(), particle.getY());
        cells.computeIfAbsent(cell, k -> new ArrayList<>());
        synchronized (cells.get(cell)) {
            cells.get(cell).add(particle);
        }
    }

    /**
     * Removes a particle from the grid.
     * @param particle The particle to remove
     */
    public void removeParticle(Particle particle) {
        Point cell = getCellForPosition(particle.getX(), particle.getY());
        List<Particle> cellParticles = cells.get(cell);
        if (cellParticles != null) {
            synchronized (cellParticles) {
                cellParticles.remove(particle);
            }
        }
    }

    /**
     * Updates a particle's position in the grid.
     * @param particle The particle that moved
     * @param oldX Previous X position
     * @param oldY Previous Y position
     */
    public void updateParticlePosition(Particle particle, double oldX, double oldY) {
        Point oldCell = getCellForPosition(oldX, oldY);
        Point newCell = getCellForPosition(particle.getX(), particle.getY());

        if (!oldCell.equals(newCell)) {
            List<Particle> oldCellParticles = cells.get(oldCell);
            if (oldCellParticles != null) {
                synchronized (oldCellParticles) {
                    oldCellParticles.remove(particle);
                }
            }

            cells.computeIfAbsent(newCell, k -> new ArrayList<>());
            List<Particle> newCellParticles = cells.get(newCell);
            synchronized (newCellParticles) {
                newCellParticles.add(particle);
            }
        }
    }

    /**
     * Gets all particles within the given radius of a position.
     * @param x The x coordinate
     * @param y The y coordinate
     * @param radius The search radius
     * @return List of particles within radius
     */
    public List<Particle> getParticlesInRange(double x, double y, double radius) {
        List<Particle> result = new ArrayList<>();
        double radiusSquared = radius * radius;

        // Get cells that could contain particles within radius
        int cellRadius = (int) Math.ceil(radius / cellSize);
        Point centerCell = getCellForPosition(x, y);

        for (int i = -cellRadius; i <= cellRadius; i++) {
            for (int j = -cellRadius; j <= cellRadius; j++) {
                Point cell = new Point(centerCell.x + i, centerCell.y + j);
                List<Particle> particlesInCell = cells.get(cell);

                if (particlesInCell != null) {
                    for (Particle particle : particlesInCell) {
                        double dx = particle.getX() - x;
                        double dy = particle.getY() - y;
                        if (dx * dx + dy * dy <= radiusSquared) {
                            result.add(particle);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Gets the cell coordinates for a position.
     * @param x The x coordinate
     * @param y The y coordinate
     * @return Point representing cell coordinates
     */
    private Point getCellForPosition(double x, double y) {
        int cellX = (int) Math.floor(x / cellSize);
        int cellY = (int) Math.floor(y / cellSize);
        return new Point(cellX, cellY);
    }

    /**
     * Clears all particles from the grid.
     */
    public void clear() {
        cells.clear();
    }

    /**
     * Simple point class for cell coordinates.
     */
    private static class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }
}
