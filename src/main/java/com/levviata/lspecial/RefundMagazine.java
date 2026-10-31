package com.levviata.lspecial;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.particle.SpentCasing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

public class RefundMagazine implements IMagazine<BulletConfig> {

    private final IMagazine<BulletConfig> original;

    public RefundMagazine(IMagazine<BulletConfig> original) {
        this.original = original;
    }

    // both guns of an akimbo pair reload on the same tick, so they share one roll per player and tick
    private static final Map<UUID, long[]> SHARED_ROLLS = new HashMap<>();

    /**
     * Counts the selected ammunition, loose in the inventory and inside ammo bags (the infinite one never runs out).
     */
    private static int[] countAmmo(IInventory inventory, BulletConfig config) {
        int loose = 0;
        int bagged = 0;
        if (inventory == null || config == null || config.ammo == null) return new int[]{0, 0};
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (config.ammo.matchesRecipe(stack, true)) {
                loose += stack.getCount();
            } else if (stack.getItem() == ModItems.ammo_bag || stack.getItem() == ModItems.ammo_bag_infinite) {
                ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(stack);
                for (int bagSlot = 0; bagSlot < bag.getSlots(); bagSlot++) {
                    ItemStack bagStack = bag.getStackInSlot(bagSlot);
                    if (!bagStack.isEmpty() && config.ammo.matchesRecipe(bagStack, true)) {
                        bagged += bagStack.getCount();
                    }
                }
            }
        }
        return new int[]{loose, bagged};
    }

    /**
     * Puts refunded ammunition back into the ammo bags that still have room, whatever doesn't fit is returned.
     */
    private static ItemStack refillBags(IInventory inventory, ItemStack refund) {
        for (int slot = 0; slot < inventory.getSizeInventory() && !refund.isEmpty(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            // the infinite bag doesn't consume anything, so there is nothing to give back to it
            if (stack.isEmpty() || stack.getItem() != ModItems.ammo_bag) continue;

            ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(stack);
            for (int bagSlot = 0; bagSlot < bag.getSlots() && !refund.isEmpty(); bagSlot++) {
                refund = bag.insertItem(bagSlot, refund, false);
            }
        }
        return refund;
    }

    /**
     * Decides once per player and tick whether the refund happens.
     */
    private static boolean rollRefund(EntityPlayer player, double chance) {
        long tick = player.world.getTotalWorldTime();
        long[] shared = SHARED_ROLLS.get(player.getUniqueID());
        if (shared != null && shared[0] == tick) {
            return shared[1] == 1L;
        }
        boolean refund = Math.random() < Math.max(0.0, Math.min(1.0, chance));
        SHARED_ROLLS.put(player.getUniqueID(), new long[]{tick, refund ? 1L : 0L});
        return refund;
    }

    /**
     * Forgets the shared rolls, they only matter within a tick.
     */
    public static void clearSharedRolls() {
        SHARED_ROLLS.clear();
    }

    public IMagazine<BulletConfig> getOriginalMagazine() {
        return original;
    }

    @Override
    public BulletConfig getType(ItemStack stack, IInventory inventory) {
        return original.getType(stack, inventory);
    }

    @Override
    public void setType(ItemStack stack, BulletConfig type) {
        original.setType(stack, type);
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return original.getCapacity(stack);
    }

    @Override
    public int getAmount(ItemStack stack, IInventory inventory) {
        return original.getAmount(stack, inventory);
    }

    @Override
    public void setAmount(ItemStack stack, int amount) {
        original.setAmount(stack, amount);
    }

    @Override
    public void useUpAmmo(ItemStack stack, IInventory inventory, int amount) {
        original.useUpAmmo(stack, inventory, amount);
    }

    @Override
    public boolean canReload(ItemStack stack, IInventory inventory) {
        return original.canReload(stack, inventory);
    }

    @Override
    public void initNewType(ItemStack stack, IInventory inventory) {
        original.initNewType(stack, inventory);
    }

    @Override
    public void reloadAction(ItemStack stack, IInventory inventory) {
        BulletConfig selectedConfig = original.getType(stack, inventory);

        int[] before = countAmmo(inventory, selectedConfig);

        original.reloadAction(stack, inventory);

        if (selectedConfig != null && selectedConfig == AMATRailgun.getBulletConfig()) return;

        int[] after = countAmmo(inventory, selectedConfig);

        int looseDebt = Math.max(0, before[0] - after[0]);
        int baggedDebt = Math.max(0, before[1] - after[1]);
        if (looseDebt + baggedDebt <= 0) {
            return;
        }

        // another IInventory can end up here through the shared receiver, check before casting
        if (!(inventory instanceof InventoryPlayer playerInventory)) {
            return;
        }
        EntityPlayer player = playerInventory.player;
        int luck = LSPECIALConfig.luck ? inst.getPlayerScorePoints(player, inst.LUCK_BOARD) : 0;
        Map<String, Double> variables = new HashMap<>();
        variables.put("luck", (double) luck);
        double chance = LSPECIALExpression.eval(LSPECIALConfig.refundAmmoFormula, variables, Math.min(luck * 0.05, 0.5));
        if (!rollRefund(player, chance)) {
            return;
        }
        if (selectedConfig == null || selectedConfig.ammo == null) {
            return;
        }

        // what came out of an ammo bag goes back into it while it has room, the rest into the inventory
        // and, if that is full too, onto the ground
        ItemStack template = selectedConfig.ammo.getStack();
        int remaining = baggedDebt;
        while (remaining > 0) {
            ItemStack part = template.copy();
            part.setCount(Math.min(remaining, part.getMaxStackSize()));
            remaining -= part.getCount();
            ItemStack leftover = refillBags(inventory, part);
            if (!leftover.isEmpty()) {
                CaliberAccess.giveToPlayer(player, leftover);
            }
        }

        remaining = looseDebt;
        while (remaining > 0) {
            ItemStack part = template.copy();
            part.setCount(Math.min(remaining, part.getMaxStackSize()));
            remaining -= part.getCount();
            CaliberAccess.giveToPlayer(player, part);
        }
        playerInventory.markDirty();
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, EntityPlayer player) {
        return original.getIconForHUD(stack, player);
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, EntityPlayer player) {
        return original.reportAmmoStateForHUD(stack, player);
    }

    @Override
    public SpentCasing getCasing(ItemStack stack, IInventory inventory) {
        return original.getCasing(stack, inventory);
    }

    @Override
    public void setAmountBeforeReload(ItemStack stack, int amount) {
        original.setAmountBeforeReload(stack, amount);
    }

    @Override
    public int getAmountBeforeReload(ItemStack stack) {
        return original.getAmountBeforeReload(stack);
    }

    @Override
    public void setAmountAfterReload(ItemStack stack, int amount) {
        original.setAmountAfterReload(stack, amount);
    }

    @Override
    public int getAmountAfterReload(ItemStack stack) {
        return original.getAmountAfterReload(stack);
    }
}
