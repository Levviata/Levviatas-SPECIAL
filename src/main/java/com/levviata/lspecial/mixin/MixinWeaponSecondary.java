package com.levviata.lspecial.mixin;

import com.hbm.handler.HbmKeybinds;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;
import com.levviata.lspecial.SPECIALGameplayHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** adds server-authoritative secondary-fire actions while retaining NTM's native gun implementation. */
@Mixin(value = ItemGunBaseNT.class, remap = false)
public abstract class MixinWeaponSecondary {

    /** handles crouch-secondary ammo cycling, the Lag's self-kill action, and double-barrel grappling. */
    @Inject(method = "handleKeybind(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/inventory/IInventory;Lnet/minecraft/item/ItemStack;Lcom/hbm/handler/HbmKeybinds$EnumKeybind;Z)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$handleSecondaryPerks(EntityLivingBase entity, IInventory inventory,
                                               ItemStack stack, HbmKeybinds.EnumKeybind keybind,
                                               boolean pressed, CallbackInfo callback) {
        if (!(entity instanceof EntityPlayer) || stack == null || stack.isEmpty()
                || keybind != HbmKeybinds.EnumKeybind.GUN_SECONDARY) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        String itemId = registryId(stack);
        if (itemId == null) return;

        // mirror the Heretic's held-secondary aiming state on the weapons that lack its native scope behavior.
        if (LSPECIALConfig.hereticStyleScoping && isHereticScopeCopyWeapon(itemId)
                && ((pressed && !player.isSneaking()) || (!pressed && ItemGunBaseNT.getIsAiming(stack)))
                && hasIntelligence(player, 5)) {
            ItemGunBaseNT.setIsAiming(stack, pressed);
            callback.cancel();
            return;
        }
        if (!pressed) return;

        int requiredLevel = isHigherPerkAmmoWeapon(itemId) ? 15 : 5;
        if (player.world.isRemote) {
            boolean isSpecialAction = (player.isSneaking() && "hbm:gun_lag".equals(itemId)
                    && LSPECIALConfig.lagSecondarySelfKill && hasIntelligence(player, 5))
                    || (player.isSneaking() && LSPECIALConfig.weaponSecondaryAmmoCycling
                    && hasIntelligence(player, requiredLevel) && supportsAmmoCycling(itemId)
                    && (!isMinigun(itemId) || LSPECIALConfig.minigunLaserModePerk))
                    || (player.isSneaking() && "hbm:gun_double_barrel".equals(itemId)
                    && LSPECIALConfig.doubleBarrelGrapple && hasIntelligence(player, 15))
                    || ("hbm:gun_light_revolver".equals(itemId) && LSPECIALConfig.lightRevolverRapidFirePerk
                    && hasIntelligence(player, 5));
            if (isSpecialAction) {
                if ("hbm:gun_light_revolver".equals(itemId) && LSPECIALConfig.lightRevolverRapidFirePerk) {
                    SPECIALGameplayHandler.startLightRevolverRapidFire(player, stack);
                }
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
        if (!hasIntelligence(player, 5)) return;

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
                && hasIntelligence(player, requiredLevel) && supportsAmmoCycling(itemId)
                && (!isMinigun(itemId) || LSPECIALConfig.minigunLaserModePerk)
                && cycleSupportedAmmo(player, stack, inventory, itemId)) {
            callback.cancel();
            return;
        }

        if (player.isSneaking() && "hbm:gun_double_barrel".equals(itemId)
                && LSPECIALConfig.doubleBarrelGrapple && hasIntelligence(player, 15)) {
            pullPlayerTowardTarget(player, LSPECIALConfig.doubleBarrelGrappleRange,
                    LSPECIALConfig.doubleBarrelGrapplePullSpeed);
            callback.cancel();
        }
    }

    /** returns whether the player's current Intelligence score meets the requested perk level. */
    private static boolean hasIntelligence(EntityPlayer player, int level) {
        return LSPECIALConfig.intelligence
                && (level >= 15 ? LSPECIALConfig.revolverPerk : LSPECIALConfig.pepperboxPerk)
                && SPECIALScoreboard.inst != null
                && SPECIALScoreboard.inst.getIntelligenceScore() != null
                && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() >= level;
    }

    /** returns the registry identifier used to match a weapon without relying on its display name. */
    private static String registryId(ItemStack stack) {
        return stack.getItem().getRegistryName() == null ? null : stack.getItem().getRegistryName().toString();
    }

    /** identifies the guns with a defined alternate caliber or energy-projectile mode. */
    private static boolean supportsAmmoCycling(String itemId) {
        return "hbm:gun_pepperbox".equals(itemId) || isStarF(itemId) || isUzi(itemId)
                || isG3(itemId) || isMinigun(itemId);
    }

    /** identifies alternate-caliber weapons that require the higher Intelligence perk. */
    private static boolean isHigherPerkAmmoWeapon(String itemId) {
        return "hbm:gun_pepperbox".equals(itemId) || isUzi(itemId) || isG3(itemId) || isMinigun(itemId);
    }

    private static boolean isStarF(String itemId) {
        return "hbm:gun_star_f".equals(itemId) || "hbm:gun_star_f_akimbo".equals(itemId);
    }

    private static boolean isUzi(String itemId) {
        return "hbm:gun_uzi".equals(itemId) || "hbm:gun_uzi_akimbo".equals(itemId);
    }

    private static boolean isG3(String itemId) {
        return "hbm:gun_g3".equals(itemId) || "hbm:gun_g3_zebra".equals(itemId);
    }

    private static boolean isMinigun(String itemId) {
        return "hbm:gun_minigun".equals(itemId) || "hbm:gun_minigun_dual".equals(itemId);
    }

    /** identifies weapons that should copy the Heretic's secondary-fire aiming behavior. */
    private static boolean isHereticScopeCopyWeapon(String itemId) {
        return "hbm:gun_maresleg".equals(itemId) || "hbm:gun_liberator".equals(itemId)
                || "hbm:gun_autoshotgun".equals(itemId) || "hbm:gun_congolake".equals(itemId)
                || "hbm:gun_am180".equals(itemId);
    }

    /** returns the requested native NTM bullet configuration for a weapon's alternate mode. */
    private static BulletConfig getRequestedAmmo(String itemId) {
        if ("hbm:gun_pepperbox".equals(itemId)) return XFactory44.m44_fmj;
        if (isStarF(itemId)) return XFactory50.bmg50_fmj;
        if (isUzi(itemId)) return XFactory556mm.r556_fmj;
        if (isG3(itemId)) return XFactory10ga.g10;
        if (isMinigun(itemId)) return XFactoryEnergy.energy_las;
        return null;
    }

    /** swaps each distinct magazine on single and akimbo weapons without changing NTM's firing pipeline. */
    private static boolean cycleSupportedAmmo(EntityPlayer player, ItemStack stack, IInventory inventory, String itemId) {
        if (!(stack.getItem() instanceof ItemGunBaseNT)) return false;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        BulletConfig requested = getRequestedAmmo(itemId);
        if (requested == null) return false;

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
                changed |= cycleOneMagazine(player, stack, inventory, magazine, requested);
            }
        }

