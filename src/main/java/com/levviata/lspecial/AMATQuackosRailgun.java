package com.levviata.lspecial;

import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.DamageResistanceHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

/** adds a native Sedna caliber for the BMG that homes exclusively toward Quackos. */
public final class AMATQuackosRailgun {
    private static BulletConfig projectileConfig;

    private AMATQuackosRailgun() {
    }

    /** uses NTM's ordinary Sedna magazine and projectile pipeline for deuterium plasma capsules. */
    public static BulletConfig getBulletConfig() {
        init();
        return projectileConfig;
    }

    /**
     * Identifies the custom AMAT capsule configuration even when a magazine
     * returns a synchronized/re-registered instance with the same config ID.
     */
    public static boolean isRailgunConfig(BulletConfig config) {
        if (config == null) return false;
        init();
        return projectileConfig != null
                && (config == projectileConfig || config.id == projectileConfig.id);
    }

    public static void init() {
        if (projectileConfig != null || ModItems.charge_railgun == null) return;

        projectileConfig = new BulletConfig()
                .setItem(ModItems.charge_railgun)
                .setVel(3.5F)
                .setSpread(0.0F)
                .setDamage(2.0F)
                .setThresholdNegation(0.5F)
                .setArmorPiercing(0.5F)
                .setupDamageClass(DamageResistanceHandler.DamageClass.LASER)
                .setReloadCount(1)
                .setGrav(0.0F)
                .setLife(120)
                .setImpactsEntities(true)
                .setOnUpdate(AMATQuackosRailgun::updateHoming)
                .setOnImpact(AMATQuackosRailgun::handleImpact);
    }

    private static void updateHoming(Entity entity) {
        if (!(entity instanceof EntityBulletBaseMK4) || entity.world.isRemote) return;
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) entity;
        Entity target = projectile.lockonTarget;
        if (!(target instanceof EntityQuackos) || target.isDead || target.world != projectile.world) {
            target = projectile.world.getEntitiesWithinAABB(EntityQuackos.class,
                            projectile.getEntityBoundingBox().grow(128.0D))
                    .stream()
                    .filter(candidate -> !candidate.isDead)
                    .min(Comparator.comparingDouble(projectile::getDistanceSq))
                    .orElse(null);
            projectile.lockonTarget = target;
        }
        if (target == null) return;

        Vec3d destination = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);
        Vec3d direction = destination.subtract(projectile.getPositionVector()).normalize();
        double speed = Math.max(1.0D, projectileConfig == null ? 3.5D : projectileConfig.velocity);
        projectile.motionX = direction.x * speed;
        projectile.motionY = direction.y * speed;
        projectile.motionZ = direction.z * speed;
    }

    private static void handleImpact(EntityBulletBaseMK4 projectile, RayTraceResult result) {
        if (result != null && result.typeOfHit == RayTraceResult.Type.ENTITY) {
            BulletConfig.LAMBDA_STANDARD_ENTITY_HIT.accept(projectile, result);
            if (result.entityHit instanceof EntityQuackos && !projectile.world.isRemote) {
                EntityQuackos target = (EntityQuackos) result.entityHit;
                target.isDead = true;
                target.world.removeEntity(target);
            }
        }
        projectile.setDead();
    }
}
