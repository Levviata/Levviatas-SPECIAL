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
    @Unique private double lspecial$initialSpeed = -1.0D;
    @Unique private boolean lspecial$loggedMotion;

    @Inject(method = "getGravityVelocity()F", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$removeFlameGravity(CallbackInfoReturnable<Float> callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (lspecial$isStandardFlame(config != null ? config : projectile.getBulletConfig()) || lspecial$isStandardFlamerShot(projectile)) callback.setReturnValue(0.0F);
    }

    @Inject(method = "onUpdate()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$limitStandardFlameRange(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        BulletConfig active = config != null ? config : projectile.getBulletConfig();
        if (!lspecial$isStandardFlame(active) && !lspecial$isStandardFlamerShot(projectile)) return;
        if (!lspecial$originSet) {
            lspecial$originX = projectile.posX; lspecial$originY = projectile.posY; lspecial$originZ = projectile.posZ;
            lspecial$originSet = true;
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
        if (lspecial$initialSpeed < 0.0D) {
            lspecial$initialSpeed = Math.sqrt(projectile.motionX * projectile.motionX
                    + projectile.motionY * projectile.motionY + projectile.motionZ * projectile.motionZ);
        }
    }

    @Inject(method = "onUpdate()V", at = @At("HEAD"), remap = false)
    private void lspecial$applyStandardFlameMotionBeforeMove(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        BulletConfig active = config != null ? config : projectile.getBulletConfig();
        boolean configMatch = lspecial$isStandardFlame(active);
        boolean shooterMatch = lspecial$isStandardFlamerShot(projectile);
        if ((!configMatch && !shooterMatch) || !lspecial$originSet || projectile.isDead) return;
        if (!lspecial$loggedMotion) {
            lspecial$loggedMotion = true;
            com.levviata.lspecial.LSPECIALMod.logWarn("[TF2FlameMotion] active: configMatch={} shooterMatch={} initialMotion=({}, {}, {})", configMatch, shooterMatch, projectile.motionX, projectile.motionY, projectile.motionZ);
        }
        double travelled = lspecial$distance(projectile.posX, projectile.posY, projectile.posZ,
                lspecial$originX, lspecial$originY, lspecial$originZ);
        if (travelled >= 8.0D) { projectile.setDead(); callback.cancel(); return; }
        double factor = Math.max(0.10D, 1.0D - 0.90D * Math.min(1.0D, travelled / 8.0D));
        double speed = lspecial$initialSpeed < 0.0D ? Math.sqrt(projectile.motionX * projectile.motionX + projectile.motionY * projectile.motionY + projectile.motionZ * projectile.motionZ) : lspecial$initialSpeed;
        if (lspecial$initialSpeed < 0.0D) lspecial$initialSpeed = speed;
        double current = Math.sqrt(projectile.motionX * projectile.motionX + projectile.motionY * projectile.motionY + projectile.motionZ * projectile.motionZ);
        if (current > 1.0E-5D) {
            double targetSpeed = speed * factor;
            projectile.motionX = projectile.motionX / current * targetSpeed;
            projectile.motionY = projectile.motionY / current * targetSpeed;
            projectile.motionZ = projectile.motionZ / current * targetSpeed;
        }
    }

    @Inject(method = "onUpdate()V", at = @At("RETURN"), remap = false)
    private void lspecial$removeStandardFlameAtRange(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        BulletConfig active = config != null ? config : projectile.getBulletConfig();
        if ((!lspecial$isStandardFlame(active) && !lspecial$isStandardFlamerShot(projectile)) || !lspecial$originSet) return;
        if (lspecial$distance(projectile.posX, projectile.posY, projectile.posZ,
                lspecial$originX, lspecial$originY, lspecial$originZ) >= 8.0D) projectile.setDead();
    }

    @Unique
    private static double lspecial$distance(double x, double y, double z, double ox, double oy, double oz) {
        double dx = x - ox, dy = y - oy, dz = z - oz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Unique
    private static boolean lspecial$isStandardFlamerShot(EntityBulletBaseMK4 projectile) {
        net.minecraft.entity.EntityLivingBase shooter = projectile.getThrower();
        if (shooter == null) return false;
        net.minecraft.item.ItemStack main = shooter.getHeldItemMainhand();
        net.minecraft.item.ItemStack off = shooter.getHeldItemOffhand();
        return lspecial$isStandardFlamerItem(main) || lspecial$isStandardFlamerItem(off);
    }

    @Unique
    private static boolean lspecial$isStandardFlamerItem(net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return false;
        String id = stack.getItem().getRegistryName().toString();
        return "hbm:gun_flamer".equals(id);
    }

    @Unique
    private static boolean lspecial$isStandardFlame(BulletConfig config) {
        return config != null && (config == XFactoryFlamer.flame_nograv || config == XFactoryFlamer.flame_diesel
                || config == XFactoryFlamer.flame_gas || config == XFactoryFlamer.flame_napalm
                || config == XFactoryFlamer.flame_balefire);
    }
}
