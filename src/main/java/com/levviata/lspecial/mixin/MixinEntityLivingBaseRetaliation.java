package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records retaliation targets and accumulates player damage for dynamic retaliation. */
@Mixin(EntityLiving.class)
public abstract class MixinEntityLivingBaseRetaliation {

    private static final String LSPECIAL_RETALIATION_TARGET = "LSPECIALRetaliationTarget";
    private static final String LSPECIAL_RETALIATION_DAMAGE = "LSPECIALRetaliationDamage";

    @Inject(method = "setRevengeTarget(Lnet/minecraft/entity/EntityLivingBase;)V", at = @At("HEAD"))
    private void lspecial$recordRetaliationTarget(EntityLivingBase target, CallbackInfo ci) {
        if (!lspecial$isPersistentMode() || !LSPECIALConfig.charisma
                || !LSPECIALConfig.charismaNeutralMobs
                || !(target instanceof EntityPlayer)) {
            return;
        }

        EntityLiving mob = (EntityLiving) (Object) this;
        NBTTagCompound data = mob.getEntityData();
        data.setUniqueId(LSPECIAL_RETALIATION_TARGET, target.getUniqueID());
    }

    @Inject(method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", at = @At("RETURN"))
    private void lspecial$trackPlayerDamage(DamageSource source, float amount,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()
                || !"DYNAMIC".equalsIgnoreCase(LSPECIALConfig.charismaNeutralRetaliationMode)
                || !LSPECIALConfig.charisma || !LSPECIALConfig.charismaNeutralMobs)  {
            return;
        }

        EntityLiving mob = (EntityLiving) (Object) this;
        if (!lspecial$isConfiguredNeutralMob(mob)) return;

        EntityPlayer player = source.getTrueSource() instanceof EntityPlayer
                ? (EntityPlayer) source.getTrueSource() : null;
        if (player == null || !player.isEntityAlive()) return;

        IAttributeInstance maxHealthAttribute = mob.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (maxHealthAttribute == null) return;

        double maxHealth = maxHealthAttribute.getAttributeValue();
        if (maxHealth <= 0.0D) return;

        NBTTagCompound entityData = mob.getEntityData();
        NBTTagCompound damageData = entityData.hasKey(LSPECIAL_RETALIATION_DAMAGE, 10)
                ? entityData.getCompoundTag(LSPECIAL_RETALIATION_DAMAGE) : new NBTTagCompound();

        String playerKey = player.getUniqueID().toString();
        double totalDamage = damageData.getDouble(playerKey) + Math.max(0.0F, amount);
        damageData.setDouble(playerKey, totalDamage);
        entityData.setTag(LSPECIAL_RETALIATION_DAMAGE, damageData);

        // Persist retaliation once this player's cumulative damage reaches one third of max health.
        if (totalDamage >= maxHealth / 3.0D) {
            entityData.setUniqueId(LSPECIAL_RETALIATION_TARGET, player.getUniqueID());
            mob.setRevengeTarget(player);
            mob.setAttackTarget(player);
        }
    }

    private static boolean lspecial$isConfiguredNeutralMob(EntityLiving mob) {
        ResourceLocation key = EntityList.getKey(mob);
        if (key == null || LSPECIALConfig.charismaNeutralEntityList == null) return false;

        String registryId = key.toString();
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

    private static boolean lspecial$matchesPattern(String pattern, String value) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') regex.append(".*");
            else {
                if ("\\.^$|?*+()[]{}-".indexOf(c) >= 0) regex.append('\\');
                regex.append(c);
            }
        }
        regex.append('$');
        return value.matches(regex.toString());
    }

    private static boolean lspecial$isPersistentMode() {
        String mode = LSPECIALConfig.charismaNeutralRetaliationMode;
        return "PERSISTENT".equalsIgnoreCase(mode) || "DYNAMIC".equalsIgnoreCase(mode);
    }
}
