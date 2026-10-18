package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.WeakHashMap;

// todo
// rationale: NTM's onUpdate owns collision and movement, so this mixin adjusts the velocity after each normal tick instead of cancelling or replacing that tick. It caches the initial velocity, applies a distance-based 100%-to-10% speed curve, and kills only standard flamer projectiles at 8 blocks. The render mixin supplies the enlarged visual flame. Other projectile configs are untouched; conflicts are avoidable if NTM adds native per-config speed, gravity and range settings.
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinTF2FlameProjectile {
    @Shadow
    public BulletConfig config;
    @Unique
    private static final Map<EntityBulletBaseMK4, double[]> lspecial$baseMotion = new WeakHashMap<>();

    @Inject(method = "getGravityVelocity()F", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$removeFlameGravity(CallbackInfoReturnable<Float> callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (LSPECIALConfig.flamethrowerPerk && lspecial$isStandardFlame(lspecial$getConfig(projectile)))
            callback.setReturnValue(0.0F);
    }

    @Inject(method = "onUpdate()V", at = @At("TAIL"), remap = false)
    private void lspecial$shapeFlameFlight(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (projectile.world == null || projectile.world.isRemote) return;
        if (!LSPECIALConfig.flamethrowerPerk || !lspecial$isStandardFlame(lspecial$getConfig(projectile))) return;

        EntityLivingBase shooter = projectile.getThrower();
        if (shooter == null || shooter.isDead) {
            // no shooter to measure distance against (e.g. they died or disconnected mid-burst):
            // kill the flame immediately instead of letting it coast on its last motion until
            // NTM's native BulletConfig#expires eventually catches it
            lspecial$baseMotion.remove(projectile);
            projectile.setDead();
            return;
        }

        if (projectile.getDistance(shooter) >= 8.0D) {
            lspecial$baseMotion.remove(projectile);
            projectile.setDead();
            return;
        }

        double[] base = lspecial$baseMotion.computeIfAbsent(projectile, p -> new double[]{p.motionX, p.motionY, p.motionZ});
        double progress = Math.min(1.0D, projectile.getDistance(shooter) / 8.0D);
        double speed = 1.0D - 0.9D * progress;
        projectile.motionX = base[0] * speed;
        projectile.motionY = base[1] * speed;
        projectile.motionZ = base[2] * speed;
    }

    @Unique
    private static BulletConfig lspecial$getConfig(EntityBulletBaseMK4 projectile) {
        return projectile.config != null ? projectile.config : projectile.getBulletConfig();
    }

    @Unique
    private static boolean lspecial$isStandardFlame(BulletConfig active) {
        return active != null && (active == XFactoryFlamer.flame_nograv || active == XFactoryFlamer.flame_diesel
                || active == XFactoryFlamer.flame_gas || active == XFactoryFlamer.flame_napalm
                || active == XFactoryFlamer.flame_balefire);
    }
}
