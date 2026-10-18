package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.RocketJumpHandler;
import com.levviata.lspecial.StingerDamageHandler;
import com.levviata.lspecial.SPECIALScoreboard;
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
 * Records configured missile-family impacts and applies their Intelligence rocket-jump behavior.
 */
@Mixin(value = EntityBulletBaseNT.class, remap = false)
public abstract class MixinStingerProjectile {

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

    @Inject(method = "<init>(Lnet/minecraft/world/World;ILnet/minecraft/entity/EntityLivingBase;)V",
            at = @At("RETURN"), remap = false)
    private void lspecial$recordLauncherConstructor(net.minecraft.world.World world, int configId,
                                                    EntityLivingBase shooter, CallbackInfo callback) {
        lspecial$recordLauncher(shooter, "constructor-config");
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;ILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/EntityLivingBase;FF)V",
            at = @At("RETURN"), remap = false)
    private void lspecial$recordTargetedLauncherConstructor(net.minecraft.world.World world, int configId,
                                                            EntityLivingBase shooter, EntityLivingBase target,
                                                            float velocity, float inaccuracy, CallbackInfo callback) {
        lspecial$recordLauncher(shooter, "constructor-targeted");
    }

    @Unique
    private void lspecial$recordLauncher(EntityLivingBase shooter, String path) {
        if (!(shooter instanceof EntityPlayer player)) {
            LSPECIALMod.info("[RocketDebug] BulletNT launcher-record skipped path={} shooter={}", path,
                    shooter == null ? "null" : shooter.getClass().getName());
            return;
        }
        String weapon = StingerDamageHandler.getHeldStingerFamilyWeapon(player);
        EntityBulletBaseNT projectile = (EntityBulletBaseNT) (Object) this;
        if (weapon == null) {
            LSPECIALMod.info("[RocketDebug] BulletNT launcher-record FAILED path={} player={} mainhand={} offhand={} projectileId={} config={}",
                    path, player.getName(), registryName(player.getHeldItemMainhand()),
                    registryName(player.getHeldItemOffhand()), projectile.getEntityId(), projectile.getConfig());
            return;
        }
        projectile.getEntityData().setUniqueId("LSPECIALShooterUUID", player.getUniqueID());
        projectile.getEntityData().setString("LSPECIALShooterWeapon", weapon);
        LSPECIALMod.info("[RocketDebug] BulletNT launcher-recorded path={} player={} weapon={} projectileId={} config={}",
                path, player.getName(), weapon, projectile.getEntityId(), projectile.getConfig());
    }

    @Unique
    private static String registryName(net.minecraft.item.ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null
                ? "empty" : stack.getItem().getRegistryName().toString();
    }

    /**
     * Wrap the full NTM impact, because both block and entity hits route through onImpact.
     */
    @Inject(method = "onImpact(Lnet/minecraft/util/math/RayTraceResult;)V", at = @At("HEAD"), remap = false)
    private void lspecial$beginRocketJumpImpact(RayTraceResult hit, CallbackInfo callback) {
        EntityBulletBaseNT projectile = (EntityBulletBaseNT) (Object) this;
        EntityLivingBase shooter = projectile.getThrower();
        NBTTagCompound data = projectile.getEntityData();
        String weapon = data.hasKey("LSPECIALShooterWeapon") ? data.getString("LSPECIALShooterWeapon") : null;
        boolean family = StingerDamageHandler.isStingerFamilyWeapon(weapon);
        LSPECIALMod.info("[RocketDebug] BulletNT impact HEAD projectileId={} hitType={} owner={} weapon={} family={} recorded={} remote={} pos=({}, {}, {})",
                projectile.getEntityId(), hit == null ? "null" : hit.typeOfHit,
                shooter == null ? "null" : shooter.getName(), weapon, family,
                StingerDamageHandler.isLauncherRecorded(projectile), projectile.world.isRemote,
                projectile.posX, projectile.posY, projectile.posZ);
        if (projectile.world.isRemote || !(shooter instanceof EntityPlayer player) || !family) return;

        lspecial$rocketJumpOwner = player;
        lspecial$rocketJumpWeapon = weapon;
        lspecial$previousMotionX = player.motionX;
        lspecial$previousMotionY = player.motionY;
        lspecial$previousMotionZ = player.motionZ;
        Vec3d hitVec = hit == null ? null : hit.hitVec;
        lspecial$explosionX = hitVec == null ? projectile.posX : hitVec.x;
        lspecial$explosionY = hitVec == null ? projectile.posY : hitVec.y;
        lspecial$explosionZ = hitVec == null ? projectile.posZ : hitVec.z;
        lspecial$fallbackMotionX = projectile.motionX;
        lspecial$fallbackMotionY = projectile.motionY;
        lspecial$fallbackMotionZ = projectile.motionZ;

        if (LSPECIALConfig.stingerSelfDamageProtection) {
            StingerDamageHandler.beginRocketExplosion(player, weapon);
            LSPECIALMod.info("[RocketDebug] BulletNT explosion context BEGIN player={} weapon={} projectileId={}",
                    player.getName(), weapon, projectile.getEntityId());
        }
    }

    @Inject(method = "onImpact(Lnet/minecraft/util/math/RayTraceResult;)V", at = @At("RETURN"), remap = false)
    private void lspecial$finishRocketJumpImpact(RayTraceResult hit, CallbackInfo callback) {
        EntityPlayer player = lspecial$rocketJumpOwner;
        if (player == null) return;

        if (LSPECIALConfig.stingerSelfDamageProtection) {
            StingerDamageHandler.endRocketExplosion();
            LSPECIALMod.info("[RocketDebug] BulletNT explosion context END player={} weapon={}",
                    player.getName(), lspecial$rocketJumpWeapon);
        }

        int intelligence = SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD);
        boolean perkActive = RocketJumpHandler.isPerkActive(player);
        LSPECIALMod.info("[RocketDebug] BulletNT rocket-jump evaluation player={} weapon={} intelligence={} minimum={} enabled={} perkConfig={} active={} impact=({}, {}, {}) motionBefore=({}, {}, {})",
                player.getName(), lspecial$rocketJumpWeapon, intelligence, LSPECIALConfig.rocketJumpLesserPerkLevel,
                LSPECIALConfig.intelligence, LSPECIALConfig.rocketJumpLesserPerk, perkActive,
                lspecial$explosionX, lspecial$explosionY, lspecial$explosionZ,
                lspecial$previousMotionX, lspecial$previousMotionY, lspecial$previousMotionZ);
        if (perkActive) {
            RocketJumpHandler.applyBlastJump(player,
                    lspecial$explosionX, lspecial$explosionY, lspecial$explosionZ,
                    lspecial$previousMotionX, lspecial$previousMotionY, lspecial$previousMotionZ,
                    lspecial$fallbackMotionX, lspecial$fallbackMotionY, lspecial$fallbackMotionZ,
                    7.5D);
        }
        lspecial$rocketJumpOwner = null;
        lspecial$rocketJumpWeapon = null;
    }
}
