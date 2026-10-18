package com.levviata.lspecial;

import com.levviata.lspecial.entity.EntityGrappleChain;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative grapple movement shared by both grapple directions.
 */
public final class GrappleController {
    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<UUID, PendingKill> PENDING_KILLS = new HashMap<>();

    private static final class PendingKill {
        final Entity target;
        final boolean pullTarget;
        final long createdAt;
        long groundGraceUntil;

        PendingKill(Entity target, boolean pullTarget, long createdAt) {
            this.target = target;
            this.pullTarget = pullTarget;
            this.createdAt = createdAt;
            this.groundGraceUntil = -1L;
        }
    }

    private static final class State {
        final int targetId;
        final Entity targetEntity;
        final boolean pullTarget;
        final double speed;
        EntityGrappleChain chain;
        long targetDeathGraceUntil;
        boolean killRewardGranted;

        State(Entity target, boolean pullTarget, double speed) {
            this.targetId = target.getEntityId();
            this.targetEntity = target;
            this.pullTarget = pullTarget;
            this.speed = speed;
        }
    }

    public GrappleController() {
    }

    public static boolean isActive(EntityPlayerMP player) {
        return ACTIVE.containsKey(player.getUniqueID());
    }

    public static boolean isOnCooldown(EntityPlayerMP player) {
        Long until = COOLDOWN_UNTIL.get(player.getUniqueID());
        return until != null && player.world.getTotalWorldTime() < until;
    }

    public static boolean isPullingTarget(EntityPlayerMP player) {
        State state = ACTIVE.get(player.getUniqueID());
        return state != null && state.pullTarget;
    }

    public static boolean start(EntityPlayerMP player, Entity target, boolean pullTarget, double speed) {
        if (target == null || target.isDead || target.world != player.world || target == player || isActive(player) || isOnCooldown(player)) {
            return false;
        }

        State state = new State(target, pullTarget, speed);
        ACTIVE.put(player.getUniqueID(), state);
        COOLDOWN_UNTIL.put(player.getUniqueID(), player.world.getTotalWorldTime() + 60L);
        if (LSPECIALConfig.doubleBarrelGrappleVisuals && target instanceof EntityLivingBase) {
            EntityGrappleChain chain = new EntityGrappleChain(player.world);
            chain.setEndpoints(player, (EntityLivingBase) target, 1200);
            state.chain = chain;
            GrappleChainSpawnHandler.queue(chain);
        }
        LSPECIALMod.NETWORK.sendTo(new GrappleCameraPacket(target.getEntityId(), true), player);
        return true;
    }

