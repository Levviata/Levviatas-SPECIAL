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
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records retaliation targets and accumulates player damage for dynamic retaliation.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseRetaliation {

    @Unique
    private static final String LSPECIAL_RETALIATION_TARGET = "LSPECIALRetaliationTarget";
    @Unique
    private static final String LSPECIAL_RETALIATION_DAMAGE = "LSPECIALRetaliationDamage";

    @Inject(method = "setRevengeTarget(Lnet/minecraft/entity/EntityLivingBase;)V", at = @At("HEAD"))
    private void lspecial$recordRetaliationTarget(EntityLivingBase target, CallbackInfo ci) {
        if (!"PERSISTENT".equalsIgnoreCase(LSPECIALConfig.charismaNeutralRetaliationMode)
                || !LSPECIALConfig.charisma || !LSPECIALConfig.charismaNeutralMobs
                || !(target instanceof EntityPlayer) || !((Object) this instanceof EntityLiving mob)) {
            return;
        }

        if (mob.world.isRemote) {
            return;
        }

        NBTTagCompound data = mob.getEntityData();
        data.setUniqueId(LSPECIAL_RETALIATION_TARGET, target.getUniqueID());
        LSPECIALMod.info("[CharismaRetaliation] entity={} action=RECORD mode=PERSISTENT player={} playerUuid={}",
                lspecial$retaliationEntityId(mob), target.getName(), target.getUniqueID());
    }

    @Inject(method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At("RETURN"))
    private void lspecial$trackPlayerDamage(DamageSource source, float amount,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof EntityLiving mob)) {
            return;
        }

        if (mob.world.isRemote) return;
        if (!"DYNAMIC".equalsIgnoreCase(LSPECIALConfig.charismaNeutralRetaliationMode)
                || !LSPECIALConfig.charisma || !LSPECIALConfig.charismaNeutralMobs) {
            return;
        }

        if (!cir.getReturnValue()) {
            LSPECIALMod.info("[CharismaRetaliation] entity={} action=IGNORE_DAMAGE reason=damage_not_applied amount={}",
                    lspecial$retaliationEntityId(mob), amount);
            return;
        }
        if (!lspecial$retaliationIsConfiguredNeutralMob(mob)) {
            LSPECIALMod.info("[CharismaRetaliation] entity={} action=IGNORE_DAMAGE reason=not_configured_neutral",
                    lspecial$retaliationEntityId(mob));
            return;
        }

        EntityPlayer player = source.getTrueSource() instanceof EntityPlayer
                ? (EntityPlayer) source.getTrueSource() : null;
        if (player == null || !player.isEntityAlive()) {
            LSPECIALMod.info("[CharismaRetaliation] entity={} action=IGNORE_DAMAGE reason=not_living_player_damage source={} amount={}",
                    lspecial$retaliationEntityId(mob), source.getTrueSource(), amount);
            return;
        }

        IAttributeInstance maxHealthAttribute = mob.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            LSPECIALMod.info("[CharismaRetaliation] entity={} action=IGNORE_DAMAGE reason=no_max_health_attribute",
                    lspecial$retaliationEntityId(mob));
            return;
        }

        double maxHealth = maxHealthAttribute.getAttributeValue();
        if (maxHealth <= 0.0D) {
            LSPECIALMod.info("[CharismaRetaliation] entity={} action=IGNORE_DAMAGE reason=invalid_max_health maxHealth={}",
                    lspecial$retaliationEntityId(mob), maxHealth);
            return;
        }

        NBTTagCompound entityData = mob.getEntityData();
        NBTTagCompound damageData = entityData.hasKey(LSPECIAL_RETALIATION_DAMAGE, 10)
                ? entityData.getCompoundTag(LSPECIAL_RETALIATION_DAMAGE) : new NBTTagCompound();

        String playerKey = player.getUniqueID().toString();
        double damageThisHit = Math.max(0.0F, amount);
        double totalDamage = damageData.getDouble(playerKey) + damageThisHit;
        damageData.setDouble(playerKey, totalDamage);
        entityData.setTag(LSPECIAL_RETALIATION_DAMAGE, damageData);

        double threshold = maxHealth / 3.0D;
        LSPECIALMod.info("[CharismaRetaliation] entity={} action=TRACK_DAMAGE player={} playerUuid={} damageThisHit={} cumulativeDamage={} threshold={} maxHealth={}",
                lspecial$retaliationEntityId(mob), player.getName(), player.getUniqueID(),
                damageThisHit, totalDamage, threshold, maxHealth);

        if (totalDamage >= threshold) {
            entityData.setUniqueId(LSPECIAL_RETALIATION_TARGET, player.getUniqueID());
            LSPECIALMod.info("[CharismaRetaliation] entity={} action=THRESHOLD_REACHED player={} cumulativeDamage={} threshold={} actionTarget=SET",
                    lspecial$retaliationEntityId(mob), player.getName(), totalDamage, threshold);
            mob.setRevengeTarget(player);
            mob.setAttackTarget(player);
        }
    }

    @Unique
    private static String lspecial$retaliationEntityId(EntityLiving mob) {
        ResourceLocation key = EntityList.getKey(mob);
        return key == null ? "<unregistered>" : key.toString();
    }

    @Unique
    private static boolean lspecial$retaliationIsConfiguredNeutralMob(EntityLiving mob) {
        ResourceLocation key = EntityList.getKey(mob);
        if (key == null || LSPECIALConfig.charismaNeutralEntityList == null) return false;

        String registryId = key.toString();
        if (LSPECIALConfig.charismaNeutralBlacklistMobs) {
            if (lspecial$retaliationMatchesAnyPattern(LSPECIALConfig.charismaNeutralBlacklistEntityList, registryId)) {
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

        return lspecial$retaliationMatchesAnyPattern(LSPECIALConfig.charismaNeutralEntityList, registryId);
    }

    @Unique
    private static boolean lspecial$retaliationMatchesAnyPattern(String configuredPatterns, String registryId) {
        if (configuredPatterns == null || configuredPatterns.trim().isEmpty()) return false;
        for (String raw : configuredPatterns.split(",")) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) continue;
            if (pattern.indexOf(':') < 0 && !pattern.equals("*")) pattern = "minecraft:" + pattern;
            if (lspecial$retaliationMatchesPattern(pattern, registryId)) return true;
        }
        return false;
    }

    @Unique
    private static boolean lspecial$retaliationMatchesPattern(String pattern, String value) {
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
}
