package com.levviata.lspecial.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class SyncSPECIALGunStatePacket implements IMessage {
    public ItemStack gun;
    public int[] indices;
    public float[] damages;

    public SyncSPECIALGunStatePacket() {
        gun = ItemStack.EMPTY;
        indices = new int[0];
        damages = new float[0];
    }

    public SyncSPECIALGunStatePacket(ItemStack gun, int[] indices, float[] damages) {
        this.gun = gun == null ? ItemStack.EMPTY : gun;
        this.indices = indices == null ? new int[0] : indices;
        this.damages = damages == null ? new float[0] : damages;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        gun = ByteBufUtils.readItemStack(buf);

        int size = buf.readInt();
        indices = new int[size];
        damages = new float[size];

        for (int i = 0; i < size; i++) {
            indices[i] = buf.readInt();
            damages[i] = buf.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, gun);

        int size = Math.min(indices.length, damages.length);
        buf.writeInt(size);

        for (int i = 0; i < size; i++) {
            buf.writeInt(indices[i]);
            buf.writeFloat(damages[i]);
        }
    }
}
