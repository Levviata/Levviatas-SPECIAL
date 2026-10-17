package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents configured hostile mobs from acquiring players as targets when that player has sufficient Charisma. */
@Mixin(value = EntityLiving.class, remap = false)
public abstract class MixinEntityLivingNeutrality {

    @Inject(method = "setAttackTarget(Lnet/minecraft/entity/EntityLivingBase;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$preventTargetingCharismaticPlayer(EntityLivingBase target, CallbackInfo ci) {
        if (!(target instanceof EntityPlayer) || !LSPECIALConfig.charisma
                || !LSPECIALConfig.charismaNeutralMobs) {
            return;
        }

        EntityLiving mob = (EntityLiving) (Object) this;
        if (!lspecial$isConfiguredNeutralMob(mob)) return;

        EntityPlayer player = (EntityPlayer) target;
        Scoreboard scoreboard = player.getWorldScoreboard();
        ScoreObjective objective = scoreboard.getObjective("Charisma");
        if (objective == null || !scoreboard.entityHasObjective(player.getName(), objective)) return;

        int charisma = scoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
        if (charisma >= Math.max(5, LSPECIALConfig.charismaNeutralLevel)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean lspecial$isConfiguredNeutralMob(EntityLivingBase entity) {
        ResourceLocation key = EntityList.getKey(entity);
        if (key == null || LSPECIALConfig.charismaNeutralEntityList == null) return false;

        String registryId = key.toString();
        if (LSPECIALConfig.charismaNeutralBlacklistMobs) {
            if (lspecial$matchesAnyPattern(LSPECIALConfig.charismaNeutralBlacklistEntityList, registryId)) {
                return false;
            }

            IAttributeInstance maxHealth = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
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

        return lspecial$matchesAnyPattern(LSPECIALConfig.charismaNeutralEntityList, registryId);
    }

    @Unique
    private static boolean lspecial$matchesAnyPattern(String configuredPatterns, String registryId) {
        if (configuredPatterns == null || configuredPatterns.trim().isEmpty()) return false;

        for (String raw : configuredPatterns.split(",")) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) continue;
            if (pattern.indexOf(':') < 0 && !pattern.equals("*")) {
                pattern = "minecraft:" + pattern;
            }
            if (lspecial$matchesPattern(pattern, registryId)) return true;
        }
        return false;
    }

    @Unique
    private static boolean lspecial$matchesPattern(String pattern, String value) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else {
                if ("\\.[]{}()+-^$|?".indexOf(c) >= 0) regex.append('\\');
                regex.append(c);
            }
        }
        regex.append('$');
        return value.matches(regex.toString());
    }
}
