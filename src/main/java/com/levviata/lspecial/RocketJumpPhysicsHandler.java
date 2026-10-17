package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Applies the Source-style low-gravity/momentum portion only during a tracked blast jump.
 * Minecraft's normal movement is untouched outside this short-lived marker.
 */
public final class RocketJumpPhysicsHandler {
    private static final String ACTIVE_TICKS = "LSPECIALRocketJumpTicks";
    private static final int MAX_TRACK_TICKS = 60;
    private static final double SOURCE_GRAVITY_COMPENSATION = 0.03D;
    private static final double MOMENTUM_RETENTION = 1.005D;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
                || !(event.player instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.player;
        NBTTagCompound data = player.getEntityData();
        int ticks = data.getInteger(ACTIVE_TICKS);
        if (ticks <= 0) return;

        if (player.onGround && player.motionY <= 0.0D) {
            data.removeTag(ACTIVE_TICKS);
            return;
        }

        // Vanilla applies roughly 0.08 blocks/tick^2 of gravity. Add back 0.03 while
        // blast-jumping to approximate Source's lighter airborne gravity after unit scaling.
        player.motionY += SOURCE_GRAVITY_COMPENSATION;
        player.motionX *= MOMENTUM_RETENTION;
        player.motionZ *= MOMENTUM_RETENTION;
        data.setInteger(ACTIVE_TICKS, ticks - 1);
    }

    public static void begin(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;
        player.getEntityData().setInteger(ACTIVE_TICKS, MAX_TRACK_TICKS);
    }
}
