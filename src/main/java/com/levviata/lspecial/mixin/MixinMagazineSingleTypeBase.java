package com.levviata.lspecial.mixin;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.levviata.lspecial.AMATRailgun;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.PlayerAcceptedBullets;
import com.levviata.lspecial.SPECIALPlayerContext;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.entity.player.EntityPlayer;
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

import java.util.List;

/**
 * Applies selected-caliber persistence and configured capacities to NTM single-type magazines.<br>
 * <br>
 * Why: acceptedBullets is one mutable list per magazine definition, shared by every copy of the gun, so alternate
 * calibers added to it would be unlocked for all players. How/where: the constructor swaps the field for a
 * PlayerAcceptedBullets view that reads the player from SPECIALPlayerContext. Risks: NTM mods that reassign the field
 * replace the view (they only touch their own dummy magazines), and outside a gun operation the view is base-only.
 * Avoidable: not without NTM giving getType a player argument.
 */
@Mixin(value = MagazineSingleTypeBase.class, remap = false)
public abstract class MixinMagazineSingleTypeBase {

    @Shadow
    public List<BulletConfig> acceptedBullets;
    @Shadow
    public int index;
    @Shadow
    public int capacity;

    /**
     * Wraps NTM's shared acceptedBullets list so alternate calibers only show up for players who may use them.
     */
    @Inject(method = "<init>(II)V", at = @At("RETURN"), remap = false)
    private void lspecial$installPlayerAcceptedBullets(int index, int capacity, CallbackInfo callback) {
        if (acceptedBullets != null && !(acceptedBullets instanceof PlayerAcceptedBullets)) {
            acceptedBullets = new PlayerAcceptedBullets(acceptedBullets);
        }
    }

    /**
     * Returns the explicitly selected caliber even when NTM's ordinary lookup would fall back to the first config.
     */
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

    /**
     * Loads only the selected ammunition and uses getCapacity so Intelligence capacity upgrades can fill completely.
     */
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

    /**
     * Applies the Intelligence higher-perk capacity without changing the shared base magazine capacity.
     */
    @Inject(method = "getCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$applyHigherPerkCapacity(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return;

        String earlyRegistryName = stack.getItem().getRegistryName().toString();

        if ("hbm:gun_amat".equals(earlyRegistryName) && stack.hasTagCompound()) {
            BulletConfig capsule = AMATRailgun.getBulletConfig();
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

        if (!LSPECIALConfig.intelligence) {
            return;
        }

        EntityPlayer player = SPECIALPlayerContext.getPlayer();
        if (!LSPECIALMod.hasIntelligenceLevel(player, LSPECIALMod.HIGHER_PERK)
                || stack.getItem().getRegistryName() == null) {
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
                && LSPECIALConfig.modifyGuns && SPECIALScoreboard.inst.getPlayerScorePoints(
                player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) >= 15) {
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
