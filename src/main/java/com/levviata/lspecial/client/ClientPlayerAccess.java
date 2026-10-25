package com.levviata.lspecial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-only access to the local player, kept out of common classes so a dedicated server never loads Minecraft's client.
 */
@SideOnly(Side.CLIENT)
public final class ClientPlayerAccess {

    private ClientPlayerAccess() {
    }

    /**
     * Returns the local player when called from the client thread, null from the integrated server thread.
     */
    public static EntityPlayer localPlayer() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || !minecraft.isCallingFromMinecraftThread()) {
            return null;
        }
        return minecraft.player;
    }
}
