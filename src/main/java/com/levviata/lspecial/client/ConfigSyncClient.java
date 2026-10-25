package com.levviata.lspecial.client;

import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Drops the server's config values when leaving a server, so the local config applies again.
 */
public class ConfigSyncClient {
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(LSPECIALConfig::clearServerOverrides);
    }
}
