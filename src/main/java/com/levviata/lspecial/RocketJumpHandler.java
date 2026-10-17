package com.levviata.lspecial;

import com.hbm.items.weapon.ItemGunBase;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared TF2-style blast-jump behavior for NTM rocket projectile implementations. */
public final class RocketJumpHandler {
    private static final String ROCKET_JUMPING = "LSPECIALRocketJumping";
    private static final Map<UUID, Integer> LAST_REWARD_TICK = new HashMap<>();

    private RocketJumpHandler() {
    }

    /** checks the Intelligence 5 lesser-perk gate for the supplied player's own scoreboard. */
    public static boolean isPerkActive(EntityPlayer player) {
        if (player == null || SPECIALScoreboard.inst == null
                || SPECIALScoreboard.inst.getIntelligenceScore() == null) return false;
        int intelligence = SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD);
        boolean active = LSPECIALConfig.intelligence && LSPECIALConfig.rocketJumpLesserPerk
                && intelligence >= LSPECIALConfig.rocketJumpLesserPerkLevel;
        LSPECIALMod.info("[RocketDebug] perk-check player={} intelligence={} minimum={} intelligenceEnabled={} perkEnabled={} active={}",
                player.getName(), intelligence, LSPECIALConfig.rocketJumpLesserPerkLevel,
                LSPECIALConfig.intelligence, LSPECIALConfig.rocketJumpLesserPerk, active);
        return active;
    }

    /** replaces the native owner knockback with a radial impulse biased upward, preserving existing momentum. */
    public static void applyBlastJump(EntityPlayer player, double explosionX, double explosionY, double explosionZ,
                                      double previousMotionX, double previousMotionY, double previousMotionZ,
                                      double fallbackMotionX, double fallbackMotionY, double fallbackMotionZ,
                                      double blastRadius) {
        if (player == null || player.world.isRemote || player.isDead) {
            LSPECIALMod.info("[RocketDebug] impulse skipped player={} remote={} dead={}",
                    player == null ? "null" : player.getName(), player != null && player.world.isRemote,
                    player != null && player.isDead);
            return;
        }

        double dx = player.posX - explosionX;
        double dy = player.posY + player.height * 0.5D - explosionY;
        double dz = player.posZ - explosionZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double distanceToBlast = distance;
        blastRadius = Math.max(1.0D, blastRadius);

        // Only replace vanilla propulsion when the owner is inside the effective blast radius.
        if (distanceToBlast > blastRadius) {
            LSPECIALMod.info("[RocketDebug] impulse skipped outside-radius player={} distance={} radius={} explosion=({}, {}, {}) playerPos=({}, {}, {})",
                    player.getName(), distanceToBlast, blastRadius, explosionX, explosionY, explosionZ,
                    player.posX, player.posY, player.posZ);
            return;
        }

        player.motionX = previousMotionX;
        player.motionY = previousMotionY;
        player.motionZ = previousMotionZ;

        if (distance < 1.0E-4D) {
            dx = -fallbackMotionX;
            dy = Math.max(0.0D, -fallbackMotionY);
            dz = -fallbackMotionZ;
            distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        if (distance < 1.0E-4D) {
            dx = 0.0D;
            dy = 1.0D;
            dz = 0.0D;
            distance = 1.0D;
        }

        dx /= distance;
        dy = dy / distance + LSPECIALConfig.rocketJumpVerticalBias;
        dz /= distance;
        double directionLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (directionLength > 1.0E-4D) {
            dx /= directionLength;
            dy /= directionLength;
            dz /= directionLength;
        }

        double falloff = Math.max(0.0D, Math.min(1.0D, 1.0D - distanceToBlast / blastRadius));
        double impulse = LSPECIALConfig.rocketJumpStrength * (0.5D + 0.5D * falloff);

        player.motionX += dx * impulse;
        player.motionY += dy * impulse;
        player.motionZ += dz * impulse;
        Vec3d look = player.getLookVec();
        double lookLength = Math.sqrt(look.x * look.x + look.z * look.z);
        double horizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        if (lookLength > 1.0E-4D) {
            player.motionX += look.x / lookLength * horizontalSpeed * 0.10D;
            player.motionZ += look.z / lookLength * horizontalSpeed * 0.10D;
        }
        player.velocityChanged = true;
        player.fallDistance = 0.0F;
        NBTTagCompound jumpData = player.getEntityData();
        jumpData.setBoolean(ROCKET_JUMPING, true);
        RocketJumpPhysicsHandler.begin(player);
        refundRocketOnSuccessfulJump(player);
        // todo PLACEHOLDER requested rocket-jump sound files were not attached, so no substitute sounds are played.
        LSPECIALMod.info("[RocketDebug] impulse APPLIED player={} distance={} radius={} strength={} falloff={} impulse={} direction=({}, {}, {}) motionBefore=({}, {}, {}) motionAfter=({}, {}, {})",
                player.getName(), distanceToBlast, blastRadius, LSPECIALConfig.rocketJumpStrength, falloff, impulse,
                dx, dy, dz, previousMotionX, previousMotionY, previousMotionZ,
                player.motionX, player.motionY, player.motionZ);
    }

    /** Refunds one loaded rocket after a successful jump, with a short duplicate-impact grace window. */
    private static void refundRocketOnSuccessfulJump(EntityPlayer player) {
        int lastTick = LAST_REWARD_TICK.containsKey(player.getUniqueID())
                ? LAST_REWARD_TICK.get(player.getUniqueID()) : Integer.MIN_VALUE / 2;
        if (player.ticksExisted - lastTick < 10) return;
        ItemStack launcher = player.getHeldItemMainhand();
        if (!StingerDamageHandler.isStingerFamilyItem(launcher)) launcher = player.getHeldItemOffhand();
        if (!StingerDamageHandler.isStingerFamilyItem(launcher)) {
            launcher = ItemStack.EMPTY;
            for (ItemStack candidate : player.inventory.mainInventory) {
                if (StingerDamageHandler.isStingerFamilyItem(candidate)) {
                    launcher = candidate;
                    break;
                }
            }
        }
        if (launcher.isEmpty()) return;
        if (launcher.getItem() instanceof ItemGunBase) {
            ItemGunBase legacyGun = (ItemGunBase) launcher.getItem();
            int current = ItemGunBase.getMag(launcher);
            int capacity = legacyGun.mainConfig == null ? 1 : Math.max(1, legacyGun.mainConfig.ammoCap);
            if (current < capacity) {
                ItemGunBase.setMag(launcher, current + 1);
                LAST_REWARD_TICK.put(player.getUniqueID(), player.ticksExisted);
            }
            return;
        }
        if (!(launcher.getItem() instanceof ItemGunBaseNT)) return;
        ItemGunBaseNT gun = (ItemGunBaseNT) launcher.getItem();
        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            GunConfig config;
            try { config = gun.getConfig(launcher, configIndex); }
            catch (RuntimeException ignored) { continue; }
            if (config == null) continue;
            Receiver[] receivers = config.getReceivers(launcher);
            if (receivers == null) continue;
            for (Receiver receiver : receivers) {
                if (receiver == null) continue;
                IMagazine magazine = receiver.getMagazine(launcher);
                if (magazine == null) continue;
                int amount = magazine.getAmount(launcher, player.inventory);
                int capacity = magazine.getCapacity(launcher);
                if (amount >= 0 && capacity > amount) {
                    magazine.setAmount(launcher, amount + 1);
                    LAST_REWARD_TICK.put(player.getUniqueID(), player.ticksExisted);
                    return;
                }
            }
        }
    }

}
