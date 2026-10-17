package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;

/** shared TF2-style blast-jump behavior for NTM rocket projectile implementations. */
public final class RocketJumpHandler {

    private RocketJumpHandler() {
    }

    /** checks the Intelligence 5 lesser-perk gate for the supplied player's own scoreboard. */
    public static boolean isPerkActive(EntityPlayer player) {
        if (player == null) {
            LSPECIALMod.info("[RocketDebug] rocket-jump perk check failed: player is null");
            return false;
        }
        int intelligence = SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD);
        boolean active = LSPECIALConfig.intelligence && LSPECIALConfig.rocketJumpLesserPerk
                && intelligence >= LSPECIALConfig.rocketJumpLesserPerkLevel;
        LSPECIALMod.info("[RocketDebug] perk check player={} intelligence={} intelligenceEnabled={} perkEnabled={} requiredLevel={} active={}",
                player.getName(), intelligence, LSPECIALConfig.intelligence, LSPECIALConfig.rocketJumpLesserPerk,
                LSPECIALConfig.rocketJumpLesserPerkLevel, active);
        return active;
    }

    /** replaces the native owner knockback with a radial impulse biased upward, preserving existing momentum. */
    public static void applyBlastJump(EntityPlayer player, double explosionX, double explosionY, double explosionZ,
                                      double previousMotionX, double previousMotionY, double previousMotionZ,
                                      double fallbackMotionX, double fallbackMotionY, double fallbackMotionZ,
                                      double blastRadius) {
        if (player == null || player.world.isRemote || player.isDead) {
            LSPECIALMod.info("[RocketDebug] applyBlastJump rejected player={} remote={} dead={}",
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
            LSPECIALMod.info("[RocketDebug] applyBlastJump out of range player={} distance={} radius={} playerPos=({}, {}, {}) explosionPos=({}, {}, {})",
                    player.getName(), distanceToBlast, blastRadius, player.posX, player.posY, player.posZ,
                    explosionX, explosionY, explosionZ);
            return;
        }

        LSPECIALMod.info("[RocketDebug] applyBlastJump in range player={} distance={} radius={} previousMotion=({}, {}, {}) fallbackMotion=({}, {}, {})",
                player.getName(), distanceToBlast, blastRadius, previousMotionX, previousMotionY, previousMotionZ,
                fallbackMotionX, fallbackMotionY, fallbackMotionZ);
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
        player.velocityChanged = true;
        player.fallDistance = 0.0F;
        LSPECIALMod.info("[RocketDebug] applyBlastJump SUCCESS player={} impulse={} direction=({}, {}, {}) finalMotion=({}, {}, {})",
                player.getName(), impulse, dx, dy, dz, player.motionX, player.motionY, player.motionZ);
    }
}
