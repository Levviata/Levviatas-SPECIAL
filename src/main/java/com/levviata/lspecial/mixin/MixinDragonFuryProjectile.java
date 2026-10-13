package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;

import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** applies flame range and width changes and direct-hit bonuses to NTM projectiles. */
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinDragonFuryProjectile {

    @Shadow public BulletConfig config;
    @Shadow public float damage;

    @Unique private float lspecial$damageBeforeImpact;
    @Unique private boolean lspecial$boostedImpactDamage;
    @Unique private boolean lspecial$rangeInitialized;
    @Unique private double lspecial$startX;
    @Unique private double lspecial$startY;
    @Unique private double lspecial$startZ;
    @Unique private int lspecial$projectileMode;

    /** limits the Dragon's Fury flame and makes the Phlogistinator flame broad and short-ranged. */
    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$applyFlameProjectileShape(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (!lspecial$rangeInitialized) {
            lspecial$rangeInitialized = true;
            lspecial$startX = projectile.posX;
            lspecial$startY = projectile.posY;
            lspecial$startZ = projectile.posZ;
            EntityLivingBase shooter = projectile.getThrower();
            String weaponId = "";
            NBTTagCompound data = projectile.getEntityData();
            if (data.hasKey("LSPECIALShooterWeapon")) {
                weaponId = data.getString("LSPECIALShooterWeapon");
            }
            if (weaponId.isEmpty() && shooter instanceof EntityPlayer && !((EntityPlayer) shooter).getHeldItemMainhand().isEmpty()
                    && ((EntityPlayer) shooter).getHeldItemMainhand().getItem().getRegistryName() != null) {
                weaponId = ((EntityPlayer) shooter).getHeldItemMainhand().getItem().getRegistryName().toString();
            }

            boolean secondaryFire = data.getBoolean("LSPECIALShooterSecondary");
            if (secondaryFire && "hbm:gun_flamer_topaz".equals(weaponId) && LSPECIALConfig.dragonsFuryDirectHitPerk
                    && lspecial$hasIntelligence(LSPECIALConfig.dragonsFuryDirectHitLevel)
                    && lspecial$isTopazFlameConfig(config)) {
                lspecial$projectileMode = 1;
            } else if (secondaryFire && "hbm:gun_flamer".equals(weaponId) && LSPECIALConfig.phlogistinatorSustainedFirePerk
                    && lspecial$hasIntelligence(LSPECIALConfig.phlogistinatorSustainedFireLevel)
                    && lspecial$isStandardFlameConfig(config)) {
                lspecial$projectileMode = 2;
                ((MixinEntityInvoker) (Object) projectile).lspecial$invokeSetSize((float) LSPECIALConfig.phlogSecondaryWidth, projectile.height);
            }
        }

        if (lspecial$projectileMode == 0) {
            return;
        }

        double range = lspecial$projectileMode == 1 ? LSPECIALConfig.dragonsFuryBurstRange : LSPECIALConfig.phlogSecondaryRange;
        double dx = projectile.posX - lspecial$startX;
        double dy = projectile.posY - lspecial$startY;
        double dz = projectile.posZ - lspecial$startZ;
        if (dx * dx + dy * dy + dz * dz > range * range) {
            projectile.setDead();
            callback.cancel();
        }
    }

    /** boosts only direct entity impacts, leaving block impacts and NTM's projectile callbacks intact. */
    @Inject(method = "onImpact", at = @At("HEAD"), remap = false)
    private void lspecial$boostDragonFuryDirectHit(RayTraceResult result, CallbackInfo callback) {
        lspecial$boostedImpactDamage = false;
        if (result == null || result.typeOfHit != RayTraceResult.Type.ENTITY || result.entityHit == null
                || ((EntityBulletBaseMK4) (Object) this).world.isRemote) {
            return;
        }

        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        EntityLivingBase shooter = projectile.getThrower();
        String weaponId = projectile.getEntityData().getString("LSPECIALShooterWeapon");
        if (weaponId.isEmpty() && shooter instanceof EntityPlayer
                && !((EntityPlayer) shooter).getHeldItemMainhand().isEmpty()
                && ((EntityPlayer) shooter).getHeldItemMainhand().getItem().getRegistryName() != null) {
            weaponId = ((EntityPlayer) shooter).getHeldItemMainhand().getItem().getRegistryName().toString();
        }

        double multiplier = 1.0D;
        if (projectile.getEntityData().getBoolean("LSPECIALShooterSecondary")
                && "hbm:gun_flamer_topaz".equals(weaponId) && LSPECIALConfig.dragonsFuryDirectHitPerk
                && lspecial$isTopazFlameConfig(config)
                && lspecial$hasIntelligence(LSPECIALConfig.dragonsFuryDirectHitLevel)) {
            multiplier = LSPECIALConfig.dragonsFuryDirectHitMultiplier;
        } else if (projectile.getEntityData().getBoolean("LSPECIALShooterSecondary")
                && "hbm:gun_tau".equals(weaponId) && LSPECIALConfig.tauSecondaryDamageBonus
                && config == XFactoryAccelerator.tau_uranium_charge && lspecial$hasIntelligence(5)) {
            multiplier = LSPECIALConfig.tauSecondaryDamageMultiplier;
        }
        if (multiplier <= 1.0D){
            return;
        }

        lspecial$damageBeforeImpact = damage;
        damage = (float) (damage * multiplier);
        lspecial$boostedImpactDamage = true;
    }

    /** restores the projectile's base damage after NTM finishes the impact callback. */
    @Inject(method = "onImpact", at = @At("RETURN"), remap = false)
    private void lspecial$restoreDragonFuryDamage(RayTraceResult result, CallbackInfo callback) {
        if (lspecial$boostedImpactDamage) {
            damage = lspecial$damageBeforeImpact;
            lspecial$boostedImpactDamage = false;
        }
    }

    /** checks the Topaz flamer's exact native bullet configurations instead of guessing from projectile names. */
    @Unique
    private static boolean lspecial$isTopazFlameConfig(BulletConfig bulletConfig) {
        return bulletConfig != null && (bulletConfig == XFactoryFlamer.flame_topaz_diesel
                || bulletConfig == XFactoryFlamer.flame_topaz_gas
                || bulletConfig == XFactoryFlamer.flame_topaz_napalm
                || bulletConfig == XFactoryFlamer.flame_topaz_balefire);
    }

    /** checks the active player's Intelligence perk before changing projectile behavior. */
    @Unique
    private boolean lspecial$hasIntelligence(int level) {
        EntityLivingBase shooter = ((EntityBulletBaseMK4) (Object) this).getThrower();
        return shooter instanceof EntityPlayer && LSPECIALConfig.intelligence
                && (level >= 15 ? LSPECIALConfig.revolverPerk : LSPECIALConfig.pepperboxPerk)
                && SPECIALScoreboard.inst != null && SPECIALScoreboard.inst.getIntelligenceScore() != null
                && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() >= level;
    }

    @Unique
    private static boolean lspecial$isStandardFlameConfig(BulletConfig bulletConfig) {
        return bulletConfig != null && (bulletConfig == XFactoryFlamer.flame_diesel
                || bulletConfig == XFactoryFlamer.flame_gas || bulletConfig == XFactoryFlamer.flame_napalm
                || bulletConfig == XFactoryFlamer.flame_balefire);
    }
}
