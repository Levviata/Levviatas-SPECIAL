package com.levviata.lspecial.mixin;

import com.hbm.items.armor.ArmorFSBPowered;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

/** Applies Luck-based armor durability and powered-armor energy protection. */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayerLuckArmor {

    @Redirect(
            method = "damageArmor(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;damageItem(ILnet/minecraft/entity/EntityLivingBase;)V"
            )
    )
    private void lspecial$protectArmorDurability(ItemStack stack, int amount, EntityLivingBase entity) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (stack == null || stack.isEmpty()) return;
        if (amount <= 0) return;

        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);

        // NTM powered armor maps ItemStack damage into battery energy consumption.
        if (stack.getItem() instanceof ArmorFSBPowered) {
            ArmorFSBPowered poweredArmor = (ArmorFSBPowered) stack.getItem();
            long chargeBefore = poweredArmor.getCharge(stack);
            if (LSPECIALConfig.luck && LSPECIALConfig.luckArmorDurabilityProtection && luck > 0) {
                double chance = Math.min(LSPECIALConfig.luckArmorDurabilityChanceCap,
                        Math.max(0.0D, luck * LSPECIALConfig.luckArmorDurabilityChancePerPoint));
                if (player.getRNG().nextDouble() < chance) {
                    stack.damageItem(amount, entity);
                    long refunded = lspecial$refundPoweredArmorEnergy(poweredArmor, stack, chargeBefore);
                    LSPECIALMod.info("[LuckArmor] POWER_ARMOR_ENERGY_REFUNDED item={} player={} luck={} chance={} energyRefunded={} chargeBefore={} chargeAfter={}",
                            stack.getItem().getRegistryName(), player.getName(), luck, chance, refunded,
                            chargeBefore, poweredArmor.getCharge(stack));
                    return;
                }
            }

            stack.damageItem(amount, entity);
            return;
        }

        if (!stack.isItemStackDamageable()) {
            stack.damageItem(amount, entity);
            return;
        }

        int remainingDurability = stack.getMaxDamage() - stack.getItemDamage();
        if (LSPECIALConfig.luck && LSPECIALConfig.luckArmorFailsafe
                && luck >= LSPECIALConfig.luckArmorFailsafeLevel
                && amount >= remainingDurability) {
            if (lspecial$unequipArmor(player, stack)) {
                LSPECIALMod.info("[LuckArmor] FAILSAFE unequipped {} before break (player={}, luck={}, incomingDurabilityDamage={}, remainingDurability={})",
                        stack.getItem().getRegistryName(), player.getName(), luck, amount, remainingDurability);
                return;
            }
        }

        if (LSPECIALConfig.luck && LSPECIALConfig.luckArmorDurabilityProtection && luck > 0) {
            double chance = Math.min(LSPECIALConfig.luckArmorDurabilityChanceCap,
                    Math.max(0.0D, luck * LSPECIALConfig.luckArmorDurabilityChancePerPoint));
            for (int i = 0; i < amount; i++) {
                if (player.getRNG().nextDouble() < chance) {
                    // dont damage it
                } else {
                    stack.damageItem(1, entity);
                }
            }
            return;
        }

        stack.damageItem(amount, entity);
    }

    @Unique
    private static long lspecial$refundPoweredArmorEnergy(ArmorFSBPowered poweredArmor, ItemStack stack,
                                                          long chargeBefore) {
        long chargeAfterDamage = poweredArmor.getCharge(stack);
        long energySpent = Math.max(0L, chargeBefore - chargeAfterDamage);
        if (energySpent > 0L) poweredArmor.chargeBattery(stack, energySpent);
        return energySpent;
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
}
