package com.levviata.lspecial.mixin;

import com.hbm.handler.HbmKeybinds;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.levviata.lspecial.CaliberAccess;
import com.levviata.lspecial.SPECIALPlayerContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bridges ItemGunBaseNT's player/LambdaContext into ItemStack-only NTM methods.<br>
 * <br>
 * ItemGunBaseNT creates LambdaContext from this exact entity/inventory before
 * executing the gun callbacks. Receiver and magazine methods such as
 * getDelayAfterFire/getCapacity only receive an ItemStack, so they cannot
 * receive LambdaContext directly. This keeps the player associated with the
 * current gun operation available to those mixins without using shared player
 * fields on Receiver or Magazine instances.
 */
@Mixin(value = ItemGunBaseNT.class, remap = false)
public abstract class MixinItemGunBaseNT {

    @Inject(method = "handleKeybind(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/inventory/IInventory;Lnet/minecraft/item/ItemStack;Lcom/hbm/handler/HbmKeybinds$EnumKeybind;Z)V", at = @At("HEAD"), remap = false)
    private void lspecial$setPlayerContext(EntityLivingBase entity, IInventory inventory, ItemStack stack,
                                           HbmKeybinds.EnumKeybind keybind, boolean newState,
                                           CallbackInfo callback) {
        EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        SPECIALPlayerContext.setContext(player, stack);
        // takes back an alternate caliber the player lost access to before it can be fired
        CaliberAccess.validateSelection(player, stack);
    }

    @Inject(method = "handleKeybind(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/inventory/IInventory;Lnet/minecraft/item/ItemStack;Lcom/hbm/handler/HbmKeybinds$EnumKeybind;Z)V", at = @At("RETURN"), remap = false)
    private void lspecial$clearPlayerContext(EntityLivingBase entity, IInventory inventory, ItemStack stack,
                                             HbmKeybinds.EnumKeybind keybind, boolean newState,
                                             CallbackInfo callback) {
        SPECIALPlayerContext.clear();
    }

    @Inject(method = "onUpdate(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;IZ)V", at = @At("HEAD"), remap = false)
    private void lspecial$setUpdatePlayerContext(ItemStack stack, World world, Entity entity, int slot,
                                                 boolean isHeld, CallbackInfo callback) {
        EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        SPECIALPlayerContext.setContext(player, stack);
        // once a second per slot is plenty, handleKeybind covers the moment of firing
        if (player != null && !world.isRemote && (player.ticksExisted + slot) % 20 == 0) {
            CaliberAccess.validateSelection(player, stack);
        }
    }

    @Inject(method = "onUpdate(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;IZ)V", at = @At("RETURN"), remap = false)
    private void lspecial$clearUpdatePlayerContext(ItemStack stack, World world, Entity entity, int slot,
                                                   boolean isHeld, CallbackInfo callback) {
        SPECIALPlayerContext.clear();
    }
}
