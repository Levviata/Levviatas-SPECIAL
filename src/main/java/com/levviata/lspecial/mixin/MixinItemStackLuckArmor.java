package com.levviata.lspecial.mixin;

import com.hbm.items.armor.ArmorFSBPowered;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.LSPECIALSounds;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

// AI IS FREE SLAVERY
/*
 * itemstack.damageItem() is the confirmed armor damage path.
 * head handles the failsafe before damage is applied.
 * tail handles the Luck durability bonus after vanilla damage.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStackLuckArmor {

    @Unique
    private boolean lspecial$armorDamageCall;

    @Unique
    private static double lspecial$getChance(int luck) {
        return Math.min(LSPECIALConfig.luckArmorDurabilityChanceCap,
                Math.max(0.0D, luck * LSPECIALConfig.luckArmorDurabilityChancePerPoint));
    }

    @Inject(method = "damageItem(ILnet/minecraft/entity/EntityLivingBase;)V", at = @At("HEAD"), cancellable = true)
    private void lspecial$armorFailsafe(int amount, EntityLivingBase entity, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;

        lspecial$armorDamageCall = false;
        if (!(entity instanceof EntityPlayer player)) {
            return;
        }
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArmor)) {
            return;
        }
        if (amount <= 0) {
            return;
        }

        if (stack.hasTagCompound() && stack.getTagCompound().getBoolean("LSPECIALArmorFailsafe")) {
            int savedDamage = stack.getTagCompound().getInteger("LSPECIALArmorFailsafeDamage");
            if (stack.getItemDamage() == savedDamage) {
                return;
            }

            stack.getTagCompound().removeTag("LSPECIALArmorFailsafe");
            stack.getTagCompound().removeTag("LSPECIALArmorFailsafeDamage");
        }

        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);

        LSPECIALMod.info("LuckArmor",
                "damageItem HEAD item={} player={} amount={} damage={} maxDamage={} luck={}",
                stack.getItem().getRegistryName(), player.getName(), amount,
                stack.getItemDamage(), stack.getMaxDamage(), luck);

        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorFailsafe) {
            return;
        }
        if (luck < LSPECIALConfig.luckArmorFailsafeLevel) {
            return;
        }

        if (stack.getItem() instanceof ArmorFSBPowered) {
            return;
        }

        if (!stack.isItemStackDamageable()) {
            return;
        }

        int remainingDurability = stack.getMaxDamage() - stack.getItemDamage();

        if (amount < remainingDurability) {
            lspecial$armorDamageCall = true;
            return;
        }

        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound tag = stack.getTagCompound();
        tag.setBoolean("LSPECIALArmorFailsafe", true);
        tag.setInteger("LSPECIALArmorFailsafeDamage", stack.getItemDamage());

        if (stack.getItem() instanceof ItemArmor armor) {
            player.playSound(armor.getArmorMaterial().getSoundEvent(), 1.0F, 1.0F);
        }

        LSPECIALMod.info("LuckArmor",
                "FAILSAFE protected {} from breaking (player={}, luck={}, incomingDurabilityDamage={}, remainingDurability={})",
                stack.getItem().getRegistryName(), player.getName(), luck,
                amount, remainingDurability);

        ci.cancel();
    }

    @Inject(method = "damageItem(ILnet/minecraft/entity/EntityLivingBase;)V", at = @At("TAIL"))
    private void lspecial$luckArmorBonus(int amount, EntityLivingBase entity, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;

        if (!(entity instanceof EntityPlayer player)) {
            return;
        }
        if (!lspecial$armorDamageCall) {
            return;
        }
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArmor)) {
            return;
        }
        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorDurabilityProtection) {
            return;
        }

        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);
        if (luck <= 0) {
            return;
        }

        double chance = lspecial$getChance(luck);
        if (chance <= 0.0D) {
            return;
        }

        LSPECIALMod.info("LuckArmor",
                "damageItem TAIL item={} player={} amount={} luck={} chance={}",
                stack.getItem().getRegistryName(), player.getName(), amount, luck, chance);

        if (!stack.isItemStackDamageable()) {
            return;
        }

        int damageBefore = stack.getItemDamage();
        int restored = 0;

        for (int i = 0; i < amount; i++) {
            if (stack.getItemDamage() <= 0) {
                break;
            }

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