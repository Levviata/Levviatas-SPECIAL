package com.levviata.lspecial.mixin;

import com.hbm.handler.HbmKeybinds;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.levviata.lspecial.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** adds server-authoritative secondary-fire actions while retaining NTM's native gun implementation. */
@Mixin(value = ItemGunBaseNT.class, remap = false)
public abstract class MixinWeaponSecondary {

    /** handles crouch-secondary ammo cycling, the Lag's self-kill action, and double-barrel grappling. */
    @Inject(method = "handleKeybind(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/inventory/IInventory;Lnet/minecraft/item/ItemStack;Lcom/hbm/handler/HbmKeybinds$EnumKeybind;Z)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$handleSecondaryPerks(EntityLivingBase entity, IInventory inventory, ItemStack stack, HbmKeybinds.EnumKeybind keybind, boolean pressed, CallbackInfo callback) {
        if (!(entity instanceof EntityPlayer) || stack == null || stack.isEmpty() || keybind == null) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        String itemId = registryId(stack);
        if (itemId == null) return;

        if ("hbm:gun_flamer_topaz".equals(itemId) && keybind.name().contains("PRIMARY")
                && stack.hasTagCompound()
                && stack.getTagCompound().getInteger("LSPECIALDragonFuryPressureLock") > 0) {
            callback.cancel();
            return;
        }

        if (LSPECIALConfig.secondaryFireToggle && lspecial$supportsToggleMode(itemId)
                && !player.isSneaking() && keybind.name().contains("SECONDARY") && pressed
                && lspecial$hasIntelligence(player, lspecial$toggleRequiredLevel(itemId))) {
            net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new net.minecraft.nbt.NBTTagCompound();
                stack.setTagCompound(tag);
            }
            boolean mode = !tag.getBoolean("LSPECIALSecondaryMode");
            tag.setBoolean("LSPECIALSecondaryMode", mode);
            player.world.playSound(null, player.getPosition(),
                    mode ? HBMSoundHandler.switchmode2 : HBMSoundHandler.switchmode1,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            lspecial$setSecondaryForAllConfigs(stack, mode);
            callback.cancel();
            return;
        }

        if (LSPECIALConfig.secondaryFireToggle && lspecial$supportsToggleMode(itemId)
                && keybind.name().contains("PRIMARY")) {
            boolean mode = stack.hasTagCompound() && stack.getTagCompound().getBoolean("LSPECIALSecondaryMode");
            lspecial$setSecondaryForAllConfigs(stack, mode);
            if (pressed && mode && ("hbm:gun_lag".equals(itemId) || "hbm:gun_henry_lincoln".equals(itemId))) {
                int requiredLevel = "hbm:gun_lag".equals(itemId) ? 5 : 15;
                boolean enabled = "hbm:gun_lag".equals(itemId)
                        ? LSPECIALConfig.lagSecondaryBurst : LSPECIALConfig.henryLincolnSecondaryBurst;
                if (enabled && lspecial$hasIntelligence(player, requiredLevel)) {
                    if (!player.world.isRemote) {
                        SPECIALGameplayHandler.startSequentialBurst(player, stack,
                                LSPECIALConfig.lagSecondaryBurstDelay, requiredLevel);
                    }
                    callback.cancel();
                    return;
                }
            }
        }

        // Route the double-barrel grapple through NTM's tertiary/scope key.
        // Cancel the native tertiary action for this weapon so it cannot scope.
        if (keybind.name().contains("TERTIARY") && "hbm:gun_double_barrel".equals(itemId)
                && LSPECIALConfig.doubleBarrelGrapple && pressed
                && lspecial$hasIntelligence(player, 15)) {
            if (player.world.isRemote) {
                if (LSPECIALConfig.doubleBarrelGrappleVisuals) {
                    net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
                    if (tag == null) {
                        tag = new net.minecraft.nbt.NBTTagCompound();
                        stack.setTagCompound(tag);
                    }
                    tag.setInteger("LSPECIALGrappleVisualTicks", LSPECIALConfig.doubleBarrelGrappleVisualTicks);
                }
            } else {
                if (player.isSneaking()) {
                    pullTargetTowardPlayer(player, LSPECIALConfig.doubleBarrelGrappleRange,
                            LSPECIALConfig.doubleBarrelGrapplePullSpeed);
                } else {
                    pullPlayerTowardTarget(player, LSPECIALConfig.doubleBarrelGrappleRange,
                            LSPECIALConfig.doubleBarrelGrapplePullSpeed);
                }
            }
            callback.cancel();
            return;
        }

        if (keybind != HbmKeybinds.EnumKeybind.GUN_SECONDARY) return;

        // Both flame weapons use TF2 airblast on secondary fire; Dragon's Fury shares its pressure lock.
        if ("hbm:gun_flamer".equals(itemId) || "hbm:gun_flamer_topaz".equals(itemId)) {
            if (pressed && !player.world.isRemote) {
                TF2FlamethrowerHandler.airblast(player, stack);
            }
            callback.cancel();
            return;
        }

        boolean carbineSecondary = "hbm:gun_carbine".equals(itemId) && LSPECIALConfig.carbineSemiAutoPerk && lspecial$hasIntelligence(player, 15);
        boolean lagSecondary = "hbm:gun_lag".equals(itemId) && LSPECIALConfig.lagSecondaryBurst && lspecial$hasIntelligence(player, 5);
        boolean lincolnSecondary = "hbm:gun_henry_lincoln".equals(itemId) && LSPECIALConfig.henryLincolnSecondaryBurst && lspecial$hasIntelligence(player, 15);
        boolean tauSecondary = "hbm:gun_tau".equals(itemId) && LSPECIALConfig.tauSecondaryDamageBonus && lspecial$hasIntelligence(player, 5);
        boolean lightRevolverSecondary = "hbm:gun_light_revolver".equals(itemId) && LSPECIALConfig.lightRevolverRapidFirePerk && lspecial$hasIntelligence(player, 5);
        if (carbineSecondary || lagSecondary || lincolnSecondary
                || tauSecondary || lightRevolverSecondary) {
            // NTM reads the secondary state for the modified mode, but still needs primary input to run its firing pipeline.
            lspecial$setPrimaryForAllConfigs(stack, pressed);
        }

        if (!pressed) return;

        if (player.world.isRemote) {
            boolean lightRevolverTrigger = "hbm:gun_light_revolver".equals(itemId)
                    && LSPECIALConfig.lightRevolverRapidFirePerk && lspecial$hasIntelligence(player, 5);
            if (lightRevolverTrigger && SPECIALGameplayHandler.startLightRevolverRapidFire(player, stack)) {
                callback.cancel();
                return;
            }

            boolean isSpecialAction = (player.isSneaking() && "hbm:gun_lag".equals(itemId)
                    && LSPECIALConfig.lagSecondarySelfKill && lspecial$hasIntelligence(player, 5))
                    || (player.isSneaking() && LSPECIALConfig.weaponSecondaryAmmoCycling
                    && lspecial$hasIntelligence(player, 5) && supportsAmmoCycling(itemId)
                    && (!"hbm:gun_amat".equals(itemId) || (LSPECIALConfig.AMATRailgunProjectile && lspecial$hasIntelligence(player, 15))))
                    || (player.isSneaking() && "hbm:gun_double_barrel".equals(itemId)
                    && LSPECIALConfig.doubleBarrelGrapple && lspecial$hasIntelligence(player, 15));
            if (isSpecialAction) {
                if (player.isSneaking() && "hbm:gun_double_barrel".equals(itemId)
                        && LSPECIALConfig.doubleBarrelGrappleVisuals) {
                    net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
                    if (tag == null) {
                        tag = new net.minecraft.nbt.NBTTagCompound();
                        stack.setTagCompound(tag);
                    }
                    tag.setInteger("LSPECIALGrappleVisualTicks", LSPECIALConfig.doubleBarrelGrappleVisualTicks);
                }
                callback.cancel();
            }
            return;
        }
        if (!lspecial$hasIntelligence(player, 5)) return;

        if (!player.isSneaking() && "hbm:gun_light_revolver".equals(itemId)
                && LSPECIALConfig.lightRevolverRapidFirePerk
                && SPECIALGameplayHandler.startLightRevolverRapidFire(player, stack)) {
            callback.cancel();
            return;
        }

        if (player.isSneaking() && "hbm:gun_lag".equals(itemId) && LSPECIALConfig.lagSecondarySelfKill
                && SPECIALGameplayHandler.startLagSelfKill(player, stack)) {
            callback.cancel();
            return;
        }

        if (player.isSneaking() && LSPECIALConfig.weaponSecondaryAmmoCycling
                && lspecial$hasIntelligence(player, 5) && supportsAmmoCycling(itemId)
                && (!"hbm:gun_amat".equals(itemId) || (LSPECIALConfig.AMATRailgunProjectile && lspecial$hasIntelligence(player, 15)))
                && cycleSupportedAmmo(player, stack, inventory, itemId)) {
            net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new net.minecraft.nbt.NBTTagCompound();
                stack.setTagCompound(tag);
            }
            boolean mode = !tag.getBoolean("LSPECIALTertiaryMode");
            tag.setBoolean("LSPECIALTertiaryMode", mode);
            player.world.playSound(null, player.getPosition(),
                    mode ? HBMSoundHandler.switchmode2 : HBMSoundHandler.switchmode1,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            callback.cancel();
            return;
        }
    }

