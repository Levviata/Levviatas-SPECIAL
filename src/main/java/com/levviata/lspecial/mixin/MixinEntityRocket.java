package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityRocket;
import com.hbm.explosion.ExplosionLarge;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.RocketJumpHandler;
import com.levviata.lspecial.StingerDamageHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private void lspecial$recordLauncher(EntityLivingBase shooter) {
        EntityRocket rocket = (EntityRocket) (Object) this;
        if (!(shooter instanceof EntityPlayer)) {
            LSPECIALMod.info("[RocketDebug] EntityRocket constructor shooter is not a player: rocket={} shooter={}",
                    rocket.getEntityId(), shooter == null ? "null" : shooter.getClass().getName());
            return;
        }
        EntityPlayer player = (EntityPlayer) shooter;
        String weapon = StingerDamageHandler.getHeldStingerFamilyWeapon(player);
        if (weapon == null) {
            LSPECIALMod.info("[RocketDebug] EntityRocket launcher NOT recorded: rocket={} player={} heldMain={} heldOff={}",
                    rocket.getEntityId(), player.getName(), player.getHeldItemMainhand(), player.getHeldItemOffhand());
            return;
        }
        rocket.getEntityData().setUniqueId("LSPECIALShooterUUID", shooter.getUniqueID());
        rocket.getEntityData().setString("LSPECIALShooterWeapon", weapon);
        LSPECIALMod.info("[RocketDebug] EntityRocket launcher RECORDED rocket={} player={} weapon={} ctor={}",
                rocket.getEntityId(), player.getName(), weapon, rocket.getClass().getName());
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
        boolean rocketJump = RocketJumpHandler.isPerkActive(player);
        LSPECIALMod.info("[RocketDebug] EntityRocket explosion redirect rocket={} shooter={} weapon={} familyRocket={} selfDamageProtection={} rocketJumpPerk={} source={} pos=({}, {}, {}) size={} explosionSize={}",
                rocket.getEntityId(), shooter == null ? "null" : shooter.getName(), weapon, familyRocket,
                LSPECIALConfig.stingerSelfDamageProtection, rocketJump, 
                explosionSource == null ? "null" : explosionSource.getClass().getName(), x, y, z, size, rocket.explosionSize);

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

        if (!rocketJump) {
            LSPECIALMod.info("[RocketDebug] EntityRocket rocket jump skipped rocket={} player={} (familyRocket={}, perkActive={})",
                    rocket.getEntityId(), player == null ? "null" : player.getName(), familyRocket, rocketJump);
            return;
        }

        LSPECIALMod.info("[RocketDebug] EntityRocket applying rocket jump rocket={} player={} previousMotion=({}, {}, {}) postExplosionMotion=({}, {}, {})",
                rocket.getEntityId(), player.getName(), previousMotionX, previousMotionY, previousMotionZ,
                player.motionX, player.motionY, player.motionZ);
        RocketJumpHandler.applyBlastJump(player, x, y, z,
                previousMotionX, previousMotionY, previousMotionZ,
                rocket.motionX, rocket.motionY, rocket.motionZ,
                Math.max(2.0D, rocket.explosionSize * 1.5D));
    }

}
