package com.levviata.lspecial;

import com.hbm.items.weapon.ItemGunBase;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared TF2-style blast-jump behavior for NTM rocket projectile implementations. */
public final class RocketJumpHandler {
    private static final String ROCKET_JUMPING = "LSPECIALRocketJumping";
    private static final String REWARD_PENDING = "LSPECIALRocketJumpRewardPending";
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
        Vec3d forward = new Vec3d(look.x, 0.0D, look.z);
        if (forward.lengthSquared() > 1.0E-4D) {
            forward = forward.normalize();
            double forwardVelocity = player.motionX * forward.x + player.motionZ * forward.z;
            if (forwardVelocity > 0.0D) {
                double forwardBoost = forwardVelocity * LSPECIALConfig.rocketJumpForwardVelocityFactor;
                player.motionX += forward.x * forwardBoost;
                player.motionZ += forward.z * forwardBoost;
            }
        }
        lspecial$applyVelocityCap(player);
        player.velocityChanged = true;
        player.fallDistance = 0.0F;
        NBTTagCompound jumpData = player.getEntityData();
        jumpData.setBoolean(ROCKET_JUMPING, true);
        RocketJumpPhysicsHandler.begin(player);
        refundRocketOnSuccessfulJump(player);
        LSPECIALMod.info("[RocketDebug] impulse APPLIED player={} distance={} radius={} strength={} falloff={} impulse={} direction=({}, {}, {}) motionBefore=({}, {}, {}) motionAfter=({}, {}, {})",
                player.getName(), distanceToBlast, blastRadius, LSPECIALConfig.rocketJumpStrength, falloff, impulse,
                dx, dy, dz, previousMotionX, previousMotionY, previousMotionZ,
                player.motionX, player.motionY, player.motionZ);
    }

    /** consumes the previous jump's one-shot reward on this jump, then arms the next reward. */
    private static void refundRocketOnSuccessfulJump(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        int lastTick = LAST_REWARD_TICK.containsKey(player.getUniqueID())
                ? LAST_REWARD_TICK.get(player.getUniqueID()) : Integer.MIN_VALUE / 2;
        if (player.ticksExisted - lastTick < 10) return;

        boolean rewardPending = data.getBoolean(REWARD_PENDING);
        data.removeTag(REWARD_PENDING);
        if (!rewardPending) {
            data.setBoolean(REWARD_PENDING, true);
            return;
        }

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
        if (launcher.isEmpty()) {
            data.setBoolean(REWARD_PENDING, true);
            return;
        }

        boolean refunded = false;
        if (launcher.getItem() instanceof ItemGunBase) {
            ItemGunBase legacyGun = (ItemGunBase) launcher.getItem();
            int current = ItemGunBase.getMag(launcher);
            int capacity = legacyGun.mainConfig == null ? 1 : Math.max(1, legacyGun.mainConfig.ammoCap);
            if (current < capacity) {
                ItemGunBase.setMag(launcher, current + 1);
                refunded = true;
            }
        } else if (launcher.getItem() instanceof ItemGunBaseNT) {
            ItemGunBaseNT gun = (ItemGunBaseNT) launcher.getItem();
            for (int configIndex = 0; configIndex < gun.getConfigCount() && !refunded; configIndex++) {
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
                        refunded = true;
                        break;
                    }
                }
            }
        }

        if (refunded) {
            LAST_REWARD_TICK.put(player.getUniqueID(), player.ticksExisted);
            lspecial$playRefundSound(player);
        }
        data.setBoolean(REWARD_PENDING, true);
    }

    private static void lspecial$playRefundSound(EntityPlayer player) {
        SoundEvent[] sounds = {
                LSPECIALSounds.ROCKET_REFUND1,
                LSPECIALSounds.ROCKET_REFUND2,
                LSPECIALSounds.ROCKET_REFUND3
        };
        SoundEvent sound = sounds[player.world.rand.nextInt(sounds.length)];
        if (sound != null) {
            player.world.playSound(null, player.getPosition(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void lspecial$applyVelocityCap(EntityPlayer player) {
        double cap = LSPECIALConfig.rocketJumpVelocityCap;
        if (cap <= 0.0D) return;
        double speed = Math.sqrt(player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ);
        if (speed > cap) {
            double factor = cap / speed;
            player.motionX *= factor;
            player.motionY *= factor;
            player.motionZ *= factor;
        }
    }

}