    /** returns whether the player's current Intelligence score meets the requested perk level. */
    @Unique
    private static boolean lspecial$hasIntelligence(EntityPlayer player, int level) {
        return LSPECIALConfig.intelligence
                && (level >= 15 ? LSPECIALConfig.revolverPerk : LSPECIALConfig.pepperboxPerk)
                && SPECIALScoreboard.inst != null
                && SPECIALScoreboard.inst.getIntelligenceScore() != null
                && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() >= level;
    }

    /** returns the registry identifier used to match a weapon without relying on its display name. */
    @Unique
    private static String registryId(ItemStack stack) {
        return stack.getItem().getRegistryName() == null ? null : stack.getItem().getRegistryName().toString();
    }

    /** identifies the guns with a defined alternate caliber or energy-projectile mode. */
    @Unique
    private static boolean supportsAmmoCycling(String itemId) {
        return "hbm:gun_pepperbox".equals(itemId) || isStarF(itemId) || isUzi(itemId)
                || isG3(itemId) || "hbm:gun_amat".equals(itemId);
    }


    @Unique
    private static boolean isStarF(String itemId) {
        return "hbm:gun_star_f".equals(itemId) || "hbm:gun_star_f_akimbo".equals(itemId);
    }

    @Unique
    private static boolean isUzi(String itemId) {
        return "hbm:gun_uzi".equals(itemId) || "hbm:gun_uzi_akimbo".equals(itemId);
    }