        if (changed) player.inventory.markDirty();
        return changed;
    }

    /** creates a stable key so shared magazines are switched once even on multi-receiver guns. */
    private static String getMagazineKey(IMagazine magazine) {
        if (magazine instanceof MagazineSingleTypeBase) {
            return "single:" + ((MagazineSingleTypeBase) magazine).index;
        }
        if (magazine instanceof MagazineBelt) return "belt";
        return null;
    }

    /** changes one magazine's stored caliber after safely refunding its loaded ammunition. */
    private static boolean cycleOneMagazine(EntityPlayer player, ItemStack stack, IInventory inventory,
                                            IMagazine magazine, BulletConfig requested) {
        List<BulletConfig> accepted;
        if (magazine instanceof MagazineSingleTypeBase) {
            accepted = ((MagazineSingleTypeBase) magazine).acceptedBullets;
        } else if (magazine instanceof MagazineBelt) {
            accepted = ((MagazineBelt) magazine).acceptedBullets;
        } else {
            return false;
        }
        if (accepted == null || accepted.isEmpty()) return false;
        if (!accepted.contains(requested)) accepted.add(requested);

        Object currentObject = magazine.getType(stack, inventory);
        if (!(currentObject instanceof BulletConfig)) return false;
        BulletConfig current = (BulletConfig) currentObject;
        BulletConfig target = current == requested
                ? accepted.stream().filter(bullet -> bullet != requested).findFirst().orElse(null)
                : requested;
        if (target == null || target == current) return false;

        int loadedAmount = magazine.getAmount(stack, inventory);
        if (loadedAmount > 0) {
            if (current.ammo == null) return false;
            int roundsPerAmmoItem = Math.max(1, current.ammoReloadCount);
            // refuse a switch if NTM's ammo item represents a bundle that cannot be returned exactly.
            if (loadedAmount % roundsPerAmmoItem != 0) return false;
            int refundCount = loadedAmount / roundsPerAmmoItem;
            if (!refundAmmoToPlayer(player, current, refundCount)) return false;
            magazine.setAmount(stack, 0);
        }

        magazine.setType(stack, target);
        magazine.setAmountBeforeReload(stack, 0);
        magazine.setAmountAfterReload(stack, 0);
        return true;
    }

    /** returns old-caliber ammunition to inventory, dropping any overflow beside the player. */
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
            if (!player.inventory.addItemStackToInventory(refund)) {
                player.dropItem(refund, false);
            }
            remaining -= stackCount;
        }
        return true;
    }

    /** finds the nearest entity in the player's look direction and applies a bounded pull velocity. */
    private static void pullPlayerTowardTarget(EntityPlayer player, double range, double pullSpeed) {
        World world = player.world;
        Vec3d look = player.getLookVec().normalize();
        Vec3d eye = player.getPositionEyes(1.0F);
        AxisAlignedBB searchBox = player.getEntityBoundingBox().grow(range);
        List<Entity> targets = world.getEntitiesWithinAABBExcludingEntity(player, searchBox);

        Entity target = targets.stream()
                .filter(candidate -> candidate instanceof EntityLivingBase && candidate.isEntityAlive())
                .filter(candidate -> candidate.getDistance(player) <= range)
                .filter(candidate -> {
                    Vec3d direction = candidate.getPositionVector().add(0, candidate.height * 0.5, 0)
                            .subtract(eye).normalize();
                    return look.dotProduct(direction) >= 0.94;
                })
                .min(Comparator.comparingDouble(player::getDistanceSq))
                .orElse(null);

        if (target == null) return;
        Vec3d destination = target.getPositionVector().add(0, target.height * 0.5, 0);
        Vec3d velocity = destination.subtract(player.getPositionVector()).normalize().scale(pullSpeed);
        player.motionX = velocity.x;
        player.motionY = velocity.y + 0.08;
        player.motionZ = velocity.z;
        player.fallDistance = 0;
        player.velocityChanged = true;
    }
}
