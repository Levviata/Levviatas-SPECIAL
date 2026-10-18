package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
        EntityPlayer player = event.player;
        NBTTagCompound data = player.getEntityData();
        int ticks = data.getInteger(ACTIVE_TICKS);
        if (data.getBoolean("LSPECIALRocketJumping")) {
            player.fallDistance = 0.0F;
            if (player.onGround && player.motionY <= 0.0D) {
                data.removeTag("LSPECIALRocketJumping");
                data.removeTag(ACTIVE_TICKS);
                return;
            }
        }
        if (ticks <= 0) return;

        if (player.onGround && player.motionY <= 0.0D) {
            data.removeTag(ACTIVE_TICKS);
            return;
        }

        // vanilla applies roughly 0.08 blocks/tick^2 of gravity; compensate only during the tracked jump.
        player.motionY += SOURCE_GRAVITY_COMPENSATION;
        player.motionX *= MOMENTUM_RETENTION;
        player.motionZ *= MOMENTUM_RETENTION;
        data.setInteger(ACTIVE_TICKS, ticks - 1);
    }

    @SubscribeEvent
    public void onRocketJumpFallDamage(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player) || !"fall".equals(event.getSource().damageType)) return;
        if (player.getEntityData().getBoolean("LSPECIALRocketJumping")) {
            player.fallDistance = 0.0F;
            event.setCanceled(true);
        }
    }

    public static void begin(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;
        player.getEntityData().setInteger(ACTIVE_TICKS, MAX_TRACK_TICKS);
        player.getEntityData().setBoolean("LSPECIALRocketJumping", true);
    }
}