    @Unique
    private static boolean isG3(String itemId) {
        return "hbm:gun_g3".equals(itemId) || "hbm:gun_g3_zebra".equals(itemId);
    }

    @Unique
    private static boolean isMinigun(String itemId) {
        return "hbm:gun_minigun".equals(itemId) || "hbm:gun_minigun_dual".equals(itemId);
    }

    /** identifies weapons that should copy the Heretic's secondary-fire aiming behavior. */
    @Unique
    private static boolean isHereticScopeCopyWeapon(String itemId) {
        return "hbm:gun_maresleg".equals(itemId) || "hbm:gun_liberator".equals(itemId)
                || "hbm:gun_autoshotgun".equals(itemId) || "hbm:gun_autoshotgun_heretic".equals(itemId)
                || "hbm:gun_autoshotgun_sexy".equals(itemId) || "hbm:gun_congolake".equals(itemId)
                || "hbm:gun_am180".equals(itemId);
    }

    /** returns every native NTM bullet configuration in the alternate caliber family. */
    @Unique
    private static List<BulletConfig> getAlternateCalibers(String itemId) {
        if ("hbm:gun_amat".equals(itemId)) {
            return java.util.Collections.singletonList(AMATRailgun.getBulletConfig());
        }
        String prefix;
        Class<?> factory;
        if ("hbm:gun_pepperbox".equals(itemId)) { factory = XFactory44.class; prefix = "m44_"; }
        else if (isStarF(itemId)) { factory = XFactory50.class; prefix = "bmg50_"; }
        else if (isUzi(itemId)) { factory = XFactory556mm.class; prefix = "r556_"; }
        else if (isG3(itemId)) { factory = XFactory10ga.class; prefix = "g10"; }
        else return java.util.Collections.emptyList();

        List<BulletConfig> result = new java.util.ArrayList<>();
        try {
            for (java.lang.reflect.Field field : factory.getFields()) {
                if (field.getType() != BulletConfig.class || !field.getName().startsWith(prefix)) continue;
                BulletConfig config = (BulletConfig) field.get(null);
                if (config != null && !result.contains(config)) result.add(config);
            }
        } catch (IllegalAccessException ignored) {
            // Public factory fields are expected; an inaccessible field simply isn't a usable caliber.
        }
        result.sort(Comparator.comparingInt(config -> config.id));
        return result;
    }

