package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.levviata.lspecial.client.render.TF2FlameParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// rationale: this client-only mixin adds the changing color and size trail because vanilla flame particles cannot be tinted or scaled per shot. It only adds particles and leaves NTM collision and damage intact; it may conflict with other trail-rendering mixins. Avoidable if NTM exposes a configurable projectile trail.
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinTF2FlameParticleTrail {
    @Shadow public BulletConfig config;
    @Unique private float lspecial$intensity = Float.NaN;
    @Unique private float lspecial$saturation = Float.NaN;
    @Unique private static int lspecial$updateEntries;
    @Unique private static int lspecial$matchedProjectiles;
    @Unique private static int lspecial$spawnedParticles;
    @Unique private boolean lspecial$loggedMatch;
    @Unique private static boolean lspecial$loggedParticle;
    @Unique
    private static int lspecial$trailUpdateCalls;

    @Inject(method = "onUpdate()V", at = @At("HEAD"), remap = false)
    private void lspecial$spawnTintedTrail(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        BulletConfig active = config != null ? config : projectile.getBulletConfig();
        boolean configMatch = lspecial$isStandardFlame(active);
        boolean shooterMatch = lspecial$isStandardFlamerShot(projectile);

        int call = ++lspecial$trailUpdateCalls;
        if (call <= 20 || call % 100 == 0) {
            com.levviata.lspecial.LSPECIALMod.logWarn(
                    "[TF2FlameTrail-DIAG] onUpdate entry call={} entityId={} age={} remote={} dead={} configNull={} configMatch={} shooterMatch={} shooter={} mainHand={} offHand={}",
                    call,
                    projectile.getEntityId(),
                    projectile.ticksExisted,
                    projectile.world.isRemote,
                    projectile.isDead,
                    active == null,
                    configMatch,
                    shooterMatch,
                    projectile.getThrower() == null ? "null" : projectile.getThrower().getClass().getName(),
                    projectile.getThrower() == null ? "null" : projectile.getThrower().getHeldItemMainhand().getItem().getRegistryName(),
                    projectile.getThrower() == null ? "null" : projectile.getThrower().getHeldItemOffhand().getItem().getRegistryName()
            );
        }

        if (!projectile.world.isRemote || (!configMatch && !shooterMatch)) {
            if (call <= 20 || call % 100 == 0) {
                com.levviata.lspecial.LSPECIALMod.logWarn(
                        "[TF2FlameTrail-DIAG] rejected call={} remote={} configMatch={} shooterMatch={}",
                        call, projectile.world.isRemote, configMatch, shooterMatch
                );
            }
            return;
        }
        if (!lspecial$loggedMatch) {
            lspecial$loggedMatch = true;
            int matched = ++lspecial$matchedProjectiles;
            if (matched <= 12 || matched % 100 == 0) {
                com.levviata.lspecial.LSPECIALMod.logWarn("[TF2FlameTrail] matched projectile={} entityId={} age={} config={} configMatch={} shooterMatch={} shooter={} pos=({}, {}, {}) motion=({}, {}, {})",
                        matched, projectile.getEntityId(), projectile.ticksExisted,
                        active == null ? "null" : active.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(active)),
                        configMatch, shooterMatch, projectile.getThrower() == null ? "null" : projectile.getThrower().getClass().getName(),
                        projectile.posX, projectile.posY, projectile.posZ, projectile.motionX, projectile.motionY, projectile.motionZ);
            }
        }
        if (projectile.ticksExisted % 2 != 0) return;
        if (Float.isNaN(lspecial$intensity)) lspecial$intensity = 0.8F + projectile.world.rand.nextFloat() * 0.4F;
        if (Float.isNaN(lspecial$saturation)) lspecial$saturation = 0.8F + projectile.world.rand.nextFloat() * 0.4F;

        EntityLivingBase shooter = projectile.getThrower();
        double distance = shooter == null ? projectile.ticksExisted * 0.08D : projectile.getDistance(shooter);
        float red, green, blue, blend;
        if (distance < 4.0D) {
            blend = (float) Math.min(1.0D, distance / 4.0D);
            red = 0.10F + 0.90F * blend; green = 0.25F + 0.65F * blend; blue = 1.0F - 0.90F * blend;
        } else {
            blend = (float) Math.min(1.0D, (distance - 4.0D) / 4.0D);
            red = 1.0F; green = 0.90F + 0.10F * blend; blue = 0.10F + 0.90F * blend;
        }
        float luminance = 0.299F * red + 0.587F * green + 0.114F * blue;
        red = (luminance + (red - luminance) * lspecial$saturation) * lspecial$intensity;
        green = (luminance + (green - luminance) * lspecial$saturation) * lspecial$intensity;
        blue = (luminance + (blue - luminance) * lspecial$saturation) * lspecial$intensity;
        float size = 0.4F + 0.8F * Math.min(1.0F, projectile.ticksExisted / 100.0F);
        double jitterX = (projectile.world.rand.nextDouble() - 0.5D) * 0.04D;
        double jitterY = (projectile.world.rand.nextDouble() - 0.5D) * 0.04D;
        double jitterZ = (projectile.world.rand.nextDouble() - 0.5D) * 0.04D;
        TF2FlameParticle particle = new TF2FlameParticle(projectile.world,
                projectile.posX, projectile.posY, projectile.posZ,
                -projectile.motionX * 0.02D + jitterX, -projectile.motionY * 0.02D + jitterY,
                -projectile.motionZ * 0.02D + jitterZ, size,
                clamp(red), clamp(green), clamp(blue));
        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
        if (!lspecial$loggedParticle) {
            lspecial$loggedParticle = true;
            com.levviata.lspecial.LSPECIALMod.logWarn("[TF2FlameTrail] first particle added: entityId={} age={} particle={} particleLifetime={} particleScale={} rgb=({}, {}, {}) effectRenderer={}",
                    projectile.getEntityId(), projectile.ticksExisted, particle.getClass().getName(), particle.lspecial$getLifetime(),
                    particle.lspecial$getScale(), clamp(red), clamp(green), clamp(blue), Minecraft.getMinecraft().effectRenderer.getClass().getName());
        }
        int spawned = ++lspecial$spawnedParticles;
        if (spawned == 1 || spawned % 500 == 0) {
            com.levviata.lspecial.LSPECIALMod.logWarn("[TF2FlameTrail] particle count={} latestEntityId={} age={} scale={} pos=({}, {}, {})", spawned,
                    projectile.getEntityId(), projectile.ticksExisted, size, projectile.posX, projectile.posY, projectile.posZ);
        }
    }

    @Unique
    private static float clamp(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }

    @Unique
    private static boolean lspecial$isStandardFlamerShot(EntityBulletBaseMK4 projectile) {
        EntityLivingBase shooter = projectile.getThrower();
        if (shooter == null) return false;
        return lspecial$isStandardFlamerItem(shooter.getHeldItemMainhand()) || lspecial$isStandardFlamerItem(shooter.getHeldItemOffhand());
    }

    @Unique
    private static boolean lspecial$isStandardFlamerItem(net.minecraft.item.ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getRegistryName() != null
                && "hbm:gun_flamer".equals(stack.getItem().getRegistryName().toString());
    }

    @Unique
    private static boolean lspecial$isStandardFlame(BulletConfig config) {
        return config != null && (config == XFactoryFlamer.flame_nograv || config == XFactoryFlamer.flame_diesel
                || config == XFactoryFlamer.flame_gas || config == XFactoryFlamer.flame_napalm
                || config == XFactoryFlamer.flame_balefire);
    }
}
