package cellularfractals.particles.particles;

import java.awt.Color;

import cellularfractals.engine.World;

/**
 * A particle that is affected by gravity and bounces off walls,
 * but passes through other particles without collision.
 */
public class GhostParticle extends BasicParticle {
    
    /**
     * Creates a new ghost particle
     * 
     * @param world The world this particle belongs to
     * @param x Initial x position
     * @param y Initial y position
     * @param dx Initial x velocity
     * @param dy Initial y velocity
     */
    public GhostParticle(World world, double x, double y, double dx, double dy) {
        super(world, x, y, dx, dy);
        this.cosmeticSettings.color = new Color(200, 200, 255, 180); // Semi-transparent light blue
    }
    
    /**
     * Indicates whether this particle should collide with other particles.
     * Ghost particles pass through others without collision.
     * 
     * @return false since ghost particles do not collide with others
     */
    @Override
    public boolean canCollideWithParticles() {
        return false;
    }
}
