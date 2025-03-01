package cellularfractals.particles.particles;

import java.awt.Color;

import cellularfractals.engine.World;
import cellularfractals.particles.effects.GravityEffect;
import cellularfractals.particles.effects.NegativeGravityEffect;

public class GravityParticle extends BasicParticle {
    private boolean isAttractive; // true = gravity, false = anti-gravity

    public GravityParticle(World world, double x, double y, double dx, double dy, Float gravitRange, Float gravityStrength) {
        super(world, x, y, dx, dy);
        if (gravityStrength < 0) {
            this.addEffect(new NegativeGravityEffect(gravitRange, gravityStrength));
            this.cosmeticSettings.color = new Color(255, 0, 175);
            isAttractive = false;
        } else {
            this.addEffect(new GravityEffect(gravitRange, gravityStrength));
            this.cosmeticSettings.color = new Color(175, 0, 255);
            isAttractive = true;
        }
    }

    // New method to get the effective particle type
    public String getType() {
        return isAttractive ? "Gravity Particle" : "Anti-Gravity Particle";
    }
}
