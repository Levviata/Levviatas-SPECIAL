package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Holds the player associated with the currently executing NTM gun operation.
 *
 * ItemGunBaseNT creates LambdaContext with the actual player before invoking
 * the gun callbacks. The ItemStack-only Receiver/Magazine methods do not carry
 * that context in their method signature, so MixinItemGunBaseNT bridges the
 * same player into this thread-local while the gun operation is executing.
 */
public final class SPECIALPlayerContext {

    private static final ThreadLocal<EntityPlayer> CURRENT_PLAYER = new ThreadLocal<>();

    private SPECIALPlayerContext() {
    }

    public static EntityPlayer getPlayer() {
        return CURRENT_PLAYER.get();
    }

    public static void setPlayer(EntityPlayer player) {
        if (player == null) {
            CURRENT_PLAYER.remove();
        } else {
            CURRENT_PLAYER.set(player);
        }
    }

    public static void clear() {
        CURRENT_PLAYER.remove();
    }
}