    /** swaps each distinct magazine on single and akimbo weapons without changing NTM's firing pipeline. */
    @Unique
    private static boolean cycleSupportedAmmo(EntityPlayer player, ItemStack stack, IInventory inventory, String itemId) {
        if (!(stack.getItem() instanceof ItemGunBaseNT)) return false;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        List<BulletConfig> alternateCalibers = getAlternateCalibers(itemId);
        if (isMinigun(itemId)) {
            for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
                GunConfig config = gun.getConfig(stack, configIndex);
                if (config == null) continue;
                Receiver[] receivers = config.getReceivers(stack);
                if (receivers == null) continue;
                for (Receiver receiver : receivers) {
                    if (receiver == null) continue;
                    IMagazine magazine = receiver.getMagazine(stack);
                    if (magazine instanceof MagazineBelt
                            && lspecial$cycleBeltAmmo(stack, (MagazineBelt) magazine)) {
                        player.inventory.markDirty();
                        return true;
                    }
                }
            }
            return false;
        }
        if (alternateCalibers.isEmpty()) return false;

        Set<String> visitedMagazineKeys = new HashSet<>();
        boolean changed = false;
        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            GunConfig config = gun.getConfig(stack, configIndex);
            if (config == null) continue;
            Receiver[] receivers = config.getReceivers(stack);
            if (receivers == null) continue;

