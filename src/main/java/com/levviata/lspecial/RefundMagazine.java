package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.mags.IMagazine;
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
    public void reloadAction(ItemStack stack, IInventory inventory) { // bug: when reloading with less than a full mags worth of bullet, the gun tries to reload twice doubling ammo and stopping the reloading for a sec

        int before = original.getAmount(stack, inventory);

        original.reloadAction(stack, inventory);

        int after = original.getAmount(stack, inventory);

        int ammoDebt = Math.abs(after - before);

        if (ammoDebt <= 0) {
            return;
        }

        int luck = LSPECIALConfig.luck ? inst.getLuckScore().getScorePoints() : 0;
        Map<String, Double> vars = new HashMap<>();
        vars.put("luck", (double)luck);
        double chance = LSPECIALExpression.eval(LSPECIALConfig.refundAmmoFormula, vars, Math.min(luck * 0.05, 0.5));
        boolean refundAmmo = Math.random() < Math.max(0.0, Math.min(1.0, chance));

        if (!refundAmmo) {
            return;
        }

        BulletConfig config = original.getType(stack, inventory);

        if (config == null || config.ammo == null) {
            return;
        }

       // int ogamount = config.ammo.getStack().getCount();
        ItemStack refund = config.ammo.getStack().copy();
        refund.setCount(ammoDebt);

        if (inventory instanceof InventoryPlayer) {
            EntityPlayer player = ((InventoryPlayer) inventory).player;
            player.inventory.addItemStackToInventory(refund);
        }
/*      no work, wontfix for now
        if (getAmount(stack, inventory) < getCapacity(stack) && config.ammo.getStack().getCount() < getCapacity(stack)) {
            config.ammo.getStack().setCount(getCapacity(stack));
        }

        config.ammo.getStack().setCount(ogAmount);*/
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
