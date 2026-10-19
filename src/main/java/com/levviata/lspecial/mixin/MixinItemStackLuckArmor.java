package com.levviata.lspecial.mixin;

import com.hbm.items.armor.ArmorFSBPowered;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

// AI IS FREE SLAVERY
/*
 * ItemStack.damageItem() is used here because it is the confirmed damage path for armor.
 * HEAD handles the failsafe before durability or power is consumed.
 * TAIL handles the Luck bonus after normal damage has happened.
 * The vanilla damage method is only cancelled when the failsafe needs to protect breaking armor.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStackLuckArmor {

    @Unique
    private boolean lspecial$armorDamageCall;

    @Unique
    private boolean lspecial$poweredArmor;

    @Unique
    private long lspecial$poweredChargeBefore;

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

    @Unique
    private static double lspecial$getChance(int luck) {
        return Math.min(LSPECIALConfig.luckArmorDurabilityChanceCap,
                Math.max(0.0D, luck * LSPECIALConfig.luckArmorDurabilityChancePerPoint));
    }

    @Inject(method = "damageItem(ILnet/minecraft/entity/EntityLivingBase;)V", at = @At("HEAD"), cancellable = true)
    private void lspecial$armorFailsafe(int amount, EntityLivingBase entity, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;

        lspecial$armorDamageCall = false;
        lspecial$poweredArmor = false;
        lspecial$poweredChargeBefore = 0L;

        if (!(entity instanceof EntityPlayer player)) return;
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArmor)) return;
        if (amount <= 0) return;

        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);

        LSPECIALMod.info("LuckArmor",
                "damageItem HEAD item={} player={} amount={} damage={} maxDamage={} luck={}",
                stack.getItem().getRegistryName(), player.getName(), amount,
                stack.getItemDamage(), stack.getMaxDamage(), luck);

        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorFailsafe) return;
        if (luck < LSPECIALConfig.luckArmorFailsafeLevel) return;

        // NTM powered armor maps ItemStack damage into battery energy consumption.
        if (stack.getItem() instanceof ArmorFSBPowered poweredArmor) {
            lspecial$poweredArmor = true;
            lspecial$poweredChargeBefore = poweredArmor.getCharge(stack);

            long requiredEnergy = (long) amount * poweredArmor.consumption;

            LSPECIALMod.info("LuckArmor",
                    "POWER_ARMOR_FAILSAFE item={} player={} charge={} requiredEnergy={} amount={} consumption={}",
                    stack.getItem().getRegistryName(), player.getName(),
                    lspecial$poweredChargeBefore, requiredEnergy, amount,
                    poweredArmor.consumption);

            if (lspecial$poweredChargeBefore < requiredEnergy
                    && lspecial$unequipArmor(player, stack)) {
                LSPECIALMod.info("LuckArmor",
                        "POWER_ARMOR_FAILSAFE unequipped {} before break (player={}, luck={}, charge={}, requiredEnergy={})",
                        stack.getItem().getRegistryName(), player.getName(), luck,
                        lspecial$poweredChargeBefore, requiredEnergy);

                ci.cancel();
            }

            return;
        }

        if (!stack.isItemStackDamageable()) return;

        int remainingDurability = stack.getMaxDamage() - stack.getItemDamage();

        if (amount >= remainingDurability) {
            if (lspecial$unequipArmor(player, stack)) {
                LSPECIALMod.info("LuckArmor",
                        "FAILSAFE unequipped {} before break (player={}, luck={}, incomingDurabilityDamage={}, remainingDurability={})",
                        stack.getItem().getRegistryName(), player.getName(), luck,
                        amount, remainingDurability);

                ci.cancel();
                return;
            }
        }

        lspecial$armorDamageCall = true;
    }

    @Inject(method = "damageItem(ILnet/minecraft/entity/EntityLivingBase;)V", at = @At("TAIL"))
    private void lspecial$luckArmorBonus(int amount, EntityLivingBase entity, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;

        if (!(entity instanceof EntityPlayer player)) return;
        if (!lspecial$armorDamageCall && !lspecial$poweredArmor) return;
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArmor)) return;
        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorDurabilityProtection) return;

        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);
        if (luck <= 0) return;

        double chance = lspecial$getChance(luck);
        if (chance <= 0.0D) return;

        LSPECIALMod.info("LuckArmor",
                "damageItem TAIL item={} player={} amount={} luck={} chance={}",
                stack.getItem().getRegistryName(), player.getName(), amount, luck, chance);

        // NTM powered armor maps ItemStack damage into battery energy consumption.
        if (stack.getItem() instanceof ArmorFSBPowered poweredArmor) {
            int successfulRolls = 0;

            for (int i = 0; i < amount; i++) {
                if (player.getRNG().nextDouble() < chance) {
                    poweredArmor.chargeBattery(stack, poweredArmor.consumption);
                    successfulRolls++;
                }
            }

            if (successfulRolls > 0) {
                long energyRefunded = (long) successfulRolls * poweredArmor.consumption;

                LSPECIALMod.info("LuckArmor",
                        "POWER_ARMOR_ENERGY_REFUNDED item={} player={} luck={} chance={} energyRefunded={} chargeBefore={} chargeAfter={}",
                        stack.getItem().getRegistryName(), player.getName(), luck, chance,
                        energyRefunded, lspecial$poweredChargeBefore,
                        poweredArmor.getCharge(stack));
            }

            return;
        }

        if (!stack.isItemStackDamageable()) return;

        int damageBefore = stack.getItemDamage();
        int restored = 0;

        for (int i = 0; i < amount; i++) {
            if (stack.getItemDamage() <= 0) break;

            if (player.getRNG().nextDouble() < chance) {
                stack.setItemDamage(stack.getItemDamage() - 1);
                restored++;
            }
        }

        if (restored > 0) {
            LSPECIALMod.info("LuckArmor",
                    "DURABILITY_RESTORED item={} player={} luck={} chance={} restored={} damageBefore={} damageAfter={}",
                    stack.getItem().getRegistryName(), player.getName(), luck, chance,
                    restored, damageBefore, stack.getItemDamage());
        }
    }
}