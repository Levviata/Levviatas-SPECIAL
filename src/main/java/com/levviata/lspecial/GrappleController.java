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
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** server-authoritative grapple movement shared by both grapple directions. */
public final class GrappleController {
    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();
    private static final class State {
        final int targetId;
        final boolean pullTarget;
        final double speed;
        EntityGrappleChain chain;
        State(int targetId, boolean pullTarget, double speed) { this.targetId = targetId; this.pullTarget = pullTarget; this.speed = speed; }
    }

    public GrappleController() { }

    public static boolean isActive(EntityPlayerMP player) { return ACTIVE.containsKey(player.getUniqueID()); }
    public static boolean isOnCooldown(EntityPlayerMP player) { Long until = COOLDOWN_UNTIL.get(player.getUniqueID()); return until != null && player.world.getTotalWorldTime() < until; }
    public static boolean isPullingTarget(EntityPlayerMP player) { State state = ACTIVE.get(player.getUniqueID()); return state != null && state.pullTarget; }

    public static boolean start(EntityPlayerMP player, Entity target, boolean pullTarget, double speed) {
        if (target == null || target.isDead || target.world != player.world || target == player || isActive(player) || isOnCooldown(player)) return false;
        State state = new State(target.getEntityId(), pullTarget, speed);
        ACTIVE.put(player.getUniqueID(), state);
        COOLDOWN_UNTIL.put(player.getUniqueID(), player.world.getTotalWorldTime() + 60L);
        if (LSPECIALConfig.doubleBarrelGrappleVisuals && target instanceof EntityLivingBase) {
            EntityGrappleChain chain = new EntityGrappleChain(player.world);
            chain.setEndpoints(player, (EntityLivingBase) target, 1200);
            state.chain = chain;
            GrappleChainSpawnHandler.queue(chain);
        }
        LSPECIALMod.NETWORK.sendTo(new GrappleCameraPacket(target.getEntityId(), true), player);
        player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.ENTITY_BOBBER_THROW, SoundCategory.PLAYERS, 0.9F, 1.15F);
        return true;
    }

    public static void stop(EntityPlayerMP player) {
        State state = ACTIVE.remove(player.getUniqueID());
        if (state != null) {
            if (state.chain != null) state.chain.setDead();
            LSPECIALMod.NETWORK.sendTo(new GrappleCameraPacket(-1, false), player);
            player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.ENTITY_LEASHKNOT_BREAK, SoundCategory.PLAYERS, 0.8F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.getEntityData().getBoolean("LSPECIALGrappleFallSafe")) {
            player.fallDistance = 0.0F;
            if (player.onGround) player.getEntityData().setBoolean("LSPECIALGrappleFallSafe", false);
        }
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
        double acceleration = LSPECIALConfig.doubleBarrelGrappleAcceleration;
        Vec3d direction = delta.normalize();
        Vec3d velocity = direction.scale(Math.min(state.speed, Math.max(0.25D, distance * acceleration)));
        mover.motionX += (velocity.x - mover.motionX) * acceleration;
        mover.motionY += (velocity.y - mover.motionY) * acceleration;
        mover.motionZ += (velocity.z - mover.motionZ) * acceleration;
        mover.fallDistance = 0;
        mover.velocityChanged = true;
        if (mover instanceof EntityPlayerMP) {
            EntityPlayerMP movingPlayer = (EntityPlayerMP) mover;
            float yaw = movingPlayer.rotationYaw * 0.017453292F;
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double rightX = Math.cos(yaw);
            double rightZ = Math.sin(yaw);
            double strafe = movingPlayer.moveStrafing * LSPECIALConfig.doubleBarrelGrappleAirStrafe;
            double forward = movingPlayer.moveForward * LSPECIALConfig.doubleBarrelGrappleAirStrafe;
            movingPlayer.motionX += rightX * strafe + forwardX * forward;
            movingPlayer.motionZ += rightZ * strafe + forwardZ * forward;
            movingPlayer.velocityChanged = true;
        }
        if (!state.pullTarget) player.fallDistance = 0.0F;
        if (player.ticksExisted % 8 == 0) player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 0.25F, 1.1F);
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
            if (player.getEntityData().getBoolean("LSPECIALGrappleFallSafe")) {
                event.setCanceled(true);
                player.fallDistance = 0.0F;
                if (player.onGround) player.getEntityData().setBoolean("LSPECIALGrappleFallSafe", false);
            }
        }
    }
}
