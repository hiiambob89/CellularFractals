package cellularfractals.engine;

import cellularfractals.particles.Particle;
import cellularfractals.particles.Effect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class GridTest {
    private Grid grid;
    private final double GRID_SIZE = 100.0;
    private final double CELL_SIZE = 10.0;
    
    @BeforeEach
    void setUp() {
        grid = new Grid(GRID_SIZE, CELL_SIZE);
    }
    
    @Test
    void testAddParticle() {
        TestParticle particle = new TestParticle(15.0, 25.0);
        grid.addParticle(particle);
        
        List<Particle> nearbyParticles = grid.getParticlesInRange(15.0, 25.0, 1.0);
        assertEquals(1, nearbyParticles.size());
        assertSame(particle, nearbyParticles.get(0));
    }
    
    @Test
    void testRemoveParticle() {
        TestParticle particle = new TestParticle(15.0, 25.0);
        grid.addParticle(particle);
        grid.removeParticle(particle);
        
        List<Particle> nearbyParticles = grid.getParticlesInRange(15.0, 25.0, 1.0);
        assertEquals(0, nearbyParticles.size());
    }
    
    @Test
    void testUpdateParticlePosition() {
        TestParticle particle = new TestParticle(15.0, 25.0);
        grid.addParticle(particle);
        
        // Move particle to a new cell
        particle.setPosition(35.0, 45.0);
        grid.updateParticlePosition(particle, 15.0, 25.0);
        
        // Should no longer be at old position
        List<Particle> oldPosParticles = grid.getParticlesInRange(15.0, 25.0, 1.0);
        assertEquals(0, oldPosParticles.size());
        
        // Should be at new position
        List<Particle> newPosParticles = grid.getParticlesInRange(35.0, 45.0, 1.0);
        assertEquals(1, newPosParticles.size());
        assertSame(particle, newPosParticles.get(0));
    }
    
    @Test
    void testGetParticlesInRange() {
        // Add several particles
        TestParticle p1 = new TestParticle(10.0, 10.0);
        TestParticle p2 = new TestParticle(15.0, 15.0);
        TestParticle p3 = new TestParticle(20.0, 20.0);
        TestParticle p4 = new TestParticle(50.0, 50.0);
        
        grid.addParticle(p1);
        grid.addParticle(p2);
        grid.addParticle(p3);
        grid.addParticle(p4);
        
        // Test small radius that should only include p1
        List<Particle> closeRange = grid.getParticlesInRange(10.0, 10.0, 2.0);
        assertEquals(1, closeRange.size());
        assertTrue(closeRange.contains(p1));
        
        // Test medium radius that should include p1, p2, and p3
        List<Particle> mediumRange = grid.getParticlesInRange(15.0, 15.0, 10.0);
        assertEquals(3, mediumRange.size());
        assertTrue(mediumRange.contains(p1));
        assertTrue(mediumRange.contains(p2));
        assertTrue(mediumRange.contains(p3));
        assertFalse(mediumRange.contains(p4));
        
        // Test large radius that should include all particles
        List<Particle> largeRange = grid.getParticlesInRange(25.0, 25.0, 50.0);
        assertEquals(4, largeRange.size());
    }
    
    @Test
    void testGetParticlesInRangeAcrossCells() {
        // Add particles in different cells but close to cell boundaries
        TestParticle p1 = new TestParticle(9.9, 9.9);   // Cell (0,0)
        TestParticle p2 = new TestParticle(10.1, 10.1); // Cell (1,1)
        
        grid.addParticle(p1);
        grid.addParticle(p2);
        
        // Particles are close to each other despite being in different cells
        double distance = Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2));
        assertTrue(distance < 0.3);
        
        // Test that querying with a suitable radius finds both particles
        List<Particle> found = grid.getParticlesInRange(10.0, 10.0, 0.5);
        assertEquals(2, found.size());
        assertTrue(found.contains(p1));
        assertTrue(found.contains(p2));
    }
    
    @Test
    void testClear() {
        // Add some particles
        grid.addParticle(new TestParticle(10.0, 10.0));
        grid.addParticle(new TestParticle(20.0, 20.0));
        grid.addParticle(new TestParticle(30.0, 30.0));
        
        // Verify they're in the grid
        List<Particle> before = grid.getParticlesInRange(15.0, 15.0, 50.0);
        assertEquals(3, before.size());
        
        // Clear the grid
        grid.clear();
        
        // Verify grid is empty
        List<Particle> after = grid.getParticlesInRange(15.0, 15.0, 50.0);
        assertEquals(0, after.size());
    }
    
    /**
     * Simple test particle implementation for unit tests
     */
    private static class TestParticle extends Particle {
        private double x, y;
        
        public TestParticle(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public double getX() {
            return x;
        }
        
        @Override
        public double getY() {
            return y;
        }
        
        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
