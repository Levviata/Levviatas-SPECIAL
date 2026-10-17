package com.levviata.lspecial.client.render;

import net.minecraft.client.particle.ParticleFlame;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** A large, gravity-free flame sprite for the dedicated NTM flamer renderer. */
@SideOnly(Side.CLIENT)
public final class TF2FlameParticle extends ParticleFlame {
    private final float lspecial$baseScale;

    public TF2FlameParticle(World world, double x, double y, double z, double motionX, double motionY, double motionZ, float scale, float red, float green, float blue) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.lspecial$baseScale = scale;
        this.particleScale = scale;
        this.particleGravity = 0.0F;
        this.particleMaxAge = 12;
        this.setRBGColorF(red, green, blue);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        float life = (float)this.particleAge / (float)this.particleMaxAge;
        this.particleScale = this.lspecial$baseScale * (1.0F - 0.45F * life);
    }
}
