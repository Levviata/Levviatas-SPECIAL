package com.levviata.lspecial.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carries the server's gameplay config values ("category/key" -> value) to a connecting client.
 */
public class SyncConfigPacket implements IMessage {
    // sanity limit, the real snapshot has a few hundred entries
    private static final int MAX_ENTRIES = 4096;

    public Map<String, String> values = new LinkedHashMap<>();

    public SyncConfigPacket() {
    }

    public SyncConfigPacket(Map<String, String> values) {
        this.values = values;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(values.size());

        for (Map.Entry<String, String> entry : values.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey());
            ByteBufUtils.writeUTF8String(buf, entry.getValue());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = Math.min(buf.readInt(), MAX_ENTRIES);
        values = new LinkedHashMap<>();

        for (int i = 0; i < count; i++) {
            String key = ByteBufUtils.readUTF8String(buf);
            values.put(key, ByteBufUtils.readUTF8String(buf));
        }
    }
}
