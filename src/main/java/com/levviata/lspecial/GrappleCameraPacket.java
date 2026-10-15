package com.levviata.lspecial;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;

public class GrappleCameraPacket implements IMessage {
    private int targetId;
    private boolean active;
    public GrappleCameraPacket() { }
    public GrappleCameraPacket(int targetId, boolean active) { this.targetId = targetId; this.active = active; }
    @Override public void toBytes(ByteBuf buf) { buf.writeInt(targetId); buf.writeBoolean(active); }
    @Override public void fromBytes(ByteBuf buf) { targetId = buf.readInt(); active = buf.readBoolean(); }
    public static class Handler implements IMessageHandler<GrappleCameraPacket, IMessage> {
        @Override public IMessage onMessage(GrappleCameraPacket message, MessageContext context) {
            if (context.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> GrappleClientHandler.setTarget(message.active ? message.targetId : -1));
            } else if (message.targetId == -2 && context.getServerHandler().player instanceof EntityPlayerMP) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    GrappleController.stop(player);
                    player.motionY = Math.max(player.motionY, 0.42D);
                    player.velocityChanged = true;
                });
            }
            return null;
        }
    }
}
