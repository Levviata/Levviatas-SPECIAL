package com.levviata.lspecial.network;

import com.levviata.lspecial.client.ClientGunState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SyncSPECIALGunStateSetter implements IMessageHandler<SyncSPECIALGunStatePacket, IMessage> {
    @Override
    public IMessage onMessage(SyncSPECIALGunStatePacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() ->
                ClientGunState.set(message.gun, message.indices, message.damages));
        return null;
    }
}
