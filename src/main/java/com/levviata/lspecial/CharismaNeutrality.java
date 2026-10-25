package com.levviata.lspecial;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides which mobs are neutral towards a player because of Charisma, shared by the targeting mixins and the
 * release of mobs that were already hunting the player when the neutrality kicked in.
 */
public final class CharismaNeutrality {

    public static final String RETALIATION_TARGET = "LSPECIALRetaliationTarget";

    // pattern matching builds a regex per entry, so the answers are kept until the lists change
    private static final Map<String, Boolean> MATCH_CACHE = new HashMap<>();
    private static String cachedLists = "";

    private CharismaNeutrality() {
    }

    /**
     * Returns whether the Charisma neutrality is on at all for this player.
     */
    public static boolean isActiveFor(EntityPlayer player) {
        return LSPECIALConfig.charisma && LSPECIALConfig.charismaNeutralMobs
                && SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.CHARISMA_BOARD) >= LSPECIALConfig.charismaNeutralLevel;
    }

    /**
     * Returns whether the mob must leave this player alone: neutrality is active, the mob is configured as neutral
     * and it isn't allowed to retaliate against the player.
     */
    public static boolean isNeutralTowards(EntityLiving mob, EntityPlayer player) {
        if (mob.world.isRemote || !isActiveFor(player)) {
            return false;
        }

        ResourceLocation key = EntityList.getKey(mob);
        String registryId = key == null ? "<unregistered>" : key.toString();
        String mode = LSPECIALConfig.charismaNeutralRetaliationMode == null
                ? "NORMAL" : LSPECIALConfig.charismaNeutralRetaliationMode.trim().toUpperCase();
        return isConfiguredNeutralMob(mob, registryId) && !isRetaliatingAgainst(mob, player, mode);
    }

    public static boolean isRetaliatingAgainst(EntityLiving mob, EntityPlayer player, String mode) {
        if ("PERSISTENT".equals(mode) || "DYNAMIC".equals(mode)) {
            NBTTagCompound data = mob.getEntityData();
            return data.hasUniqueId(RETALIATION_TARGET)
                    && player.getUniqueID().equals(data.getUniqueId(RETALIATION_TARGET));
        }

        // NORMAL mode only permits the vanilla revenge target to attack back
        return mob.getRevengeTarget() == player;
    }

    public static boolean isConfiguredNeutralMob(EntityLiving mob, String registryId) {
        if (registryId == null || "<unregistered>".equals(registryId)
                || LSPECIALConfig.charismaNeutralEntityList == null) {
            return false;
        }

        if (LSPECIALConfig.charismaNeutralBlacklistMobs) {
            if (matchesAnyPattern(LSPECIALConfig.charismaNeutralBlacklistEntityList, registryId)) {
                return false;
            }

            IAttributeInstance maxHealth = mob.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
            if (maxHealth != null) {
                double baseHealth = maxHealth.getBaseValue();
                if (LSPECIALConfig.charismaNeutralBlacklistNonPositiveHealth
                        && baseHealth <= LSPECIALConfig.charismaNeutralBlacklistNonPositiveHealthThreshold) {
                    return false;
                }
                if (LSPECIALConfig.charismaNeutralBlacklistHighHealth
                        && baseHealth >= LSPECIALConfig.charismaNeutralBlacklistHighHealthThreshold) {
                    return false;
                }
            }
        }

        return matchesAnyPattern(LSPECIALConfig.charismaNeutralEntityList, registryId);
    }

    /**
     * Makes the mobs around the player that are hunting them and should be neutral drop the target.
     */
    public static void releaseTargets(EntityPlayer player) {
        if (player.world.isRemote || !isActiveFor(player)) {
            return;
        }

        List<EntityLiving> mobs = player.world.getEntitiesWithinAABB(EntityLiving.class, player.getEntityBoundingBox().grow(64.0D));
        for (EntityLiving mob : mobs) {
            if (mob.getAttackTarget() == player && isNeutralTowards(mob, player)) {
                mob.setAttackTarget(null);
                mob.getNavigator().clearPath();
                LSPECIALMod.info("CharismaNeutrality", "entity={} target={} result=RELEASED", EntityList.getKey(mob), player.getName());
            }
        }
    }

    public static boolean matchesAnyPattern(String configuredPatterns, String registryId) {
        if (configuredPatterns == null || configuredPatterns.trim().isEmpty()) {
            return false;
        }

        String lists = LSPECIALConfig.charismaNeutralEntityList + "\u0000" + LSPECIALConfig.charismaNeutralBlacklistEntityList;
        if (!lists.equals(cachedLists)) {
            MATCH_CACHE.clear();
            cachedLists = lists;
        }

        String cacheKey = configuredPatterns + "\u0000" + registryId;
        Boolean cached = MATCH_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        boolean matches = false;
        for (String raw : configuredPatterns.split(",")) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            if (pattern.indexOf(':') < 0 && !pattern.equals("*")) {
                pattern = "minecraft:" + pattern;
            }
            if (matchesPattern(pattern, registryId)) {
                matches = true;
                break;
            }
        }

        MATCH_CACHE.put(cacheKey, matches);
        return matches;
    }

    private static boolean matchesPattern(String pattern, String value) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else {
                if ("\\.^$|?+()[]{}-".indexOf(c) >= 0) {
                    regex.append('\\');
                }
                regex.append(c);
            }
        }
        regex.append('$');
        return value.matches(regex.toString());
    }
}
