package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.RocketJumpHandler;
import com.levviata.lspecial.StingerDamageHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** records configured missile-family impacts and applies their Intelligence rocket-jump behavior. */
@Mixin(value = EntityBulletBaseNT.class, remap = false)
public abstract class MixinStingerProjectile {

    @Unique private EntityPlayer lspecial$rocketJumpOwner;
    @Unique private double lspecial$previousMotionX;
    @Unique private double lspecial$previousMotionY;
    @Unique private double lspecial$previousMotionZ;
    @Unique private double lspecial$explosionX;
    @Unique private double lspecial$explosionY;
    @Unique private double lspecial$explosionZ;
    @Unique private double lspecial$fallbackMotionX;
    @Unique private double lspecial$fallbackMotionY;
    @Unique private double lspecial$fallbackMotionZ;

    @Inject(method = "<init>(Lnet/minecraft/world/World;ILnet/minecraft/entity/EntityLivingBase;)V",
            at = @At("RETURN"), remap = false)
    private void lspecial$recordLauncherConstructor(net.minecraft.world.World world, int configId,
                                                     EntityLivingBase shooter, CallbackInfo callback) {
        lspecial$recordLauncher(shooter);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;ILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/EntityLivingBase;FF)V",
            at = @At("RETURN"), remap = false)
    private void lspecial$recordTargetedLauncherConstructor(net.minecraft.world.World world, int configId,
                                                             EntityLivingBase shooter, EntityLivingBase target,
                                                             float velocity, float inaccuracy, CallbackInfo callback) {
        lspecial$recordLauncher(shooter);
    }

    @Unique
    private void lspecial$recordLauncher(EntityLivingBase shooter) {
        if (!(shooter instanceof EntityPlayer)) return;
        String weapon = StingerDamageHandler.getHeldStingerFamilyWeapon((EntityPlayer) shooter);
        if (weapon == null) return;
        EntityBulletBaseNT projectile = (EntityBulletBaseNT) (Object) this;
        projectile.getEntityData().setUniqueId("LSPECIALShooterUUID", shooter.getUniqueID());
        projectile.getEntityData().setString("LSPECIALShooterWeapon", weapon);
    }

    /** tracks the launcher's blast while NTM applies its impact behavior and explosion knockback. */
    @Inject(method = "onBlockImpact(IIII)V", at = @At("HEAD"), remap = false)
    private void lspecial$beginRocketJumpImpact(int x, int y, int z, int side, CallbackInfo callback) {
        EntityBulletBaseNT projectile = (EntityBulletBaseNT) (Object) this;
        EntityLivingBase shooter = projectile.getThrower();
        if (projectile.world.isRemote || !(shooter instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) shooter;
        NBTTagCompound data = projectile.getEntityData();
        String weapon = data.hasKey("LSPECIALShooterWeapon") ? data.getString("LSPECIALShooterWeapon") : null;
        if (!StingerDamageHandler.isStingerFamilyWeapon(weapon)) return;

        lspecial$rocketJumpOwner = player;
        lspecial$previousMotionX = player.motionX;
        lspecial$previousMotionY = player.motionY;
        lspecial$previousMotionZ = player.motionZ;
        lspecial$explosionX = projectile.posX;
        lspecial$explosionY = projectile.posY;
        lspecial$explosionZ = projectile.posZ;
        lspecial$fallbackMotionX = projectile.motionX;
        lspecial$fallbackMotionY = projectile.motionY;
        lspecial$fallbackMotionZ = projectile.motionZ;

        if (LSPECIALConfig.stingerSelfDamageProtection) {
            StingerDamageHandler.beginRocketExplosion(player, weapon);
        }
    }

    /** clears the damage context and applies the replacement TF2-style impulse after the NTM impact behavior. */
    @Inject(method = "onBlockImpact(IIII)V", at = @At("RETURN"), remap = false)
    private void lspecial$finishRocketJumpImpact(int x, int y, int z, int side, CallbackInfo callback) {
        EntityPlayer player = lspecial$rocketJumpOwner;
        if (player == null) return;

        if (LSPECIALConfig.stingerSelfDamageProtection) {
            StingerDamageHandler.endRocketExplosion();
        }
        if (RocketJumpHandler.isPerkActive(player)) {
            RocketJumpHandler.applyBlastJump(player,
                    lspecial$explosionX, lspecial$explosionY, lspecial$explosionZ,
                    lspecial$previousMotionX, lspecial$previousMotionY, lspecial$previousMotionZ,
                    lspecial$fallbackMotionX, lspecial$fallbackMotionY, lspecial$fallbackMotionZ,
                    7.5D);
        }
        lspecial$rocketJumpOwner = null;
    }

}
