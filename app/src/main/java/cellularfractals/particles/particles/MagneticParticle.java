package cellularfractals.particles.particles;

import java.awt.Color;

import cellularfractals.engine.World;
import cellularfractals.particles.Particle;
import cellularfractals.particles.effects.MagneticEffect;

public class MagneticParticle  extends Particle {
    String type;
    public MagneticParticle(World world, double x, double y, double dx, double dy, Float magRange, Float magStrength, String type) {
        super(world, x, y, dx, dy);
        this.type = type;
        this.addEffect(new MagneticEffect(magRange, magStrength));
        if (type.equals("Positive")) {
            this.cosmeticSettings.color = new Color(0, 0, 255);
        } else if (type.equals("Negative")) {
            this.cosmeticSettings.color = new Color(255, 0, 0);
        } else {
            this.cosmeticSettings.color = new Color(255, 255, 255);
        }

    }
    public String getType() {
        return type; // Return the actual polarity type
    }
}
