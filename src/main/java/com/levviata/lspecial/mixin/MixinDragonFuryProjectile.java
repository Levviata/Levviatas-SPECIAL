package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** applies the Dragon's Fury perk to NTM's existing Topaz flamer projectiles. */
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinDragonFuryProjectile {

    @Shadow public BulletConfig config;
    @Shadow public float damage;

    @Unique private float lspecial$damageBeforeImpact;
    @Unique private boolean lspecial$boostedImpactDamage;

    /** boosts only direct entity impacts, leaving block impacts and NTM's projectile callbacks intact. */
    @Inject(method = "onImpact", at = @At("HEAD"), remap = false)
    private void lspecial$boostDragonFuryDirectHit(RayTraceResult result, CallbackInfo callback) {
        lspecial$boostedImpactDamage = false;
        if (result == null || result.typeOfHit != RayTraceResult.Type.ENTITY || result.entityHit == null
                || ((EntityBulletBaseMK4) (Object) this).world.isRemote
                || !LSPECIALConfig.dragonsFuryDirectHitPerk
                || !lspecial$isTopazFlameConfig(config)
                || !lspecial$hasDragonFuryIntelligence()) {
            return;
        }

        lspecial$damageBeforeImpact = damage;
        damage = (float) (damage * LSPECIALConfig.dragonsFuryDirectHitMultiplier);
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

    /** checks the projectile owner so Dragon's Fury damage only applies to a player's perk. */
    @Unique
    private boolean lspecial$hasDragonFuryIntelligence() {
        EntityLivingBase thrower = ((EntityBulletBaseMK4) (Object) this).getThrower();
        return thrower instanceof EntityPlayer && LSPECIALConfig.intelligence && LSPECIALConfig.revolverPerk
                && SPECIALScoreboard.inst != null && SPECIALScoreboard.inst.getIntelligenceScore() != null
                && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints()
                >= LSPECIALConfig.dragonsFuryDirectHitLevel;
    }
}
