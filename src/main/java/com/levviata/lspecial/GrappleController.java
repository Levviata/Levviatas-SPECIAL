package com.levviata.lspecial;

import com.levviata.lspecial.entity.EntityGrappleChain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** server-authoritative grapple movement shared by both grapple directions. */
public final class GrappleController {
    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final class State {
        final int targetId;
        final boolean pullTarget;
        final double speed;
        State(int targetId, boolean pullTarget, double speed) { this.targetId = targetId; this.pullTarget = pullTarget; this.speed = speed; }
    }

    public static boolean start(EntityPlayerMP player, Entity target, boolean pullTarget, double speed) {
        if (target == null || target.isDead || target.world != player.world) return false;
        ACTIVE.put(player.getUniqueID(), new State(target.getEntityId(), pullTarget, speed));
        if (LSPECIALConfig.doubleBarrelGrappleVisuals && target instanceof EntityLivingBase) {
            EntityGrappleChain chain = new EntityGrappleChain(player.world);
            chain.setEndpoints(player, (EntityLivingBase) target, 200);
            GrappleChainSpawnHandler.queue(chain);
        }
        LSPECIALMod.NETWORK.sendTo(new GrappleCameraPacket(target.getEntityId(), true), player);
        player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_BOBBER_THROW, SoundCategory.PLAYERS, 0.9F, 1.15F);
        return true;
    }

    public static void stop(EntityPlayerMP player) {
        if (ACTIVE.remove(player.getUniqueID()) != null) {
            LSPECIALMod.NETWORK.sendTo(new GrappleCameraPacket(-1, false), player);
            player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.ENTITY_LEASHKNOT_BREAK, SoundCategory.PLAYERS, 0.8F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        State state = ACTIVE.get(player.getUniqueID());
        if (state == null) return;
        Entity target = player.world.getEntityByID(state.targetId);
        if (target == null || target.isDead || !target.isEntityAlive()) { stop(player); return; }
        Entity mover = state.pullTarget ? target : player;
        Entity anchor = state.pullTarget ? player : target;
        Vec3d from = mover.getPositionVector().add(0, mover.height * 0.5D, 0);
        Vec3d to = anchor.getPositionVector().add(0, anchor.height * 0.5D, 0);
        Vec3d delta = to.subtract(from);
        double distance = delta.length();
        if (distance < 1.25D) {
            if (state.pullTarget) { target.motionX = target.motionY = target.motionZ = 0; target.velocityChanged = true; }
            stop(player);
            return;
        }
        Vec3d velocity = delta.normalize().scale(Math.min(state.speed, Math.max(0.25D, distance * 0.32D)));
        mover.motionX = velocity.x;
        mover.motionY = velocity.y;
        mover.motionZ = velocity.z;
        mover.fallDistance = 0;
        mover.velocityChanged = true;
        if (mover instanceof EntityPlayerMP) {
            EntityPlayerMP movingPlayer = (EntityPlayerMP) mover;
            movingPlayer.motionX += movingPlayer.moveStrafing * 0.16D;
            movingPlayer.motionZ += movingPlayer.moveForward * 0.16D;
            movingPlayer.velocityChanged = true;
        }
        if (player.ticksExisted % 8 == 0) player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 0.25F, 1.1F);
    }
}
