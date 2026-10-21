package com.levviata.lspecial.network;

import com.levviata.lspecial.client.ClientGunState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SyncSPECIALGunStateSetter implements IMessageHandler<SyncSPECIALGunStatePacket, IMessage> {
    @Override
    public IMessage onMessage(SyncSPECIALGunStatePacket message, MessageContext ctx) {
        com.levviata.lspecial.LSPECIALMod.info("GunDebug",
                "Client packet handler received gun state: gun={}, gunIndex={}, entries={}",
                message.gun == null || message.gun.isEmpty() ? "EMPTY" : message.gun.getDisplayName(),
                message.currentGunIndex,
                message.indices == null ? 0 : message.indices.length);

        Minecraft.getMinecraft().addScheduledTask(() -> {
            ClientGunState.set(message.gun, message.currentGunIndex, message.indices, message.damages);
            com.levviata.lspecial.LSPECIALMod.info("GunDebug",
                    "Client gun state applied: currentIndex={}, damage={}",
                    ClientGunState.getCurrentGunIndex(),
                    ClientGunState.getDamage(ClientGunState.getCurrentGunIndex()));
        });
        return null;
    }
}
