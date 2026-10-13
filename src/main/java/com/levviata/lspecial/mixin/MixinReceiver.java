package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

/** applies Intelligence perk firing delays without permanently mutating NTM receiver data. */
@Mixin(value = Receiver.class, remap = false)
public abstract class MixinReceiver {

    @Shadow protected int index;

    @Inject(method = "getDelayAfterFire", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$applyPerkFiringDelay(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (stack == null || stack.isEmpty() || !LSPECIALConfig.intelligence
                || stack.getItem().getRegistryName() == null || SPECIALScoreboard.inst == null
                || SPECIALScoreboard.inst.getIntelligenceScore() == null) {
            return;
        }

        int intelligence = SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints();
        String registryName = stack.getItem().getRegistryName().toString();

        if (!LSPECIALConfig.revolverPerk || intelligence < 15
                || !LSPECIALConfig.fatmanHigherPerk || !LSPECIALConfig.fatmanHigherPerkDelay
                || !"hbm:gun_fatman".equals(registryName)) {
            return;
        }

        callback.setReturnValue(LSPECIALConfig.fatmanHigherPerkDelayTicks);
    }

    /** disables held-button automatic refire for the Carbine while its higher perk is active. */
    @Inject(method = "getRefireOnHold", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$makeCarbineSemiAutomatic(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (LSPECIALConfig.carbineSemiAutoPerk && lspecial$isCarbineHigherPerk(stack)) {
            callback.setReturnValue(false);
        }
    }

    /** keeps the Phlogistinator-style flamer on NTM's native held-fire path while its perk is active. */
    @Inject(method = "getRefireOnHold", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$enablePhlogistinatorSustainedFire(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (LSPECIALConfig.phlogistinatorSustainedFirePerk
                && lspecial$hasIntelligence(LSPECIALConfig.phlogistinatorSustainedFireLevel)
                && "hbm:gun_flamer".equals(lspecial$registryId(stack))) {
            callback.setReturnValue(true);
        }
    }

    /** applies the configured native firing interval to the Phlogistinator-style flamer. */
    @Inject(method = "getDelayAfterFire", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$setPhlogistinatorFireDelay(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (LSPECIALConfig.phlogistinatorSustainedFirePerk
                && lspecial$hasIntelligence(LSPECIALConfig.phlogistinatorSustainedFireLevel)
                && "hbm:gun_flamer".equals(lspecial$registryId(stack))) {
            callback.setReturnValue(LSPECIALConfig.phlogistinatorFireDelayTicks);
        }
    }

    /** applies native firing intervals only while a requested alternate caliber or energy mode is selected. */
    @Inject(method = "getDelayAfterFire", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$applyAlternateCaliberDelay(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (stack == null || stack.isEmpty() || !LSPECIALConfig.weaponSecondaryAmmoCycling
                || !lspecial$hasIntelligence(15)) return;

        String itemId = lspecial$registryId(stack);
        if (!lspecial$isAlternateCaliberDelayWeapon(itemId)) return;
        BulletConfig activeAmmo = lspecial$getActiveAmmo(stack);
        if (activeAmmo == null) return;

        if (lspecial$isStarF(itemId) && activeAmmo == XFactory50.bmg50_fmj) {
            callback.setReturnValue(LSPECIALConfig.starFFiftyBmgDelayTicks);
        } else if (lspecial$isUzi(itemId) && activeAmmo == XFactory556mm.r556_fmj) {
            callback.setReturnValue(LSPECIALConfig.uzi556DelayTicks);
        } else if (lspecial$isG3(itemId) && activeAmmo == XFactory10ga.g10) {
            callback.setReturnValue(LSPECIALConfig.g3TenGaugeDelayTicks);
        } else if (lspecial$isMinigun(itemId) && LSPECIALConfig.minigunLaserModePerk
                && activeAmmo == XFactoryEnergy.energy_las) {
            callback.setReturnValue(LSPECIALConfig.minigunLaserModeDelayTicks);
        }
    }

    @Unique
    private static boolean lspecial$isAlternateCaliberDelayWeapon(String itemId) {
        return lspecial$isStarF(itemId) || lspecial$isUzi(itemId) || lspecial$isG3(itemId)
                || lspecial$isMinigun(itemId);
    }

    @Unique
    private static boolean lspecial$isStarF(String itemId) {
        return "hbm:gun_star_f".equals(itemId) || "hbm:gun_star_f_akimbo".equals(itemId);
    }

    @Unique
    private static boolean lspecial$isUzi(String itemId) {
        return "hbm:gun_uzi".equals(itemId) || "hbm:gun_uzi_akimbo".equals(itemId);
    }

    @Unique
    private static boolean lspecial$isG3(String itemId) {
        return "hbm:gun_g3".equals(itemId) || "hbm:gun_g3_zebra".equals(itemId);
    }

    @Unique
    private static boolean lspecial$isMinigun(String itemId) {
        return "hbm:gun_minigun".equals(itemId) || "hbm:gun_minigun_dual".equals(itemId);
    }

    /** reads the active magazine bullet configuration for the current receiver without forcing a reload. */
    @Unique
    private BulletConfig lspecial$getActiveAmmo(ItemStack stack) {
        IMagazine magazine = ((Receiver) (Object) this).getMagazine(stack);
        if (magazine == null) return null;
        Object ammo = magazine.getType(stack, null);
        return ammo instanceof BulletConfig ? (BulletConfig) ammo : null;
    }

    /** scales the native Carbine recoil callback without replacing its ordinary firing implementation. */
    @Inject(method = "getRecoil", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$scaleCarbineRecoil(ItemStack stack,
            CallbackInfoReturnable<BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext>> callback) {
        if (!lspecial$isCarbineHigherPerk(stack)) return;
        final double multiplier = LSPECIALConfig.carbineRecoilMultiplier;
        final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> original = callback.getReturnValue();
        if (original == null) return;
        callback.setReturnValue((gunStack, context) -> {
            EntityLivingBase entity = context == null ? null : context.entity;
            if (entity == null || multiplier >= 0.999999D) {
                original.accept(gunStack, context);
                return;
            }
            float pitchBefore = entity.rotationPitch;
            float yawBefore = entity.rotationYaw;
            float headYawBefore = entity.rotationYawHead;
            original.accept(gunStack, context);
            float pitchDelta = entity.rotationPitch - pitchBefore;
            float yawDelta = MathHelper.wrapDegrees(entity.rotationYaw - yawBefore);
            float headYawDelta = MathHelper.wrapDegrees(entity.rotationYawHead - headYawBefore);
            entity.rotationPitch = pitchBefore + (float) (pitchDelta * multiplier);
            entity.rotationYaw = yawBefore + (float) (yawDelta * multiplier);
            entity.rotationYawHead = headYawBefore + (float) (headYawDelta * multiplier);
        });
    }

    /** checks that the stack is the Carbine and the higher Intelligence perk is active. */
    private static boolean lspecial$isCarbineHigherPerk(ItemStack stack) {
        return stack != null && !stack.isEmpty() && LSPECIALConfig.intelligence
                && LSPECIALConfig.revolverPerk && SPECIALScoreboard.inst != null
                && SPECIALScoreboard.inst.getIntelligenceScore() != null
                && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() >= 15
                && "hbm:gun_carbine".equals(lspecial$registryId(stack));
    }

    /** applies the Tau charged-secondary damage bonus and balances three-shot secondary bursts. */
    @Inject(method = "getBaseDamage", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$adjustSecondaryDamage(ItemStack stack, CallbackInfoReturnable<Float> callback) {
        if (!lspecial$isSecondaryHeld(stack)) return;
        String itemId = lspecial$registryId(stack);
        float damage = callback.getReturnValue();

        if ("hbm:gun_tau".equals(itemId) && LSPECIALConfig.tauSecondaryDamageBonus
                && lspecial$hasIntelligence(5)) {
            callback.setReturnValue((float) (damage * LSPECIALConfig.tauSecondaryDamageMultiplier));
        } else if (("hbm:gun_lag".equals(itemId) && LSPECIALConfig.lagSecondaryBurst && lspecial$hasIntelligence(5))
                || ("hbm:gun_henry_lincoln".equals(itemId) && LSPECIALConfig.henryLincolnSecondaryBurst
                && lspecial$hasIntelligence(15))) {
            callback.setReturnValue((float) (damage * LSPECIALConfig.burstProjectileDamageFraction));
        }
    }

    /** changes native receiver burst count only while the matching secondary-fire key is held. */
    @Inject(method = "getRoundsPerCycle", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$adjustSecondaryBurst(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (!lspecial$isSecondaryHeld(stack)) return;
        String itemId = lspecial$registryId(stack);
        if ("hbm:gun_lag".equals(itemId) && LSPECIALConfig.lagSecondaryBurst && lspecial$hasIntelligence(5)) {
            callback.setReturnValue(3);
        } else if ("hbm:gun_henry_lincoln".equals(itemId)
                && LSPECIALConfig.henryLincolnSecondaryBurst && lspecial$hasIntelligence(15)) {
            callback.setReturnValue(3);
        }
    }

    /** applies the configured Lag secondary-fire delay without changing its primary-fire cadence. */
    @Inject(method = "getDelayAfterFire", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$adjustLagSecondaryDelay(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (lspecial$isSecondaryHeld(stack) && "hbm:gun_lag".equals(lspecial$registryId(stack))
                && LSPECIALConfig.lagSecondaryBurst && lspecial$hasIntelligence(5)) {
            callback.setReturnValue(LSPECIALConfig.lagSecondaryBurstDelay);
        }
    }

    /** tightens spread while the supported lesser-perk weapons are scoped. */
    @Inject(method = "getInnateSpread", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$adjustScopedSpread(ItemStack stack, CallbackInfoReturnable<Float> callback) {
        if (stack == null || stack.isEmpty() || !LSPECIALConfig.scopedSecondarySpread
                || !ItemGunBaseNT.getIsAiming(stack) || !lspecial$hasIntelligence(5)) return;
        String itemId = lspecial$registryId(stack);
        if ("hbm:gun_maresleg".equals(itemId) || "hbm:gun_liberator".equals(itemId)
                || "hbm:gun_autoshotgun".equals(itemId) || "hbm:gun_autoshotgun_heretic".equals(itemId)
                || "hbm:gun_autoshotgun_sexy".equals(itemId)
                || "hbm:gun_congolake".equals(itemId) || "hbm:gun_am180".equals(itemId)) {
            callback.setReturnValue((float) (callback.getReturnValue() * LSPECIALConfig.scopedSpreadMultiplier));
        }
    }

    /** tightens ammunition-derived spread as well as innate spread while a copied scope is active. */
    @Inject(method = "getAmmoSpread", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$adjustScopedAmmoSpread(ItemStack stack, CallbackInfoReturnable<Float> callback) {
        if (stack == null || stack.isEmpty() || !LSPECIALConfig.scopedSecondarySpread
                || !ItemGunBaseNT.getIsAiming(stack) || !lspecial$hasIntelligence(5)) return;
        String itemId = lspecial$registryId(stack);
        if ("hbm:gun_maresleg".equals(itemId) || "hbm:gun_liberator".equals(itemId)
                || "hbm:gun_autoshotgun".equals(itemId) || "hbm:gun_autoshotgun_heretic".equals(itemId)
                || "hbm:gun_autoshotgun_sexy".equals(itemId)
                || "hbm:gun_congolake".equals(itemId) || "hbm:gun_am180".equals(itemId)) {
            callback.setReturnValue((float) (callback.getReturnValue() * LSPECIALConfig.scopedSpreadMultiplier));
        }
    }

    /** checks the shared SPECIAL score safely because NTM can query receiver values on either side. */
    private static boolean lspecial$hasIntelligence(int level) {
        return LSPECIALConfig.intelligence
                && (level >= 15 ? LSPECIALConfig.revolverPerk : LSPECIALConfig.pepperboxPerk)
                && SPECIALScoreboard.inst != null
                && SPECIALScoreboard.inst.getIntelligenceScore() != null
                && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() >= level;
    }

    /** checks the NTM per-config secondary input state stored on the weapon stack. */
    private boolean lspecial$isSecondaryHeld(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ItemGunBaseNT.getSecondary(stack, index);
    }

    /** returns the namespaced item ID without depending on translated display text. */
    private static String lspecial$registryId(ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null
                ? "" : stack.getItem().getRegistryName().toString();
    }

}