    public static void stop(EntityPlayerMP player) {
        State state = ACTIVE.remove(player.getUniqueID());
        if (state != null) {
            if (!state.pullTarget && state.targetEntity != null && !state.targetEntity.isDead) {
                PENDING_KILLS.put(player.getUniqueID(),
                        new PendingKill(state.targetEntity, false, player.world.getTotalWorldTime()));
            }
            if (state.chain != null) state.chain.setDead();
            LSPECIALMod.NETWORK.sendTo(new GrappleCameraPacket(-1, false), player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !(event.player instanceof EntityPlayerMP player)) {
            return;
        }
        if (player.getEntityData().getBoolean("LSPECIALGrappleFallSafe")) {
            player.fallDistance = 0.0F;
            if (player.onGround) player.getEntityData().setBoolean("LSPECIALGrappleFallSafe", false);
        }
        UUID playerId = player.getUniqueID();
        long worldTime = player.world.getTotalWorldTime();
        PendingKill pending = PENDING_KILLS.get(playerId);
        if (pending != null) {
            if (pending.target == null || pending.target.isDead && pending.target.world != player.world
                    || worldTime - pending.createdAt > 100L) {
                PENDING_KILLS.remove(playerId);
                pending = null;
            } else if (player.onGround) {
                if (pending.groundGraceUntil < 0L) pending.groundGraceUntil = worldTime + 14L;
                else if (worldTime > pending.groundGraceUntil) {
                    PENDING_KILLS.remove(playerId);
                    pending = null;
                }
            }
        }

        State state = ACTIVE.get(playerId);
        if (state == null) return;
        Entity target = state.targetEntity;
        if (target == null || target.isDead || !target.isEntityAlive()) {
            if (!state.pullTarget && !state.killRewardGranted && target != null
                    && (target.isDead || !target.isEntityAlive())) {
                state.killRewardGranted = true;
                grantKillRewards(player, target);
            }
            if (state.targetDeathGraceUntil == 0L) {
                state.targetDeathGraceUntil = worldTime + 20L;
            }
            if (worldTime >= state.targetDeathGraceUntil) stop(player);
            return;
        }
        Entity mover = state.pullTarget ? target : player;
        Entity anchor = state.pullTarget ? player : target;
        Vec3d from = mover.getPositionVector().add(0, mover.height * 0.5D, 0);
        Vec3d to = anchor.getPositionVector().add(0, anchor.height * 0.5D, 0);
        Vec3d delta = to.subtract(from);
        double distance = delta.length();
        if (distance < 1.25D) {
            if (state.pullTarget) {
                target.motionX = target.motionY = target.motionZ = 0;
                target.velocityChanged = true;
            }
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
        if (mover instanceof EntityPlayerMP movingPlayer) {
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
    }

    @SubscribeEvent
    public void onGrappledEntityDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        Entity deadTarget = event.getEntityLiving();
        for (Map.Entry<UUID, State> entry : ACTIVE.entrySet()) {
            State state = entry.getValue();
            if (state.pullTarget || state.killRewardGranted || state.targetEntity != deadTarget) continue;
            EntityPlayerMP player = deadTarget.world.getMinecraftServer() == null ? null
                    : deadTarget.world.getMinecraftServer().getPlayerList().getPlayerByUUID(entry.getKey());
            if (player == null) continue;

            state.killRewardGranted = true;
            state.targetDeathGraceUntil = deadTarget.world.getTotalWorldTime() + 20L;
            grantKillRewards(player, deadTarget);
            return;
        }

        for (Map.Entry<UUID, PendingKill> entry : PENDING_KILLS.entrySet()) {
            PendingKill pending = entry.getValue();
            if (pending.pullTarget || pending.target != deadTarget) continue;
            EntityPlayerMP player = deadTarget.world.getMinecraftServer() == null ? null
                    : deadTarget.world.getMinecraftServer().getPlayerList().getPlayerByUUID(entry.getKey());
            if (player == null) continue;
            grantKillRewards(player, deadTarget);
            PENDING_KILLS.remove(entry.getKey());
            return;
        }
    }

    private static void grantKillRewards(EntityPlayerMP player, Entity target) {
        COOLDOWN_UNTIL.remove(player.getUniqueID());
        LSPECIALMod.NETWORK.sendTo(new GrappleCameraPacket(-3, false), player);
        lspecial$refillDoubleBarrel(player);
    }

    /**
     * Refills the held double-barrel magazine after killing an entity while pulling the player toward it.
     */
    private static void lspecial$refillDoubleBarrel(EntityPlayerMP player) {
        ItemStack stack = player.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT gun)
                || !"hbm:gun_double_barrel".equals(stack.getItem().getRegistryName().toString())) return;
        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            GunConfig config = gun.getConfig(stack, configIndex);
            if (config == null) continue;
            Receiver[] receivers = config.getReceivers(stack);
            if (receivers == null) continue;
            for (Receiver receiver : receivers) {
                if (receiver == null) continue;
                IMagazine magazine = receiver.getMagazine(stack);
                if (magazine != null) {
                    magazine.setAmount(stack, magazine.getCapacity(stack));
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP player) {
            if (player.getEntityData().getBoolean("LSPECIALGrappleFallSafe")) {
                event.setCanceled(true);
                player.fallDistance = 0.0F;
                if (player.onGround) player.getEntityData().setBoolean("LSPECIALGrappleFallSafe", false);
            }
        }
    }
}
