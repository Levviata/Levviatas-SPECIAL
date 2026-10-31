package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import com.levviata.lspecial.client.ClientPlayerAccess;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayDeque;

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

    private static final ThreadLocal<ArrayDeque<Object[]>> SAVED = ThreadLocal.withInitial(ArrayDeque::new);

    private SPECIALPlayerContext() {
    }

    /**
     * Returns the player of the current operation. On the client thread outside of one it falls back to the
     * local player, so HUD queries (ammo icon, counter) resolve the same calibers the server does.
     */
    public static EntityPlayer getPlayer() {
        EntityPlayer player = CURRENT_PLAYER.get();
        if (player != null) {
            return player;
        }
        return FMLCommonHandler.instance().getSide().isClient() ? ClientPlayerAccess.localPlayer() : null;
    }

    /**
     * Returns the gun stack of the current operation, or the local player's held gun on the client thread.
     */
    public static ItemStack getStack() {
        ItemStack stack = CURRENT_STACK.get();
        if (stack != null || CURRENT_PLAYER.get() != null) {
            return stack;
        }
        if (!FMLCommonHandler.instance().getSide().isClient()) {
            return null;
        }
        EntityPlayer local = ClientPlayerAccess.localPlayer();
        return local == null ? null : local.getHeldItemMainhand();
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

    /**
     * Starts a gun operation, remembering the enclosing one so {@link #pop()} can put it back.
     */
    public static void push(EntityPlayer player, ItemStack stack) {
        ArrayDeque<Object[]> saved = SAVED.get();
        if (saved.size() > 16) {
            // a hook that threw never popped, don't let the leftovers pile up
            saved.clear();
        }
        saved.push(new Object[]{CURRENT_PLAYER.get(), CURRENT_STACK.get()});
        setContext(player, stack);
    }

    /**
     * Ends the operation started by the last {@link #push}, restoring the enclosing one if there was any.
     */
    public static void pop() {
        ArrayDeque<Object[]> saved = SAVED.get();
        if (saved.isEmpty()) {
            clear();
            return;
        }
        Object[] previous = saved.pop();
        setContext((EntityPlayer) previous[0], (ItemStack) previous[1]);
    }

    public static void clear() {
        CURRENT_PLAYER.remove();
        CURRENT_STACK.remove();
    }
}
