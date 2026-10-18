package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.RocketJumpHandler;
import com.levviata.lspecial.StingerDamageHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the rocket-jump perk to NTM's native bullet-based rocket projectiles.
 */
@Mixin(value = EntityBulletBaseNT.class, remap = false)
public abstract class MixinEntityBulletBaseNT {

    @Unique
    private double lspecial$motionX;
    @Unique
    private double lspecial$motionY;
    @Unique
    private double lspecial$motionZ;
    @Unique
    private double lspecial$impactX;
    @Unique
    private double lspecial$impactY;
    @Unique
    private double lspecial$impactZ;
    @Unique
    private boolean lspecial$jumpCandidate;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void lspecial$recordRocketLauncher(CallbackInfo ci) {
        EntityBulletBaseNT bullet = (EntityBulletBaseNT) (Object) this;
        EntityLivingBase owner = bullet.getThrower();
        if (owner instanceof EntityPlayer) {
            String weapon = StingerDamageHandler.getHeldStingerFamilyWeapon((EntityPlayer) owner);
            if (weapon != null) {
                bullet.getEntityData().setUniqueId("LSPECIALShooterUUID", owner.getUniqueID());
                bullet.getEntityData().setString("LSPECIALShooterWeapon", weapon);
                LSPECIALMod.info("[RocketDebug] BulletBaseNT launcher-recorded player={} weapon={} projectileId={}",
                        owner.getName(), weapon, bullet.getEntityId());
            }
        }
    }

    @Inject(method = "onImpact(Lnet/minecraft/util/math/RayTraceResult;)V", at = @At("HEAD"), remap = false)
    private void lspecial$beforeRocketImpact(RayTraceResult result, CallbackInfo ci) {
        EntityBulletBaseNT bullet = (EntityBulletBaseNT) (Object) this;
        Entity owner = bullet.getThrower();
        String weapon = bullet.getEntityData().getString("LSPECIALShooterWeapon");
        boolean family = owner instanceof EntityPlayer && StingerDamageHandler.isStingerFamilyWeapon(weapon);
        if (!family && owner instanceof EntityPlayer) {
            String held = StingerDamageHandler.getHeldStingerFamilyWeapon((EntityPlayer) owner);
            if (held != null) {
                weapon = held;
                family = true;
            }
        }

        lspecial$jumpCandidate = !bullet.world.isRemote && family && bullet.getConfig() != null
                && (bullet.getConfig().explosive > 0.0F || bullet.getConfig().dmgExplosion);
        if (!lspecial$jumpCandidate) return;

        EntityPlayer player = (EntityPlayer) owner;
        lspecial$motionX = player.motionX;
        lspecial$motionY = player.motionY;
        lspecial$motionZ = player.motionZ;
        if (result != null && result.hitVec != null) {
            lspecial$impactX = result.hitVec.x;
            lspecial$impactY = result.hitVec.y;
            lspecial$impactZ = result.hitVec.z;
        } else {
            lspecial$impactX = bullet.posX;
            lspecial$impactY = bullet.posY;
            lspecial$impactZ = bullet.posZ;
        }
        LSPECIALMod.info("[RocketDebug] BulletBaseNT impact HEAD player={} weapon={} projectileId={} configExplosion={} explosive={} impact=({}, {}, {}) perkActive={}",
                player.getName(), weapon, bullet.getEntityId(), bullet.getConfig().dmgExplosion,
                bullet.getConfig().explosive, lspecial$impactX, lspecial$impactY, lspecial$impactZ,
                RocketJumpHandler.isPerkActive(player));
    }

    @Inject(method = "onImpact(Lnet/minecraft/util/math/RayTraceResult;)V", at = @At("RETURN"), remap = false)
    private void lspecial$afterRocketImpact(RayTraceResult result, CallbackInfo ci) {
        if (!lspecial$jumpCandidate) return;
        lspecial$jumpCandidate = false;

        EntityBulletBaseNT bullet = (EntityBulletBaseNT) (Object) this;
        Entity owner = bullet.getThrower();
        if (!(owner instanceof EntityPlayer player)) {
            return;
        }
        boolean active = RocketJumpHandler.isPerkActive(player);
        LSPECIALMod.info("[RocketDebug] BulletBaseNT impact RETURN player={} projectileId={} perkActive={} owner={} motionBefore=({}, {}, {})",
                player.getName(), bullet.getEntityId(), active, player.getUniqueID(),
                lspecial$motionX, lspecial$motionY, lspecial$motionZ);
        if (!active) return;

        double radius = Math.max(2.0D, bullet.getConfig().explosive * 1.5D);
        RocketJumpHandler.applyBlastJump(player, lspecial$impactX, lspecial$impactY, lspecial$impactZ,
                lspecial$motionX, lspecial$motionY, lspecial$motionZ,
                bullet.motionX, bullet.motionY, bullet.motionZ, radius);
    }
}
