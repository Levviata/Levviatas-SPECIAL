package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLiving.class)
public abstract class MixinEntityLivingCharismaNeutrality {

    @Unique
    private static final String LSPECIAL_RETALIATION_TARGET = "LSPECIALRetaliationTarget";

    @Unique
    private static int lspecial$getCharisma(EntityPlayer player) {
        Scoreboard scoreboard = player.getWorldScoreboard();
        ScoreObjective objective = scoreboard.getObjective("Charisma");
        if (objective == null || !scoreboard.entityHasObjective(player.getName(), objective)) return 0;
        return scoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
    }

    @Unique
    private static boolean lspecial$isRetaliatingAgainst(EntityLiving mob, EntityPlayer player, String mode) {
        if ("PERSISTENT".equals(mode) || "DYNAMIC".equals(mode)) {
            NBTTagCompound data = mob.getEntityData();
            return data.hasUniqueId(LSPECIAL_RETALIATION_TARGET)
                    && player.getUniqueID().equals(data.getUniqueId(LSPECIAL_RETALIATION_TARGET));
        }

        // NORMAL mode only permits the vanilla revenge target to attack back.
        return mob.getRevengeTarget() == player;
    }

    @Unique
    private static String lspecial$getStoredRetaliationTarget(EntityLiving mob) {
        NBTTagCompound data = mob.getEntityData();
        return data.hasUniqueId(LSPECIAL_RETALIATION_TARGET)
                ? data.getUniqueId(LSPECIAL_RETALIATION_TARGET).toString() : "<none>";
    }

    @Unique
    private static boolean lspecial$isConfiguredNeutralMob(EntityLiving mob, String registryId) {
        if (registryId == null || "<unregistered>".equals(registryId)
                || LSPECIALConfig.charismaNeutralEntityList == null) return false;

        if (LSPECIALConfig.charismaNeutralBlacklistMobs) {
            if (lspecial$matchesAnyPattern(LSPECIALConfig.charismaNeutralBlacklistEntityList, registryId)) {
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

        return lspecial$matchesAnyPattern(LSPECIALConfig.charismaNeutralEntityList, registryId);
    }

    @Unique
    private static boolean lspecial$matchesAnyPattern(String configuredPatterns, String registryId) {
        if (configuredPatterns == null || configuredPatterns.trim().isEmpty()) return false;
        for (String raw : configuredPatterns.split(",")) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) continue;
            if (pattern.indexOf(':') < 0 && !pattern.equals("*")) pattern = "minecraft:" + pattern;
            if (lspecial$matchesPattern(pattern, registryId)) return true;
        }
        return false;
    }

    @Unique
    private static boolean lspecial$matchesPattern(String pattern, String value) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') regex.append(".*");
            else {
                if ("\\.^$|?+()[]{}-".indexOf(c) >= 0) regex.append('\\');
                regex.append(c);
            }
        }
        regex.append('$');
        return value.matches(regex.toString());
    }

    @Inject(method = "setAttackTarget(Lnet/minecraft/entity/EntityLivingBase;)V",
            at = @At("HEAD"), cancellable = true)
    private void lspecial$applyCharismaNeutrality(EntityLivingBase target, CallbackInfo ci) {
        if (!(target instanceof EntityPlayer player)) {
            return;
        }

        EntityLiving mob = (EntityLiving) (Object) this;
        if (mob.world.isRemote) {
            return;
        }

        ResourceLocation registryKey = EntityList.getKey(mob);
        String entityId = registryKey == null ? "<unregistered>" : registryKey.toString();
        String mode = LSPECIALConfig.charismaNeutralRetaliationMode == null
                ? "NORMAL" : LSPECIALConfig.charismaNeutralRetaliationMode.trim().toUpperCase();

        if (!LSPECIALConfig.charisma || !LSPECIALConfig.charismaNeutralMobs) {
            LSPECIALMod.info("[CharismaNeutrality] entity={} target={} result=ALLOW reason=feature_disabled charismaEnabled={} neutralityEnabled={}",
                    entityId, player.getName(), LSPECIALConfig.charisma, LSPECIALConfig.charismaNeutralMobs);
            return;
        }

        int charisma = lspecial$getCharisma(player);
        if (charisma < LSPECIALConfig.charismaNeutralLevel) {
            LSPECIALMod.info("[CharismaNeutrality] entity={} target={} result=ALLOW reason=below_threshold charisma={} required={}",
                    entityId, player.getName(), charisma, LSPECIALConfig.charismaNeutralLevel);
            return;
        }

        boolean configured = lspecial$isConfiguredNeutralMob(mob, entityId);
        if (!configured) {
            LSPECIALMod.info("[CharismaNeutrality] entity={} target={} result=ALLOW reason=not_in_neutral_list mode={} list={}",
                    entityId, player.getName(), mode, LSPECIALConfig.charismaNeutralEntityList);
            return;
        }

        boolean retaliationAllowed = lspecial$isRetaliatingAgainst(mob, player, mode);
        if (retaliationAllowed) {
            LSPECIALMod.info("[CharismaNeutrality] entity={} target={} result=ALLOW reason=retaliation mode={} revengeTarget={} storedTarget={}",
                    entityId, player.getName(), mode,
                    mob.getRevengeTarget() == null ? "<none>" : mob.getRevengeTarget().getName(),
                    lspecial$getStoredRetaliationTarget(mob));
            return;
        }

        LSPECIALMod.info("[CharismaNeutrality] entity={} target={} result=CANCEL reason=neutral charisma={} required={} mode={} revengeTarget={} storedTarget={}",
                entityId, player.getName(), charisma, LSPECIALConfig.charismaNeutralLevel, mode,
                mob.getRevengeTarget() == null ? "<none>" : mob.getRevengeTarget().getName(),
                lspecial$getStoredRetaliationTarget(mob));
        ci.cancel();
    }
}
