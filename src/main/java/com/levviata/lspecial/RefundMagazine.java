package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.particle.SpentCasing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

public class RefundMagazine implements IMagazine<BulletConfig> {

    private final IMagazine<BulletConfig> original;

    public RefundMagazine(IMagazine<BulletConfig> original) {
        this.original = original;
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

        int ammoBefore = countAmmo(inventory, selectedConfig);

        original.reloadAction(stack, inventory);

        if (selectedConfig != null && selectedConfig == AMATRailgun.getBulletConfig()) return;

        int ammoAfter = countAmmo(inventory, selectedConfig);

        int ammoDebt = Math.max(0, ammoBefore - ammoAfter);
        if (ammoDebt <= 0) {
            return;
        }

        int luck = LSPECIALConfig.luck ? inst.getLuckScore().getScorePoints() : 0;
        Map<String, Double> variables = new HashMap<>();
        variables.put("luck", (double) luck);
        double chance = LSPECIALExpression.eval(LSPECIALConfig.refundAmmoFormula, variables, Math.min(luck * 0.05, 0.5));
        if (Math.random() >= Math.max(0.0, Math.min(1.0, chance))) {
            return;
        }
        if (selectedConfig == null || selectedConfig.ammo == null || !(inventory instanceof InventoryPlayer)) {
            return;
        }

        EntityPlayer player = ((InventoryPlayer) inventory).player;
        ItemStack refund = selectedConfig.ammo.getStack().copy();
        refund.setCount(ammoDebt);
        player.inventory.addItemStackToInventory(refund);
        if (!refund.isEmpty()) {
            player.dropItem(refund, false);
        }
    }

    /** counts only the selected ammunition, including stacks stored inside NTM ammo bags. */
    private static int countAmmo(IInventory inventory, BulletConfig config) {
        if (inventory == null || config == null || config.ammo == null) return 0;
        int count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (config.ammo.matchesRecipe(stack, true)) {
                count += stack.getCount();
            } else if (stack.getItem() == ModItems.ammo_bag || stack.getItem() == ModItems.ammo_bag_infinite) {
                ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(stack);
                for (int bagSlot = 0; bagSlot < bag.getSlots(); bagSlot++) {
                    ItemStack bagStack = bag.getStackInSlot(bagSlot);
                    if (!bagStack.isEmpty() && config.ammo.matchesRecipe(bagStack, true)) {
                        count += bagStack.getCount();
                    }
                }
            }
        }
        return count;
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
