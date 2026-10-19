package com.levviata.lspecial.mixin;

import com.hbm.items.armor.ArmorFSBPowered;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

/**
 * Applies Luck-based armor durability and powered-armor energy protection.
 */
// AI IS FREE SLAVERY
/*
 * This mixin is used because vanilla applies armor damage directly inside InventoryPlayer.damageArmor.
 * HEAD is only used for the failsafe so armor that would break can be removed before vanilla damages it.
 * TAIL is only used for the Luck bonus, after vanilla armor damage has completed.
 * The vanilla damage path is otherwise left untouched for addon compatibility.
 */
@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayerLuckArmor {

    @Unique
    private static int lspecial$getArmorDamage(float damage) {
        int armorDamage = (int) (damage / 4.0F);
        return Math.max(1, armorDamage);
    }

    @Unique
    private static boolean lspecial$unequipArmor(EntityPlayer player, ItemStack stack) {
        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;
        for (int slot = 0; slot < armorInventory.size(); slot++) {
            if (armorInventory.get(slot) != stack) continue;

            armorInventory.set(slot, ItemStack.EMPTY);
            if (!player.inventory.addItemStackToInventory(stack)) {
                player.dropItem(stack, false);
            }
            return true;
        }
        return false;
    }

    // runs before vanilla damage so the failsafe can remove armor that would break.
    @Inject(method = "damageArmor(F)V", at = @At("HEAD"))
    private void lspecial$armorFailsafe(float damage, CallbackInfo ci) {
        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorFailsafe) return;

        EntityPlayer player = ((InventoryPlayer) (Object) this).player;
        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);
        if (luck < LSPECIALConfig.luckArmorFailsafeLevel) return;

        int armorDamage = lspecial$getArmorDamage(damage);
        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;

        for (int slot = 0; slot < armorInventory.size(); slot++) {
            ItemStack stack = armorInventory.get(slot);
            if (stack == null || stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ItemArmor)) continue;

            // NTM powered armor maps item damage into battery consumption instead of durability.
            if (stack.getItem() instanceof ArmorFSBPowered) continue;
            if (!stack.isItemStackDamageable()) continue;

            int remainingDurability = stack.getMaxDamage() - stack.getItemDamage();
            if (armorDamage < remainingDurability) continue;

            if (lspecial$unequipArmor(player, stack)) {
                LSPECIALMod.info("LuckArmor", "FAILSAFE unequipped {} before break (player={}, luck={}, incomingDurabilityDamage={}, remainingDurability={})",
                        stack.getItem().getRegistryName(), player.getName(), luck, armorDamage, remainingDurability);
            }
        }
    }

    // runs after vanilla damage so Luck is only a bonus and never replaces the normal armor path.
    @Inject(method = "damageArmor(F)V", at = @At("TAIL"))
    private void lspecial$luckArmorBonus(float damage, CallbackInfo ci) {
        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorDurabilityProtection) return;

        EntityPlayer player = ((InventoryPlayer) (Object) this).player;
        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);
        if (luck <= 0) return;

        double chance = Math.min(LSPECIALConfig.luckArmorDurabilityChanceCap,
                Math.max(0.0D, luck * LSPECIALConfig.luckArmorDurabilityChancePerPoint));
        if (chance <= 0.0D) return;

        int armorDamage = lspecial$getArmorDamage(damage);
        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;

        for (int slot = 0; slot < armorInventory.size(); slot++) {
            ItemStack stack = armorInventory.get(slot);
            if (stack == null || stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ItemArmor)) continue;

            if (stack.getItem() instanceof ArmorFSBPowered poweredArmor) {
                long energyRefund = 0L;
                for (int i = 0; i < armorDamage; i++) {
                    if (player.getRNG().nextDouble() >= chance) continue;
                    poweredArmor.chargeBattery(stack, poweredArmor.consumption);
                    energyRefund += poweredArmor.consumption;
                }

                if (energyRefund > 0L) {
                    LSPECIALMod.info("LuckArmor", "POWER_ARMOR_ENERGY_REFUNDED item={} player={} luck={} chance={} energyRefunded={} charge={}",
                            stack.getItem().getRegistryName(), player.getName(), luck, chance, energyRefund,
                            poweredArmor.getCharge(stack));
                }
                continue;
            }

            if (!stack.isItemStackDamageable()) continue;

            int damageBeforeBonus = stack.getItemDamage();
            int restored = 0;
            for (int i = 0; i < armorDamage && stack.getItemDamage() > 0; i++) {
                if (player.getRNG().nextDouble() >= chance) continue;
                stack.setItemDamage(stack.getItemDamage() - 1);
                restored++;
            }

            if (restored > 0) {
                LSPECIALMod.info("LuckArmor", "DURABILITY_RESTORED item={} player={} luck={} chance={} restored={} damageBefore={} damageAfter={}",
                        stack.getItem().getRegistryName(), player.getName(), luck, chance, restored,
                        damageBeforeBonus, stack.getItemDamage());
            }
        }
    }
}
