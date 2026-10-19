package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.RocketJumpHandler;
import com.levviata.lspecial.RocketDamageHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Connects Sedna rocket-family bullets, to the Intelligence rocket-jump perk.
 */
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinRocketJumpBulletMK4 {

    @Unique
    private EntityPlayer lspecial$rocketJumpOwner;
    @Unique
    private String lspecial$rocketJumpWeapon;
    @Unique
    private double lspecial$previousMotionX;
    @Unique
    private double lspecial$previousMotionY;
    @Unique
    private double lspecial$previousMotionZ;
    @Unique
    private double lspecial$explosionX;
    @Unique
    private double lspecial$explosionY;
    @Unique
    private double lspecial$explosionZ;
    @Unique
    private double lspecial$fallbackMotionX;
    @Unique
    private double lspecial$fallbackMotionY;
    @Unique
    private double lspecial$fallbackMotionZ;

    @Unique
    private static String lspecial$registryName(net.minecraft.item.ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null
                ? "empty" : stack.getItem().getRegistryName().toString();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;Lcom/hbm/items/weapon/sedna/BulletConfig;FFDDDDDD)V",
            at = @At("RETURN"), remap = false)
    private void lspecial$recordWorldShooterConstructor(net.minecraft.world.World world, EntityLivingBase shooter, com.hbm.items.weapon.sedna.BulletConfig config, float yaw, float pitch,
                                                        double x, double y, double z, double velocityX, double velocityY, double velocityZ,
                                                        CallbackInfo callback) {
        lspecial$recordLauncher(shooter, "world-shooter-constructor");
    }

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityLivingBase;Lcom/hbm/items/weapon/sedna/BulletConfig;FFDDD)V",
            at = @At("RETURN"), remap = false)
    private void lspecial$recordShooterConstructor(EntityLivingBase shooter,
                                                   com.hbm.items.weapon.sedna.BulletConfig config, float yaw, float pitch,
                                                   double velocityX, double velocityY, double velocityZ, CallbackInfo callback) {
        lspecial$recordLauncher(shooter, "shooter-constructor");
    }

    @Unique
    private void lspecial$recordLauncher(EntityLivingBase shooter, String path) {
        if (!(shooter instanceof EntityPlayer player)) return;
        String weapon = RocketDamageHandler.getHeldStingerFamilyWeapon(player);
        if (weapon == null) {
            LSPECIALMod.info("RocketDebug", "BulletMK4 launcher-record FAILED path={} player={} mainhand={} offhand={} projectileId={}",
                    path, player.getName(), lspecial$registryName(player.getHeldItemMainhand()),
                    lspecial$registryName(player.getHeldItemOffhand()), ((EntityBulletBaseMK4) (Object) this).getEntityId());
            return;
        }
        EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) (Object) this;
        bullet.getEntityData().setUniqueId("LSPECIALShooterUUID", player.getUniqueID());
        bullet.getEntityData().setString("LSPECIALShooterWeapon", weapon);
        LSPECIALMod.info("RocketDebug", "BulletMK4 launcher-recorded path={} player={} weapon={} projectileId={} config={}",
                path, player.getName(), weapon, bullet.getEntityId(), bullet.getBulletConfig());
    }

    @Inject(method = "onImpact(Lnet/minecraft/util/math/RayTraceResult;)V", at = @At("HEAD"), remap = false)
    private void lspecial$beginRocketJumpImpact(RayTraceResult hit, CallbackInfo callback) {
        EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) (Object) this;
        if (bullet.world.isRemote) return;

        // NTM's Quadro impact callback intentionally ignores entity collisions during its first three ticks.
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY && bullet.ticksExisted < 3) {
            LSPECIALMod.info("RocketDebug", "BulletMK4 impact ignored early entity hit projectileId={} ticks={}",
                    bullet.getEntityId(), bullet.ticksExisted);
            return;
        }

        EntityLivingBase shooter = bullet.getThrower();
        NBTTagCompound data = bullet.getEntityData();
        String weapon = data.hasKey("LSPECIALShooterWeapon") ? data.getString("LSPECIALShooterWeapon") : null;
        boolean family = RocketDamageHandler.isStingerFamilyWeapon(weapon);
        LSPECIALMod.info("RocketDebug", "BulletMK4 impact HEAD projectileId={} hitType={} owner={} weapon={} family={} recorded={} remote={} pos=({}, {}, {})",
                bullet.getEntityId(), hit == null ? "null" : hit.typeOfHit,
                shooter == null ? "null" : shooter.getName(), weapon, family,
                data.hasKey("LSPECIALShooterWeapon"), bullet.world.isRemote,
                bullet.posX, bullet.posY, bullet.posZ);
        if (!(shooter instanceof EntityPlayer player) || !family) return;

        lspecial$rocketJumpOwner = player;
        lspecial$rocketJumpWeapon = weapon;
        lspecial$previousMotionX = player.motionX;
        lspecial$previousMotionY = player.motionY;
        lspecial$previousMotionZ = player.motionZ;
        Vec3d hitVec = hit == null ? null : hit.hitVec;
        lspecial$explosionX = hitVec == null ? bullet.posX : hitVec.x;
        lspecial$explosionY = hitVec == null ? bullet.posY : hitVec.y;
        lspecial$explosionZ = hitVec == null ? bullet.posZ : hitVec.z;
        lspecial$fallbackMotionX = bullet.motionX;
        lspecial$fallbackMotionY = bullet.motionY;
        lspecial$fallbackMotionZ = bullet.motionZ;

        if (LSPECIALConfig.stingerSelfDamageProtection) {
            RocketDamageHandler.beginRocketExplosion(player, weapon);
            LSPECIALMod.info("RocketDebug", "BulletMK4 explosion context BEGIN player={} weapon={} projectileId={}",
                    player.getName(), weapon, bullet.getEntityId());
        }
    }

    @Inject(method = "onImpact(Lnet/minecraft/util/math/RayTraceResult;)V", at = @At("RETURN"), remap = false)
    private void lspecial$finishRocketJumpImpact(RayTraceResult hit, CallbackInfo callback) {
        EntityPlayer player = lspecial$rocketJumpOwner;
        if (player == null) return;

        if (LSPECIALConfig.stingerSelfDamageProtection) {
            RocketDamageHandler.endRocketExplosion();
            LSPECIALMod.info("RocketDebug", "BulletMK4 explosion context END player={} weapon={}",
                    player.getName(), lspecial$rocketJumpWeapon);
        }

        boolean perkActive = RocketJumpHandler.isPerkActive(player);
        LSPECIALMod.info("RocketDebug", "BulletMK4 rocket-jump evaluation player={} weapon={} active={} impact=({}, {}, {}) motionBefore=({}, {}, {})",
                player.getName(), lspecial$rocketJumpWeapon, perkActive,
                lspecial$explosionX, lspecial$explosionY, lspecial$explosionZ,
                lspecial$previousMotionX, lspecial$previousMotionY, lspecial$previousMotionZ);
        if (perkActive) {
            // The Quadro's NTM explosion is configured with a radius of 5 blocks.
            RocketJumpHandler.applyBlastJump(player,
                    lspecial$explosionX, lspecial$explosionY, lspecial$explosionZ,
                    lspecial$previousMotionX, lspecial$previousMotionY, lspecial$previousMotionZ,
                    lspecial$fallbackMotionX, lspecial$fallbackMotionY, lspecial$fallbackMotionZ,
                    5.0D);
        }
        lspecial$rocketJumpOwner = null;
        lspecial$rocketJumpWeapon = null;
    }
}