            for (Receiver receiver : receivers) {
                if (receiver == null) continue;
                IMagazine magazine = receiver.getMagazine(stack);
                if (magazine == null) continue;
                String magazineKey = getMagazineKey(magazine);
                if (magazineKey == null || !visitedMagazineKeys.add(magazineKey)) continue;
                changed |= cycleOneMagazine(player, stack, inventory, magazine, alternateCalibers);
            }
        }

        if (changed) {
            player.inventory.markDirty();
        }
        return changed;
    }

    /** unwraps the Luck wrapper so magazine-specific mode and capacity logic sees the native magazine type. */
    @Unique
    private static IMagazine unwrapMagazine(IMagazine magazine) {
        while (magazine instanceof RefundMagazine) {
            magazine = ((RefundMagazine) magazine).getOriginalMagazine();
        }
        return magazine;
    }

    /** creates a stable key so shared magazines are switched once even on multi-receiver guns. */
    @Unique
    private static String getMagazineKey(IMagazine magazine) {
        IMagazine original = unwrapMagazine(magazine);
        if (original instanceof MagazineSingleTypeBase) {
            return "single:" + ((MagazineSingleTypeBase) original).index;
        }
        if (original instanceof MagazineBelt) return "belt";
        return null;
    }

    /** changes one magazine's selected caliber and refunds only ammunition physically loaded into that magazine. */
    @Unique
    private static boolean cycleOneMagazine(EntityPlayer player, ItemStack stack, IInventory inventory,
                                            IMagazine magazine, List<BulletConfig> alternateCalibers) {
        IMagazine original = unwrapMagazine(magazine);
        if (original instanceof MagazineBelt) return false;
        if (!(original instanceof MagazineSingleTypeBase)) return false;

        MagazineSingleTypeBase single = (MagazineSingleTypeBase) original;
        List<BulletConfig> accepted = single.acceptedBullets;
        if (accepted == null || accepted.isEmpty()) return false;
        for (BulletConfig config : alternateCalibers) {
            if (config != null && !accepted.contains(config)) accepted.add(config);
        }

        Object currentObject = single.getType(stack, null);
        if (!(currentObject instanceof BulletConfig)) return false;
        BulletConfig current = (BulletConfig) currentObject;
        BulletConfig defaultAmmo = accepted.get(0);
        List<BulletConfig> alternateOptions = new java.util.ArrayList<>();
        for (BulletConfig config : alternateCalibers) {
            if (config != null && config != defaultAmmo && !alternateOptions.contains(config)) alternateOptions.add(config);
        }
        if (alternateOptions.isEmpty()) return false;
        int currentAlternate = alternateOptions.indexOf(current);
        BulletConfig target;
        if (isStarF(registryId(stack))) {
            // Star F switches between the original .22 group and the BMG group,
            // rather than cycling every individual BMG projectile configuration.
            target = currentAlternate < 0 ? alternateOptions.get(0) : defaultAmmo;
        } else if (currentAlternate < 0) {
            target = alternateOptions.get(0);
        } else if (currentAlternate + 1 < alternateOptions.size()) {
            target = alternateOptions.get(currentAlternate + 1);
        } else {
            target = defaultAmmo;
        }
        if (target == null || target == current) return false;

        int loadedAmount = single.getAmount(stack, null);
        if (loadedAmount > 0) {
            if (current.ammo == null) return false;
            int roundsPerAmmoItem = Math.max(1, current.ammoReloadCount);
            if (loadedAmount % roundsPerAmmoItem != 0) return false;
            int refundCount = loadedAmount / roundsPerAmmoItem;
            if (!refundAmmoToPlayer(player, current, refundCount)) return false;
            single.setAmount(stack, 0);
        }

        single.setType(stack, target);
        single.setAmountBeforeReload(stack, 0);
        single.setAmountAfterReload(stack, 0);
        if (!stack.hasTagCompound()) stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        stack.getTagCompound().setInteger("LSPECIALSelectedCaliber_" + single.index, target.id);
        return true;
    }

    /** cycles the Minigun's inventory-fed belt through native capacitor configurations without refunding inventory ammo. */
    @Unique
    private static boolean lspecial$cycleBeltAmmo(ItemStack stack, MagazineBelt magazine) {
        List<BulletConfig> accepted = magazine.acceptedBullets;
        if (accepted == null || accepted.isEmpty()) return false;
        lspecial$registerEnergyCapacitors(accepted);

        net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
        int currentId = tag != null && tag.hasKey("LSPECIALSelectedBeltCaliber")
                ? tag.getInteger("LSPECIALSelectedBeltCaliber") : accepted.get(0).id;
        BulletConfig current = lspecial$findBulletConfig(currentId);
        List<BulletConfig> modes = lspecial$getEnergyCapacitors();
        int currentIndex = modes.indexOf(current);
        BulletConfig target;
        if (currentIndex < 0) {
            target = modes.get(0);
        } else if (currentIndex + 1 < modes.size()) {
            target = modes.get(currentIndex + 1);
        } else {
            target = accepted.get(0);
        }

        if (target == null || target == current) {
            return false;
        }

        // update NTM's native magazine type as well as the saved LSPECIAL selection,
        // so every receiver resolves the new projectile immediately.
        magazine.setType(stack, target);
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
        if (target == accepted.get(0)) {
            stack.getTagCompound().removeTag("LSPECIALSelectedBeltCaliber");
        } else {
            stack.getTagCompound().setInteger("LSPECIALSelectedBeltCaliber", target.id);
        }
        return true;
    }

    /** adds the native capacitor configurations used by NTM's laser, Tesla, and emerald energy weapons. */
    @Unique
    private static void lspecial$registerEnergyCapacitors(List<BulletConfig> accepted) {
        for (BulletConfig config : lspecial$getEnergyCapacitors()) {
            if (!accepted.contains(config)) accepted.add(config);
        }
    }

    @Unique
    private static List<BulletConfig> lspecial$getEnergyCapacitors() {
        return Arrays.asList(XFactoryEnergy.energy_las, XFactoryEnergy.energy_las_overcharge,
                XFactoryEnergy.energy_las_ir, XFactoryEnergy.energy_tesla,
                XFactoryEnergy.energy_tesla_overcharge, XFactoryEnergy.energy_tesla_ir,
                XFactoryEnergy.energy_emerald,
                XFactoryEnergy.energy_emerald_overcharge, XFactoryEnergy.energy_emerald_ir);
    }

    @Unique
    private static BulletConfig lspecial$findBulletConfig(int id) {
        for (BulletConfig config : BulletConfig.configs) {
            if (config != null && config.id == id) return config;
        }
        return null;
    }

    /** returns old-caliber ammunition to inventory, dropping every leftover stack beside the player. */
    @Unique
    private static boolean refundAmmoToPlayer(EntityPlayer player, BulletConfig oldConfig, int count) {
        if (count <= 0) return true;
        if (oldConfig.ammo == null) return false;
        ItemStack template = oldConfig.ammo.getStack();
        if (template.isEmpty()) return false;

        int remaining = count;
        while (remaining > 0) {
            ItemStack refund = template.copy();
            int stackCount = Math.min(remaining, refund.getMaxStackSize());
            refund.setCount(stackCount);
            player.inventory.addItemStackToInventory(refund);
            if (!refund.isEmpty()) {
                player.dropItem(refund, false);
            }
            remaining -= stackCount;
        }
        return true;
    }


    /** identifies weapons with implemented alternate fire that can be selected as a persistent mode. */
    @Unique
    private static boolean lspecial$supportsToggleMode(String itemId) {
        return "hbm:gun_carbine".equals(itemId) || "hbm:gun_lag".equals(itemId)
                || "hbm:gun_henry_lincoln".equals(itemId) || "hbm:gun_tau".equals(itemId)
                || "hbm:gun_light_revolver".equals(itemId);
    }

    @Unique
    private static int lspecial$toggleRequiredLevel(String itemId) {
        return "hbm:gun_carbine".equals(itemId) || "hbm:gun_henry_lincoln".equals(itemId) ? 15 : 5;
    }

    @Unique
    private static void lspecial$setSecondaryForAllConfigs(ItemStack stack, boolean pressed) {
        if (!(stack.getItem() instanceof ItemGunBaseNT)) return;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            ItemGunBaseNT.setSecondary(stack, configIndex, pressed);
        }
    }

    /** sets native primary input while the secondary button is held for alternate-fire modes. */
    @Unique
    private static void lspecial$setPrimaryForAllConfigs(ItemStack stack, boolean pressed) {
        if (!(stack.getItem() instanceof ItemGunBaseNT)) {
            return;
        }

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            ItemGunBaseNT.setPrimary(stack, configIndex, pressed);
        }
    }

    /** finds the nearest entity in the player's look direction and applies a bounded pull velocity. */
    @Unique
    private static void pullPlayerTowardTarget(EntityPlayer player, double range, double pullSpeed) {
        if (player.world.isRemote || !(player instanceof net.minecraft.entity.player.EntityPlayerMP)) return;
        Entity target = lspecial$findGrappleTarget(player, range);
        if (target != null) GrappleController.start((net.minecraft.entity.player.EntityPlayerMP) player, target, false, pullSpeed);
    }

    @Unique
    private static void pullTargetTowardPlayer(EntityPlayer player, double range, double pullSpeed) {
        if (player.world.isRemote || !(player instanceof net.minecraft.entity.player.EntityPlayerMP)) return;
        Entity target = lspecial$findGrappleTarget(player, range);
        if (target != null) GrappleController.start((net.minecraft.entity.player.EntityPlayerMP) player, target, true, pullSpeed);
    }

    @Unique
    private static Entity lspecial$findGrappleTarget(EntityPlayer player, double range) {
        Vec3d look = player.getLookVec().normalize();
        Vec3d eye = player.getPositionEyes(1.0F);
        return player.world.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox().grow(range)).stream()
                .filter(candidate -> candidate instanceof EntityLivingBase && candidate.isEntityAlive())
                .filter(candidate -> candidate.getDistance(player) <= range)
                .filter(candidate -> {
                    Vec3d direction = candidate.getPositionVector().add(0, candidate.height * 0.5D, 0).subtract(eye).normalize();
                    return look.dotProduct(direction) >= 0.94D;
                })
                .min(Comparator.comparingDouble(player::getDistanceSq)).orElse(null);
    }

}
