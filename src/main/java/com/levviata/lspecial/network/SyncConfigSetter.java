package com.levviata.lspecial.network;

import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SyncConfigSetter implements IMessageHandler<SyncConfigPacket, IMessage> {
    @Override
    public IMessage onMessage(SyncConfigPacket message, MessageContext ctx) {
        Minecraft minecraft = Minecraft.getMinecraft();

        minecraft.addScheduledTask(() -> {
            // the integrated server shares this JVM and this config, there is nothing to override
            if (minecraft.isIntegratedServerRunning()) {
                return;
            }
            LSPECIALConfig.applyServerOverrides(message.values);
        });
        return null;
    }
}
