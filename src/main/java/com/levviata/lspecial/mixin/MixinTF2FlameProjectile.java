package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// rationale: NTM owns the Sedna flame projectile tick and gravity, so an addon mixin is needed to tune only the four standard flamer configs. It hooks getGravityVelocity and onUpdate without replacing hit logic; other addons touching these methods may conflict. Avoidable if NTM exposes projectile movement and trail hooks.
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinTF2FlameProjectile {
    @Shadow public BulletConfig config;
    @Unique private double lspecial$originX;
    @Unique private double lspecial$originY;
    @Unique private double lspecial$originZ;
    @Unique private boolean lspecial$originSet;
    @Unique private double lspecial$lastSpeedFactor = 1.0D;

    @Inject(method = "getGravityVelocity()F", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$removeFlameGravity(CallbackInfoReturnable<Float> callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (lspecial$isStandardFlame(config != null ? config : projectile.getBulletConfig())) callback.setReturnValue(0.0F);
    }

    @Inject(method = "onUpdate()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$limitStandardFlameRange(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        BulletConfig active = config != null ? config : projectile.getBulletConfig();
        if (!lspecial$isStandardFlame(active)) return;
        if (!lspecial$originSet) {
            lspecial$originX = projectile.posX; lspecial$originY = projectile.posY; lspecial$originZ = projectile.posZ;
            lspecial$originSet = true;
            ((EntityAccessor) projectile).lspecial$invokeSetSize(0.04F, 0.04F);
        }
        double travelled = lspecial$distance(projectile.posX, projectile.posY, projectile.posZ,
                lspecial$originX, lspecial$originY, lspecial$originZ);
        if (travelled >= 8.0D) {
            projectile.setDead();
            callback.cancel();
            return;
        }
        float growth = Math.min(1.0F, Math.max(0.0F, projectile.ticksExisted / 100.0F));
        float scale = 0.4F + growth * 0.8F;
        ((EntityAccessor) projectile).lspecial$invokeSetSize(0.10F * scale, 0.10F * scale);
        double desiredFactor = Math.max(0.10D, 1.0D - 0.90D * Math.min(1.0D, travelled / 8.0D));
        if (lspecial$lastSpeedFactor > 1.0E-5D) {
            double adjust = desiredFactor / lspecial$lastSpeedFactor;
            projectile.motionX *= adjust; projectile.motionY *= adjust; projectile.motionZ *= adjust;
        }
        lspecial$lastSpeedFactor = desiredFactor;
    }

    @Inject(method = "onUpdate()V", at = @At("RETURN"), remap = false)
    private void lspecial$removeStandardFlameAtRange(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        BulletConfig active = config != null ? config : projectile.getBulletConfig();
        if (!lspecial$isStandardFlame(active) || !lspecial$originSet) return;
        if (lspecial$distance(projectile.posX, projectile.posY, projectile.posZ,
                lspecial$originX, lspecial$originY, lspecial$originZ) >= 8.0D) projectile.setDead();
    }

    @Unique
    private static double lspecial$distance(double x, double y, double z, double ox, double oy, double oz) {
        double dx = x - ox, dy = y - oy, dz = z - oz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Unique
    private static boolean lspecial$isStandardFlame(BulletConfig config) {
        return config != null && (config == XFactoryFlamer.flame_nograv || config == XFactoryFlamer.flame_diesel
                || config == XFactoryFlamer.flame_gas || config == XFactoryFlamer.flame_napalm
                || config == XFactoryFlamer.flame_balefire);
    }
}
