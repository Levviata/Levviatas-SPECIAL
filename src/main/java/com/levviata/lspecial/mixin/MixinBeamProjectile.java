package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;

import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** records beam-projectile ownership and strengthens the Tau's charged secondary shot. */
@Mixin(value = EntityBulletBeamBase.class, remap = false)
public abstract class MixinBeamProjectile {
    @Shadow public EntityLivingBase thrower;
    @Shadow public BulletConfig config;
    @Shadow public float damage;
    @Unique private float lspecial$damageBeforeImpact;
    @Unique private boolean lspecial$boostedImpact;

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityLivingBase;Lcom/hbm/items/weapon/sedna/BulletConfig;F)V", at = @At("RETURN"), remap = false)
    private void lspecial$recordBeamOwner(EntityLivingBase entity, BulletConfig config, float baseDamage, CallbackInfo callback) {
        lspecial$recordShooter(entity);
    }

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityLivingBase;Lcom/hbm/items/weapon/sedna/BulletConfig;FFDDD)V", at = @At("RETURN"), remap = false)
    private void lspecial$recordBeamOwnerWithOffset(EntityLivingBase entity, BulletConfig config, float baseDamage, float angularInaccuracy, double sideOffset, double heightOffset, double frontOffset, CallbackInfo callback) {
        lspecial$recordShooter(entity);
    }

    @Unique
    private void lspecial$recordShooter(EntityLivingBase shooter) {
        if (shooter == null) return;
        NBTTagCompound data = ((EntityBulletBeamBase) (Object) this).getEntityData();
        data.setUniqueId("LSPECIALShooterUUID", shooter.getUniqueID());
        if (shooter instanceof EntityPlayer) {
            ItemStack held = ((EntityPlayer) shooter).getHeldItemMainhand();
            if (!held.isEmpty() && held.getItem().getRegistryName() != null) {
                data.setString("LSPECIALShooterWeapon", held.getItem().getRegistryName().toString());
            }
        }
    }

    @Inject(method = "onImpact", at = @At("HEAD"), remap = false)
    private void lspecial$boostTauCharge(RayTraceResult mop, CallbackInfo callback) {
        lspecial$boostedImpact = false;
        if (mop == null || mop.typeOfHit != RayTraceResult.Type.ENTITY || mop.entityHit == null
                || ((EntityBulletBeamBase) (Object) this).world.isRemote || !LSPECIALConfig.tauSecondaryDamageBonus
                || config != XFactoryAccelerator.tau_uranium_charge || !(thrower instanceof EntityPlayer)
                || !LSPECIALConfig.intelligence || !LSPECIALConfig.pepperboxPerk
                || SPECIALScoreboard.inst == null || SPECIALScoreboard.inst.getPlayerScore((EntityPlayer) thrower, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) == null
                || SPECIALScoreboard.inst.getPlayerScorePoints((EntityPlayer) thrower, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) < 5) {
            return;
        }

        NBTTagCompound data = ((EntityBulletBeamBase) (Object) this).getEntityData();
        String weaponId = data.getString("LSPECIALShooterWeapon");
        if (weaponId.isEmpty()) {
            ItemStack held = ((EntityPlayer) thrower).getHeldItemMainhand();
            weaponId = held.isEmpty() || held.getItem().getRegistryName() == null
                    ? "" : held.getItem().getRegistryName().toString();
        }
        if (!"hbm:gun_tau".equals(weaponId)) {
            return;
        }

        lspecial$damageBeforeImpact = damage;
        damage = (float) (damage * LSPECIALConfig.tauSecondaryDamageMultiplier);
        lspecial$boostedImpact = true;
    }

    @Inject(method = "onImpact", at = @At("RETURN"), remap = false)
    private void lspecial$restoreTauCharge(RayTraceResult mop, CallbackInfo callback) {
        if (lspecial$boostedImpact) {
            damage = lspecial$damageBeforeImpact;
            lspecial$boostedImpact = false;
        }
    }
}
