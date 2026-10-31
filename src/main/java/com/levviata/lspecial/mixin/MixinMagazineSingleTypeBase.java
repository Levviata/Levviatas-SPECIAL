package com.levviata.lspecial.mixin;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.levviata.lspecial.AMATRailgun;
import com.levviata.lspecial.CaliberAccess;
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

import java.util.ArrayList;
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
     * Only offers ammunition the selected caliber allows, so canReload and initNewType agree with what actually gets loaded.
     */
    @Inject(method = "getFirstConfig(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;)Lcom/hbm/items/weapon/sedna/BulletConfig;", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$restrictFirstConfig(ItemStack stack, IInventory inventory, CallbackInfoReturnable<BulletConfig> callback) {
        if (stack == null || inventory == null || !lspecial$hasValidSelection(stack)) {
            return;
        }

        MagazineSingleTypeBase magazine = (MagazineSingleTypeBase) (Object) this;
        BulletConfig result;
        if (magazine.getAmount(stack, null) > 0) {
            BulletConfig loaded = magazine.getType(stack, null);
            result = loaded != null && lspecial$hasAmmo(inventory, loaded) ? loaded : null;
        } else {
            result = lspecial$findLoadable(inventory, lspecial$candidates(stack));
        }
        callback.setReturnValue(result);
    }

    /**
     * Loads the selected ammunition and uses getCapacity so Intelligence and caliber capacities can fill completely.
     */
    @Inject(method = "standardReload(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$reloadSelectedCaliber(ItemStack stack, IInventory inventory, int loadLimit, CallbackInfo callback) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        MagazineSingleTypeBase magazine = (MagazineSingleTypeBase) (Object) this;
        if (!lspecial$hasValidSelection(stack) && magazine.getCapacity(stack) == capacity) {
            return;
        }

        callback.cancel();
        List<BulletConfig> candidates = lspecial$candidates(stack);
        if (candidates.isEmpty()) {
            return;
        }

        int loaded = magazine.getAmount(stack, null);
        BulletConfig selected;
        if (inventory == null) {
            selected = loaded > 0 ? magazine.getType(stack, null) : candidates.get(0);
        } else if (loaded > 0) {
            selected = magazine.getType(stack, null);
        } else {
            selected = lspecial$findLoadable(inventory, candidates);
        }
        if (selected == null) {
            return;
        }

        // the type goes first, the capacity can depend on it
        magazine.setType(stack, selected);
        int actualCapacity = magazine.getCapacity(stack);
        if (inventory == null) {
            magazine.setAmount(stack, actualCapacity);
            return;
        }

        int room = Math.max(0, actualCapacity - loaded);
        int reloadCount = Math.max(1, selected.ammoReloadCount);
        if (room <= 0 || selected.ammo == null) {
            return;
        }

        int limit = loadLimit;
        for (int slot = 0; slot < inventory.getSizeInventory() && room > 0 && limit > 0; slot++) {
            ItemStack inventoryStack = inventory.getStackInSlot(slot);
            if (inventoryStack.isEmpty()) {
                continue;
            }

            if (selected.ammo.matchesRecipe(inventoryStack, true)) {
                int needed = Math.min(Math.min((int) Math.ceil(room / (double) reloadCount), inventoryStack.getCount()), limit);
                if (needed > 0) {
                    int added = Math.min(room, needed * reloadCount);
                    loaded += added;
                    magazine.setAmount(stack, loaded);
                    inventory.decrStackSize(slot, needed);
                    room -= added;
                    limit -= needed;
                }
                continue;
            }

            if (inventoryStack.getItem() != ModItems.ammo_bag && inventoryStack.getItem() != ModItems.ammo_bag_infinite) {
                continue;
            }

            boolean infiniteBag = inventoryStack.getItem() == ModItems.ammo_bag_infinite;
            ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(inventoryStack);
            for (int bagSlot = 0; bagSlot < bag.getSlots() && room > 0 && limit > 0; bagSlot++) {
                ItemStack bagStack = bag.getStackInSlot(bagSlot);
                if (bagStack.isEmpty() || !selected.ammo.matchesRecipe(bagStack, true)) {
                    continue;
                }

                int available = infiniteBag ? 9_999 : bagStack.getCount();
                int needed = Math.min(Math.min((int) Math.ceil(room / (double) reloadCount), available), limit);
                if (needed <= 0) {
                    continue;
                }

                int added = Math.min(room, needed * reloadCount);
                loaded += added;
                magazine.setAmount(stack, loaded);
                if (!infiniteBag) {
                    bag.extractItem(bagSlot, needed, false);
                }
                room -= added;
                limit -= needed;
            }
        }
    }

    /**
     * Applies caliber and Intelligence capacities without changing the shared base magazine capacity.
     */
    @Inject(method = "getCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$applyHigherPerkCapacity(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return;
        }

        String registryName = stack.getItem().getRegistryName().toString();
        BulletConfig type = lspecial$effectiveType(stack);

        // the capacity follows the caliber that is loaded or selected, not only the selection key
        if ("hbm:gun_amat".equals(registryName) && AMATRailgun.isRailgunConfig(type)) {
            callback.setReturnValue(1);
            return;
        }

        if (("hbm:gun_star_f".equals(registryName) || "hbm:gun_star_f_akimbo".equals(registryName)) && lspecial$isBmgConfig(type)) {
            callback.setReturnValue(10);
            return;
        }

        if (!LSPECIALConfig.intelligence) {
            return;
        }

        EntityPlayer player = SPECIALPlayerContext.getPlayer();
        if (!LSPECIALMod.hasIntelligenceLevel(player, LSPECIALMod.HIGHER_PERK)) {
            return;
        }

        if (LSPECIALConfig.fatmanHigherPerk && "hbm:gun_fatman".equals(registryName)) {
            callback.setReturnValue(LSPECIALConfig.fatmanMagazineCapacity);
        } else if (LSPECIALConfig.laserPewPewHigherPerk && "hbm:gun_laser_pistol_pew_pew".equals(registryName)) {
            callback.setReturnValue(LSPECIALConfig.laserPewPewMagazineCapacity);
        } else if (LSPECIALConfig.carbineHigherPerkCapacity && "hbm:gun_carbine".equals(registryName)) {
            callback.setReturnValue(LSPECIALConfig.carbineMagazineCapacity);
        } else if (("hbm:gun_star_f".equals(registryName) || "hbm:gun_star_f_akimbo".equals(registryName))
                && LSPECIALConfig.modifyGuns) {
            callback.setReturnValue(LSPECIALConfig.starFCapacity);
        }
    }

    /**
     * Returns the caliber the magazine is set to: the selected one if there is one, otherwise what NTM has stored.
     */
    @Unique
    private BulletConfig lspecial$effectiveType(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(lspecial$selectedCaliberKey())) {
            BulletConfig selected = lspecial$findConfig(stack.getTagCompound().getInteger(lspecial$selectedCaliberKey()));
            if (selected != null) {
                return selected;
            }
        }
        int raw = MagazineSingleTypeBase.getMagType(stack, index);
        return raw >= 0 && raw < BulletConfig.configs.size() ? BulletConfig.configs.get(raw) : null;
    }

    /**
     * Returns whether the stack has a selected caliber the current player may still use.
     */
    @Unique
    private boolean lspecial$hasValidSelection(ItemStack stack) {
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(lspecial$selectedCaliberKey())) {
            return false;
        }
        BulletConfig selected = lspecial$findConfig(stack.getTagCompound().getInteger(lspecial$selectedCaliberKey()));
        return selected != null && acceptedBullets != null && acceptedBullets.contains(selected);
    }

    /**
     * Returns the calibers that can be loaded right now: the selected one (the whole alternate group for the Star F),
     * or every caliber the player can use when nothing is selected.
     */
    @Unique
    private List<BulletConfig> lspecial$candidates(ItemStack stack) {
        List<BulletConfig> result = new ArrayList<>();
        if (acceptedBullets == null) {
            return result;
        }
        if (!lspecial$hasValidSelection(stack)) {
            result.addAll(acceptedBullets);
            return result;
        }

        BulletConfig selected = lspecial$findConfig(stack.getTagCompound().getInteger(lspecial$selectedCaliberKey()));
        String itemId = stack.getItem().getRegistryName() == null ? "" : stack.getItem().getRegistryName().toString();
        if (CaliberAccess.isStarF(itemId) && !PlayerAcceptedBullets.baseOf(acceptedBullets).contains(selected)) {
            // the Star F switches between groups, any round of the selected one can be loaded
            for (BulletConfig config : acceptedBullets) {
                if (!PlayerAcceptedBullets.baseOf(acceptedBullets).contains(config)) {
                    result.add(config);
                }
            }
        } else {
            result.add(selected);
        }
        return result;
    }

    /**
     * Returns the first candidate with ammunition in the inventory, checking the slots in order like NTM does.
     */
    @Unique
    private static BulletConfig lspecial$findLoadable(IInventory inventory, List<BulletConfig> candidates) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack inventoryStack = inventory.getStackInSlot(slot);
            if (inventoryStack.isEmpty()) {
                continue;
            }
            for (BulletConfig config : candidates) {
                if (config != null && config.ammo != null && config.ammo.matchesRecipe(inventoryStack, true)) {
                    return config;
                }
            }
            if (inventoryStack.getItem() == ModItems.ammo_bag || inventoryStack.getItem() == ModItems.ammo_bag_infinite) {
                ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(inventoryStack);
                for (int bagSlot = 0; bagSlot < bag.getSlots(); bagSlot++) {
                    ItemStack bagStack = bag.getStackInSlot(bagSlot);
                    if (bagStack.isEmpty()) {
                        continue;
                    }
                    for (BulletConfig config : candidates) {
                        if (config != null && config.ammo != null && config.ammo.matchesRecipe(bagStack, true)) {
                            return config;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Unique
    private static boolean lspecial$hasAmmo(IInventory inventory, BulletConfig config) {
        return lspecial$findLoadable(inventory, java.util.Collections.singletonList(config)) != null;
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
        // ids are list indexes, only scan if something shifted them
        if (id >= 0 && id < BulletConfig.configs.size() && BulletConfig.configs.get(id) != null && BulletConfig.configs.get(id).id == id) {
            return BulletConfig.configs.get(id);
        }
        for (BulletConfig config : BulletConfig.configs) {
            if (config != null && config.id == id) return config;
        }
        return null;
    }
}
