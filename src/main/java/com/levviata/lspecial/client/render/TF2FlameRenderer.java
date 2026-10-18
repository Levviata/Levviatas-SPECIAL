package com.levviata.lspecial.client.render;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.levviata.lspecial.TF2FlamethrowerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Random;

/** Renders a large, colored flame body through NTM's native BulletConfig renderer callback. */
public final class TF2FlameRenderer {
    private static final Map<EntityBulletBaseMK4, int[]> LAST_AGE_AND_VARIATION = new WeakHashMap<>();

    private TF2FlameRenderer() {}

    public static void render(EntityBulletBaseMK4 bullet, Float partialTicks) {
        World world = bullet.world;
        if (world == null || !world.isRemote || Minecraft.getMinecraft().effectRenderer == null) return;
        int[] state = LAST_AGE_AND_VARIATION.get(bullet);
        if (state == null) {
            Random random = new Random(bullet.getUniqueID().getMostSignificantBits() ^ bullet.getUniqueID().getLeastSignificantBits());
            state = new int[] {-1, 80 + random.nextInt(41), 80 + random.nextInt(41)};
            LAST_AGE_AND_VARIATION.put(bullet, state);
        }
        if (state[0] == bullet.ticksExisted) return;
        state[0] = bullet.ticksExisted;

        // read the live, tuned velocity off the bullet's own config instead of a hardcoded number, so
        // retuning TF2FlamethrowerHandler.LSPECIAL_FLAME_VELOCITY is a one-place change (see ticksToReachRange)
        float configVelocity = (bullet.config != null && bullet.config.velocity > 0.0001F) ? bullet.config.velocity : 0.2F;

        EntityLivingBase shooter = bullet.getThrower();
        // no shooter to measure real distance against: approximate using the average of the 100%->10%
        // decel curve (~55% of the base velocity) over elapsed ticks
        double distance = shooter == null
                ? Math.min(TF2FlamethrowerHandler.LSPECIAL_FLAME_RANGE_BLOCKS, bullet.ticksExisted * configVelocity * 0.55D)
                : bullet.getDistance(shooter);
        float r, g, b;
        // bands match the spec exactly: near 0-2 blocks (blue), mid 2-5 blocks (yellow), far 5-8 blocks (white)
        if (distance < 2.0D) {
            float t = (float)(distance / 2.0D); r = 0.08F + 0.32F * t; g = 0.20F + 0.35F * t; b = 1.0F;
        } else if (distance < 5.0D) {
            float t = (float)((distance - 2.0D) / 3.0D); r = 0.40F + 0.60F * t; g = 0.55F + 0.40F * t; b = 1.0F - 0.92F * t;
        } else {
            float t = (float)Math.min(1.0D, (distance - 5.0D) / 3.0D); r = 1.0F; g = 0.95F + 0.05F * t; b = 0.08F + 0.92F * t;
        }
        float saturation = state[2] / 100.0F;
        float luminance = 0.299F*r + 0.587F*g + 0.114F*b;
        r = clamp((luminance + (r-luminance)*saturation) * state[1]/100.0F);
        g = clamp((luminance + (g-luminance)*saturation) * state[1]/100.0F);
        b = clamp((luminance + (b-luminance)*saturation) * state[1]/100.0F);

        // growth duration tracks whatever velocity the flame is actually tuned to, so it always finishes
        // growing right around when the flame reaches its hard range - see TF2FlamethrowerHandler.ticksToReachRange
        int growthTicks = TF2FlamethrowerHandler.ticksToReachRange(configVelocity);
        float growth = Math.min(1.0F, bullet.ticksExisted / (float) growthTicks);
        float coreScale = 2.4F + 4.8F * growth;
        // overlap several large sprites so the trail reads as one flame instead of isolated dots
        for (int i = 0; i < 7; i++) {
            double back = i * 0.035D;
            double jitterX = (world.rand.nextDouble() - 0.5D) * (0.16D + growth * 0.25D);
            double jitterY = (world.rand.nextDouble() - 0.5D) * (0.16D + growth * 0.25D);
            double jitterZ = (world.rand.nextDouble() - 0.5D) * (0.16D + growth * 0.25D);
            float layer = 1.0F - i * 0.085F;
            TF2FlameParticle particle = new TF2FlameParticle(world,
                    bullet.posX - bullet.motionX * back + jitterX, bullet.posY - bullet.motionY * back + jitterY, bullet.posZ - bullet.motionZ * back + jitterZ,
                    -bullet.motionX * (0.025D + i * 0.009D), -bullet.motionY * (0.025D + i * 0.009D), -bullet.motionZ * (0.025D + i * 0.009D),
                    coreScale * layer, r, g, b);
            Minecraft.getMinecraft().effectRenderer.addEffect(particle);
        }
    }

    private static float clamp(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
