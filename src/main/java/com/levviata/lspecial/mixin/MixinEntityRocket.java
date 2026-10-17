package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityRocket;
import com.hbm.explosion.ExplosionLarge;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.RocketJumpHandler;
import com.levviata.lspecial.StingerDamageHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/** records the launcher's identity and replaces owner blast propulsion with the Intelligence rocket-jump perk. */
@Mixin(value = EntityRocket.class, remap = false)
public abstract class MixinEntityRocket {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/EntityLivingBase;FF)V",
            at = @At("RETURN"), remap = false)
    private void lspecial$recordTargetedRocketLauncher(World world, EntityLivingBase shooter,
                                                        EntityLivingBase target, float velocity,
                                                        float inaccuracy, CallbackInfo callback) {
        lspecial$recordLauncher(shooter);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;FLnet/minecraft/util/EnumHand;)V",
            at = @At("RETURN"), remap = false)
    private void lspecial$recordHandRocketLauncher(World world, EntityLivingBase shooter,
                                                   float velocity, EnumHand hand, CallbackInfo callback) {
        lspecial$recordLauncher(shooter);
    }

    private void lspecial$recordLauncher(EntityLivingBase shooter) {
        if (!(shooter instanceof EntityPlayer)) return;
        String weapon = StingerDamageHandler.getHeldStingerFamilyWeapon((EntityPlayer) shooter);
        if (weapon == null) return;
        EntityRocket rocket = (EntityRocket) (Object) this;
        rocket.getEntityData().setUniqueId("LSPECIALShooterUUID", shooter.getUniqueID());
        rocket.getEntityData().setString("LSPECIALShooterWeapon", weapon);
    }

    @Redirect(method = "onUpdate()V",
            at = @At(value = "INVOKE",
                    target = "Lcom/hbm/explosion/ExplosionLarge;explode(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;DDDFZZZ)V"),
            remap = false)
    private void lspecial$replaceRocketOwnerPropulsion(World world, Entity explosionSource,
                                                        double x, double y, double z, float size,
                                                        boolean smoke, boolean rubble, boolean shrapnel) {
        EntityRocket rocket = (EntityRocket) (Object) this;
        Entity shooter = rocket.shootingEntity;
        String weapon = rocket.getEntityData().getString("LSPECIALShooterWeapon");
        boolean familyRocket = StingerDamageHandler.isStingerFamilyWeapon(weapon)
                && shooter instanceof EntityPlayer;
        EntityPlayer player = familyRocket ? (EntityPlayer) shooter : null;
        boolean rocketJump = player != null && RocketJumpHandler.isPerkActive(player);

        double previousMotionX = player == null ? 0.0D : player.motionX;
        double previousMotionY = player == null ? 0.0D : player.motionY;
        double previousMotionZ = player == null ? 0.0D : player.motionZ;

        if (familyRocket && LSPECIALConfig.stingerSelfDamageProtection) {
            StingerDamageHandler.beginRocketExplosion(player, weapon);
        }
        try {
            ExplosionLarge.explode(world, explosionSource, x, y, z, size, smoke, rubble, shrapnel);
        } finally {
            if (familyRocket && LSPECIALConfig.stingerSelfDamageProtection) {
                StingerDamageHandler.endRocketExplosion();
            }
        }

        if (!rocketJump) return;

        RocketJumpHandler.applyBlastJump(player, x, y, z,
                previousMotionX, previousMotionY, previousMotionZ,
                rocket.motionX, rocket.motionY, rocket.motionZ,
                Math.max(2.0D, rocket.explosionSize * 1.5D));
    }

}
