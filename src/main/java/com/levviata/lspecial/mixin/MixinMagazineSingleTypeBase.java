package com.levviata.lspecial.mixin;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.levviata.lspecial.AMATQuackosRailgun;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.hbm.items.weapon.sedna.factory.*;

import java.util.List;

/** applies selected-caliber persistence and configured capacities to NTM single-type magazines. */
@Mixin(value = MagazineSingleTypeBase.class, remap = false)
public abstract class MixinMagazineSingleTypeBase {

    @Shadow public List<BulletConfig> acceptedBullets;
    @Shadow public int index;
    @Shadow public int capacity;

    /** registers every supported bullet variant in each alternate caliber family before NTM validates saved types. */
    @Inject(method = "getType(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;)Lcom/hbm/items/weapon/sedna/BulletConfig;", at = @At("HEAD"), remap = false)
    private void lspecial$registerSavedAlternateCaliber(ItemStack stack, IInventory inventory,
            CallbackInfoReturnable<BulletConfig> callback) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null || acceptedBullets == null) return;
        String itemId = stack.getItem().getRegistryName().toString();
        Class<?> factory = null;
        String prefix = null;
        if ("hbm:gun_pepperbox".equals(itemId)) { factory = XFactory44.class; prefix = "m44_"; }
        else if ("hbm:gun_amat".equals(itemId)) {
            BulletConfig requested = AMATQuackosRailgun.getBulletConfig();
            if (requested != null && !acceptedBullets.contains(requested)) acceptedBullets.add(requested);
            return;
        } else if ("hbm:gun_star_f".equals(itemId) || "hbm:gun_star_f_akimbo".equals(itemId)) { factory = XFactory50.class; prefix = "bmg50_"; }
        else if ("hbm:gun_uzi".equals(itemId) || "hbm:gun_uzi_akimbo".equals(itemId)) { factory = XFactory556mm.class; prefix = "r556_"; }
        else if ("hbm:gun_g3".equals(itemId) || "hbm:gun_g3_zebra".equals(itemId)) { factory = XFactory10ga.class; prefix = "g10"; }
        if (factory == null) return;
        try {
            for (java.lang.reflect.Field field : factory.getFields()) {
                if (field.getType() != BulletConfig.class || !field.getName().startsWith(prefix)) continue;
                BulletConfig config = (BulletConfig) field.get(null);
                if (config != null && !acceptedBullets.contains(config)) acceptedBullets.add(config);
            }
        } catch (IllegalAccessException ignored) {
            // Factory configs are public; ignore any unavailable entry without breaking native ammo lookup.
        }
    }

    /** returns the explicitly selected caliber even when NTM's ordinary lookup would fall back to the first config. */
    @Inject(method = "getType(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;)Lcom/hbm/items/weapon/sedna/BulletConfig;", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$preserveSelectedCaliber(ItemStack stack, IInventory inventory, CallbackInfoReturnable<BulletConfig> callback) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return;
        }

        String key = lspecial$selectedCaliberKey();
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(key)) {
            return;
        }

        BulletConfig selected = lspecial$findConfig(tag.getInteger(key));
        if (selected != null && acceptedBullets != null && acceptedBullets.contains(selected)) {
            callback.setReturnValue(selected);
        }
    }

    /** loads only the selected ammunition and uses getCapacity so Intelligence capacity upgrades can fill completely. */
    @Inject(method = "standardReload(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$reloadSelectedCaliber(ItemStack stack, IInventory inventory, int amount, CallbackInfo callback) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        boolean selectedCaliber = stack.hasTagCompound() && stack.getTagCompound().hasKey(lspecial$selectedCaliberKey());
        int actualCapacity = ((MagazineSingleTypeBase) (Object) this).getCapacity(stack);
        if (!selectedCaliber && actualCapacity <= capacity) return;

        MagazineSingleTypeBase magazine = (MagazineSingleTypeBase) (Object) this;
        BulletConfig selected = magazine.getType(stack, null);
        if (selected == null && acceptedBullets != null && !acceptedBullets.isEmpty()) {
            selected = acceptedBullets.get(0);
        }

        if (selected == null) {
            callback.cancel();
            return;
        }

        magazine.setType(stack, selected);

        if (inventory == null) {
            magazine.setAmount(stack, actualCapacity);
            callback.cancel();
            return;
        }

        int loaded = magazine.getAmount(stack, inventory);
        int room = Math.max(0, actualCapacity - loaded);
        int reloadCount = Math.max(1, selected.ammoReloadCount);
        if (room <= 0 || selected.ammo == null) {
            callback.cancel();
            return;
        }

        for (int slot = 0; slot < inventory.getSizeInventory() && room > 0; slot++) {
            ItemStack inventoryStack = inventory.getStackInSlot(slot);
            if (inventoryStack.isEmpty()) {
                continue;
            }

            if (selected.ammo.matchesRecipe(inventoryStack, true)) {
                int needed = Math.min((int) Math.ceil(room / (double) reloadCount), inventoryStack.getCount());
                if (needed <= 0) {
                    continue;
                }

                int added = Math.min(room, needed * reloadCount);
                magazine.setAmount(stack, loaded + added);
                inventory.decrStackSize(slot, needed);
                loaded += added;
                room -= added;
                continue;
            }

            if (inventoryStack.getItem() != ModItems.ammo_bag && inventoryStack.getItem() != ModItems.ammo_bag_infinite) {
                continue;
            }

            boolean infiniteBag = inventoryStack.getItem() == ModItems.ammo_bag_infinite;
            ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(inventoryStack);
            for (int bagSlot = 0; bagSlot < bag.getSlots() && room > 0; bagSlot++) {
                ItemStack bagStack = bag.getStackInSlot(bagSlot);
                if (bagStack.isEmpty() || !selected.ammo.matchesRecipe(bagStack, true)) {
                    continue;
                }

                int needed = Math.min((int) Math.ceil(room / (double) reloadCount), bagStack.getCount());
                if (needed <= 0) {
                    continue;
                }

                int added = Math.min(room, needed * reloadCount);
                magazine.setAmount(stack, loaded + added);
                if (!infiniteBag) bag.extractItem(bagSlot, needed, false);
                loaded += added;
                room -= added;
            }
        }
        callback.cancel();
    }

    /** applies the Intelligence higher-perk capacity without changing the shared base magazine capacity. */
    @Inject(method = "getCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$applyHigherPerkCapacity(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return;

        String earlyRegistryName = stack.getItem().getRegistryName().toString();
        if ("hbm:gun_amat".equals(earlyRegistryName) && stack.hasTagCompound()
                && stack.getTagCompound().getInteger(lspecial$selectedCaliberKey()) == AMATQuackosRailgun.getBulletConfig().id) {
            callback.setReturnValue(1);
            return;
        }

        if ("hbm:gun_amat".equals(earlyRegistryName) && stack.hasTagCompound()) {
            BulletConfig capsule = AMATQuackosRailgun.getBulletConfig();
            if (capsule != null && stack.getTagCompound().getInteger(lspecial$selectedCaliberKey()) == capsule.id) {
                callback.setReturnValue(1);
                return;
            }
        }

        if (("hbm:gun_star_f".equals(earlyRegistryName) || "hbm:gun_star_f_akimbo".equals(earlyRegistryName))
                && stack.hasTagCompound() && stack.getTagCompound().hasKey(lspecial$selectedCaliberKey())) {
            BulletConfig selected = lspecial$findConfig(stack.getTagCompound().getInteger(lspecial$selectedCaliberKey()));
            if (lspecial$isBmgConfig(selected)) {
                callback.setReturnValue(10);
                return;
            }
        }

        if (!LSPECIALConfig.intelligence || !LSPECIALConfig.revolverPerk) return;

        if (SPECIALScoreboard.inst == null || SPECIALScoreboard.inst.getIntelligenceScore() == null
                || SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() < 15 || stack.getItem().getRegistryName() == null) {
            return;
        }

        String registryName = stack.getItem().getRegistryName().toString();
        if (LSPECIALConfig.fatmanHigherPerk && "hbm:gun_fatman".equals(registryName)) {
            callback.setReturnValue(LSPECIALConfig.fatmanMagazineCapacity);
        } else if (LSPECIALConfig.laserPewPewHigherPerk && "hbm:gun_laser_pistol_pew_pew".equals(registryName)) {
            callback.setReturnValue(LSPECIALConfig.laserPewPewMagazineCapacity);
        } else if (LSPECIALConfig.carbineHigherPerkCapacity && "hbm:gun_carbine".equals(registryName)) {
            callback.setReturnValue(LSPECIALConfig.carbineMagazineCapacity);
        } else if (("hbm:gun_star_f".equals(registryName) || "hbm:gun_star_f_akimbo".equals(registryName))
                && LSPECIALConfig.modifyGuns && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() >= 15) {
            callback.setReturnValue(LSPECIALConfig.starFCapacity);
        }
    }

    @Unique
    private static boolean lspecial$isBmgConfig(BulletConfig config) {
        return config != null && (config == XFactory50.bmg50_sp || config == XFactory50.bmg50_fmj
                || config == XFactory50.bmg50_jhp || config == XFactory50.bmg50_ap || config == XFactory50.bmg50_du
                || config == XFactory50.bmg50_he || config == XFactory50.bmg50_sm || config == XFactory50.bmg50_black
                || config == XFactory50.bmg50_equestrian);
    }

    @Unique
    private String lspecial$selectedCaliberKey() {
        return "LSPECIALSelectedCaliber_" + index;
    }

    @Unique
    private static BulletConfig lspecial$findConfig(int id) {
        for (BulletConfig config : BulletConfig.configs) {
            if (config != null && config.id == id) return config;
        }
        return null;
    }
}
