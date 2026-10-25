package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Holds the player associated with the currently executing NTM gun operation.<br>
 * <br>
 * ItemGunBaseNT creates LambdaContext with the actual player before invoking
 * the gun callbacks. The ItemStack-only Receiver/Magazine methods do not carry
 * that context in their method signature, so MixinItemGunBaseNT bridges the
 * same player into this thread-local while the gun operation is executing.
 */
public final class SPECIALPlayerContext {

    private static final ThreadLocal<EntityPlayer> CURRENT_PLAYER = new ThreadLocal<>();
    private static final ThreadLocal<ItemStack> CURRENT_STACK = new ThreadLocal<>();

    private SPECIALPlayerContext() {
    }

    public static EntityPlayer getPlayer() {
        return CURRENT_PLAYER.get();
    }

    /**
     * Returns the gun stack of the current operation, or null outside of one.
     */
    public static ItemStack getStack() {
        return CURRENT_STACK.get();
    }

    public static void setPlayer(EntityPlayer player) {
        setContext(player, null);
    }

    /**
     * Sets the player and gun stack of the running operation, or clears both when the player is null.
     */
    public static void setContext(EntityPlayer player, ItemStack stack) {
        if (player == null) {
            CURRENT_PLAYER.remove();
            CURRENT_STACK.remove();
        } else {
            CURRENT_PLAYER.set(player);
            if (stack == null) {
                CURRENT_STACK.remove();
            } else {
                CURRENT_STACK.set(stack);
            }
        }
    }

    public static void clear() {
        CURRENT_PLAYER.remove();
        CURRENT_STACK.remove();
    }
}
