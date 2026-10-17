package com.levviata.lspecial.client.render;

import net.minecraft.client.particle.ParticleFlame;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** A scaled, tinted flame particle for the TF2-style flamethrower trail. */
@SideOnly(Side.CLIENT)
public final class TF2FlameParticle extends ParticleFlame {
    public TF2FlameParticle(World world, double x, double y, double z,
                            double motionX, double motionY, double motionZ,
                            float scale, float red, float green, float blue) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.particleScale = scale;
        this.particleGravity = 0.0F;
        this.particleMaxAge = 8;
        this.setRBGColorF(red, green, blue);
    }
}
