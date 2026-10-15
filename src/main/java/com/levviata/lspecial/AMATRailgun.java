package com.levviata.lspecial;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.DamageResistanceHandler;
import net.minecraft.util.math.RayTraceResult;

/** adds a native Sedna caliber for the BMG that homes exclusively toward Quackos. */
public final class AMATRailgun {
    private static BulletConfig projectileConfig;

    private AMATRailgun() {
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
                .setOnImpact(AMATRailgun::handleImpact);
    }

    private static void handleImpact(EntityBulletBaseMK4 projectile, RayTraceResult result) {
        if (result != null && result.typeOfHit == RayTraceResult.Type.ENTITY) {
            BulletConfig.LAMBDA_STANDARD_ENTITY_HIT.accept(projectile, result);
        }
        projectile.setDead();
    }
}
