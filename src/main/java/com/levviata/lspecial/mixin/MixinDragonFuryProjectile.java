package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ports the defining Dragon's Fury hit rules onto NTM's native fireball projectile:
 * 3x impact damage against an already-burning target, brief ignition on hit, and
 * faster pressure recovery after a successful hit. NTM still owns collision and rendering.
 */
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinDragonFuryProjectile {
    @Shadow public BulletConfig config;
    @Shadow public float damage;

    @Unique private float lspecial$damageBeforeImpact;
    @Unique private boolean lspecial$boostedImpactDamage;
    @Unique private EntityLivingBase lspecial$igniteAfterImpact;

    @Inject(method = "onUpdate", at = @At("HEAD"), remap = false)
    private void lspecial$renderDragonFuryTrail(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (!projectile.world.isRemote
                || !"hbm:gun_flamer_topaz".equals(projectile.getEntityData().getString("LSPECIALShooterWeapon"))
                || !lspecial$isDragonFuryFlame(config)) return;
        for (int i = 0; i < 3; i++) {
            double spreadX = (projectile.world.rand.nextDouble() - 0.5D) * 0.12D;
            double spreadY = (projectile.world.rand.nextDouble() - 0.5D) * 0.12D;
            double spreadZ = (projectile.world.rand.nextDouble() - 0.5D) * 0.12D;
            projectile.world.spawnParticle(EnumParticleTypes.FLAME,
                    projectile.posX, projectile.posY, projectile.posZ,
                    -projectile.motionX * 0.025D + spreadX,
                    -projectile.motionY * 0.025D + spreadY,
                    -projectile.motionZ * 0.025D + spreadZ);
        }
    }

    @Inject(method = "onImpact", at = @At("HEAD"), remap = false)
    private void lspecial$applyDragonFuryHitRules(RayTraceResult hit, CallbackInfo callback) {
        lspecial$boostedImpactDamage = false;
        lspecial$igniteAfterImpact = null;
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (projectile.world.isRemote || hit == null || hit.typeOfHit != RayTraceResult.Type.ENTITY
                || !(hit.entityHit instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) hit.entityHit;
        String weaponId = projectile.getEntityData().getString("LSPECIALShooterWeapon");
        if ("hbm:gun_flamer_topaz".equals(weaponId) && lspecial$isDragonFuryFlame(config)) {
            lspecial$igniteAfterImpact = target;
            if (target.isBurning()) {
                lspecial$damageBeforeImpact = damage;
                damage = damage * 3.0F;
                lspecial$boostedImpactDamage = true;
            }

            EntityLivingBase shooter = projectile.getThrower();
            if (shooter instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) shooter;
                ItemStack held = player.getHeldItemMainhand();
                if (!held.isEmpty() && held.getItem().getRegistryName() != null
                        && "hbm:gun_flamer_topaz".equals(held.getItem().getRegistryName().toString())) {
                    net.minecraft.nbt.NBTTagCompound tag = held.getTagCompound();
                    if (tag == null) {
                        tag = new net.minecraft.nbt.NBTTagCompound();
                        held.setTagCompound(tag);
                    }
                    tag.setBoolean("LSPECIALDragonFuryNextShotFast", true);
                }
            }
            return;
        }

        // Preserve the unrelated LSPECIAL Tau charged-shot bonus.
        if ("hbm:gun_tau".equals(weaponId) && projectile.getEntityData().getBoolean("LSPECIALShooterSecondary")
                && LSPECIALConfig.tauSecondaryDamageBonus && config == XFactoryAccelerator.tau_uranium_charge) {
            lspecial$damageBeforeImpact = damage;
            damage = (float) (damage * LSPECIALConfig.tauSecondaryDamageMultiplier);
            lspecial$boostedImpactDamage = true;
        }
    }

    @Inject(method = "onImpact", at = @At("RETURN"), remap = false)
    private void lspecial$finishDragonFuryHitRules(RayTraceResult hit, CallbackInfo callback) {
        if (lspecial$boostedImpactDamage) {
            damage = lspecial$damageBeforeImpact;
            lspecial$boostedImpactDamage = false;
        }
        if (lspecial$igniteAfterImpact != null && !lspecial$igniteAfterImpact.isDead) {
            lspecial$igniteAfterImpact.setFire(3);
            lspecial$igniteAfterImpact = null;
        }
    }

    @Unique
    private static boolean lspecial$isDragonFuryFlame(BulletConfig bulletConfig) {
        return bulletConfig != null && (bulletConfig == XFactoryFlamer.flame_topaz_diesel
                || bulletConfig == XFactoryFlamer.flame_topaz_gas
                || bulletConfig == XFactoryFlamer.flame_topaz_napalm
                || bulletConfig == XFactoryFlamer.flame_topaz_balefire);
    }
}
