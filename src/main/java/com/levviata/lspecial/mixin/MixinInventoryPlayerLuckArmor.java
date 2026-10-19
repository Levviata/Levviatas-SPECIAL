package com.levviata.lspecial.mixin;

import com.hbm.items.armor.ArmorFSBPowered;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemArmor;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

/**
 * Applies Luck-based armor durability and powered-armor energy protection.
 */
@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayerLuckArmor {

    @Inject(method = "damageArmor(F)V", at = @At("HEAD"))
    private void lspecial$armorFailsafe(float damage, CallbackInfo ci) {
        EntityPlayer player = ((InventoryPlayer) (Object) this).player;
        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorFailsafe) return;

        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);
        if (luck < LSPECIALConfig.luckArmorFailsafeLevel) return;

        int armorDamage = (int) (damage / 4.0F);
        if (armorDamage < 1) armorDamage = 1;

        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;
        for (int slot = 0; slot < armorInventory.size(); slot++) {
            ItemStack stack = armorInventory.get(slot);
            if (stack.isEmpty() || stack.getItem() instanceof ArmorFSBPowered) continue;
            if (!(stack.getItem() instanceof ItemArmor)) continue;
            if (!stack.isItemStackDamageable()) continue;

            int remainingDurability = stack.getMaxDamage() - stack.getItemDamage();
            if (armorDamage < remainingDurability) continue;

            armorInventory.set(slot, ItemStack.EMPTY);
            if (!player.inventory.addItemStackToInventory(stack)) {
                player.dropItem(stack, false);
            }

            LSPECIALMod.info("LuckArmor", "FAILSAFE unequipped {} before break (player={}, luck={}, incomingDurabilityDamage={}, remainingDurability={})",
                    stack.getItem().getRegistryName(), player.getName(), luck, armorDamage, remainingDurability);
        }
    }

    @Inject(method = "damageArmor(F)V", at = @At("TAIL"))
    private void lspecial$protectArmorDurability(float damage, CallbackInfo ci) {
        EntityPlayer player = ((InventoryPlayer) (Object) this).player;
        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorDurabilityProtection) return;

        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);
        if (luck <= 0) return;

        double chance = Math.min(LSPECIALConfig.luckArmorDurabilityChanceCap,
                Math.max(0.0D, luck * LSPECIALConfig.luckArmorDurabilityChancePerPoint));
        if (chance <= 0.0D) return;

        int armorDamage = (int) (damage / 4.0F);
        if (armorDamage < 1) armorDamage = 1;

        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;
        for (ItemStack stack : armorInventory) {
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ArmorFSBPowered poweredArmor) {
                int prevented = 0;
                for (int i = 0; i < armorDamage; i++) {
                    if (player.getRNG().nextDouble() < chance) prevented++;
                }
                if (prevented > 0) {
                    long energyRefund = (long) prevented * poweredArmor.consumption;
                    poweredArmor.chargeBattery(stack, energyRefund);
                    LSPECIALMod.info("LuckArmor", "POWER_ARMOR_ENERGY_REFUNDED item={} player={} luck={} chance={} preventedDamage={} energyRefunded={} chargeAfter={}",
                            stack.getItem().getRegistryName(), player.getName(), luck, chance, prevented, energyRefund,
                            poweredArmor.getCharge(stack));
                }
                continue;
            }

            if (!stack.isItemStackDamageable()) continue;

            int prevented = 0;
            for (int i = 0; i < armorDamage && stack.getItemDamage() > 0; i++) {
                if (player.getRNG().nextDouble() < chance) prevented++;
            }

            if (prevented > 0) {
                stack.setItemDamage(Math.max(0, stack.getItemDamage() - prevented));
                LSPECIALMod.info("LuckArmor", "DURABILITY_PROTECTED item={} player={} luck={} chance={} preventedDamage={}",
                        stack.getItem().getRegistryName(), player.getName(), luck, chance, prevented);
            }
        }
    }

}
