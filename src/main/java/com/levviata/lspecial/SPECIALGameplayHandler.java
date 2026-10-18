package com.levviata.lspecial;

import com.hbm.items.armor.ArmorFSBPowered;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.render.anim.sedna.AnimationEnums.GunAnimation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

/** server-side SPECIAL effects that need Forge events rather than per-frame GUI code. */
public final class SPECIALGameplayHandler {
    private static final Map<EntityPlayer, LockedGun> rapidFireLocks = new WeakHashMap<>();
    private static final Map<EntityPlayer, PendingSelfKill> pendingSelfKills = new WeakHashMap<>();
    private static final Map<EntityPlayer, List<PendingProjectile>> pendingProjectileBursts = new WeakHashMap<>();

    private static final class PendingProjectile {
        private final BulletConfig config;
        private final float yaw;
        private final float pitch;
        private final double x, y, z, motionX, motionY, motionZ, velocity, previousVelocity, acceleration;
        private final float damage;
        private final int ricochets;
        private final Entity lockonTarget;
        private final NBTTagCompound entityData;
        private int ticksRemaining;

        private PendingProjectile(EntityBulletBaseMK4 bullet, int delay) {
            config = bullet.getBulletConfig();
            yaw = bullet.rotationYaw;
            pitch = bullet.rotationPitch;
            x = bullet.posX; y = bullet.posY; z = bullet.posZ;
            motionX = bullet.motionX; motionY = bullet.motionY; motionZ = bullet.motionZ;
            velocity = bullet.velocity; previousVelocity = bullet.prevVelocity; acceleration = bullet.accel;
            damage = bullet.damage; ricochets = bullet.ricochets; lockonTarget = bullet.lockonTarget;
            entityData = bullet.getEntityData().copy();
            ticksRemaining = delay;
        }

        private EntityBulletBaseMK4 spawn(EntityPlayer shooter) {
            double spawnX = shooter.posX;
            double spawnY = shooter.posY;
            double spawnZ = shooter.posZ;
            EntityBulletBaseMK4 clone = new EntityBulletBaseMK4(shooter.world, shooter, config,
                    shooter.rotationYaw, shooter.rotationPitch, spawnX, spawnY, spawnZ, motionX, motionY, motionZ);
            clone.setPosition(spawnX, spawnY, spawnZ);
            clone.motionX = motionX; clone.motionY = motionY; clone.motionZ = motionZ;
            clone.damage = damage; clone.velocity = velocity; clone.prevVelocity = previousVelocity;
            clone.accel = acceleration; clone.ricochets = 0; clone.lockonTarget = lockonTarget;
            clone.getEntityData().merge(entityData.copy());
            clone.getEntityData().setBoolean("LSPECIALBurstClone", true);
            return clone;
        }
    }

    private static final class PendingSelfKill {
        private final ItemStack stack;
        private int ticksRemaining;
        private boolean firedCue;

        private PendingSelfKill(ItemStack stack, int ticksRemaining) {
            this.stack = stack;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final class LockedGun {
        private final ItemStack stack;
        private final int slot;
        private int ticksRemaining;
        private boolean firedCue;

        private LockedGun(ItemStack stack, int slot, int ticksRemaining) {
            this.stack = stack;
            this.slot = slot;
            this.ticksRemaining = ticksRemaining;
        }
    }

    /** keeps the selected light revolver locked while its triggered rapid-fire burst is active. */
    @SubscribeEvent
    public void onRapidFirePlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        LockedGun locked = rapidFireLocks.get(player);
        if (locked == null) return;

        if (!lspecial$canUseLightRevolverRapidFire(player)) {
            lspecial$releaseRapidFire(player, locked);
            return;
        }

        int actualSlot = lspecial$findHotbarStack(player, locked.stack);
        if (actualSlot < 0 || locked.stack.isEmpty() || --locked.ticksRemaining <= 0) {
            lspecial$releaseRapidFire(player, locked);
            return;
        }

        if (player.inventory.currentItem != actualSlot) {
            player.inventory.currentItem = actualSlot;
        }
        lspecial$setRapidFire(locked.stack, true);
    }

    /** starts a native rapid-fire burst and locks selection to the exact stack that triggered it. */
    public static boolean startLightRevolverRapidFire(EntityPlayer player, ItemStack stack) {
        if (!lspecial$canUseLightRevolverRapidFire(player) || !lspecial$isLightRevolver(stack)) return false;
        int slot = lspecial$findHotbarStack(player, stack);
        if (slot < 0) {
            ItemStack held = player.getHeldItemMainhand();
            if (!held.isEmpty() && ItemStack.areItemsEqual(held, stack)) {
                slot = player.inventory.currentItem;
            }
        }
        if (slot < 0) return false;

        // use the actual inventory object: keybind callbacks can supply an equivalent stack copy.
        ItemStack actualStack = player.inventory.getStackInSlot(slot);
        if (actualStack.isEmpty() || !lspecial$isLightRevolver(actualStack)) return false;
        LockedGun previous = rapidFireLocks.put(player,
                new LockedGun(actualStack, slot, LSPECIALConfig.lightRevolverRapidFireTicks));
        if (previous != null && previous.stack != actualStack) lspecial$setRapidFire(previous.stack, false);
        lspecial$setRapidFire(actualStack, true);
        return true;
    }

    /** reports whether this exact stack currently owns the player's rapid-fire burst. */
    public static boolean isLightRevolverRapidFireLocked(EntityPlayer player, ItemStack stack) {
        LockedGun locked = rapidFireLocks.get(player);
        return locked != null && locked.stack == stack && lspecial$canUseLightRevolverRapidFire(player);
    }

    /** releases the lock and clears native primary input so firing cannot continue after the burst. */
    private static void lspecial$releaseRapidFire(EntityPlayer player, LockedGun locked) {
        rapidFireLocks.remove(player);
        lspecial$setRapidFire(locked.stack, false);
    }

    /** checks the configured perk and its Intelligence threshold before forcing fire input. */
    private static boolean lspecial$canUseLightRevolverRapidFire(EntityPlayer player) {
        return player != null && !player.isDead && LSPECIALConfig.modifyGuns
                && LSPECIALConfig.intelligence && LSPECIALConfig.pepperboxPerk
                && LSPECIALConfig.lightRevolverRapidFirePerk && inst != null
                && inst.getPlayerScore(player, inst.INTELLIGENCE_BOARD) != null
                && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= 5;
    }

    /** identifies the NTM light revolver without relying on its translated name. */
    private static boolean lspecial$isLightRevolver(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getRegistryName() != null
                && "hbm:gun_light_revolver".equals(stack.getItem().getRegistryName().toString());
    }

    /** finds the exact stack identity in the hotbar so a replacement stack cannot inherit the lock. */
    private static int lspecial$findHotbarStack(EntityPlayer player, ItemStack target) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.inventory.getStackInSlot(slot) == target) return slot;
        }
        return -1;
    }

    /** sets the native primary-fire state for each NTM configuration on the locked stack. */
    private static void lspecial$setRapidFire(ItemStack stack, boolean firing) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        for (int config = 0; config < gun.getConfigCount(); config++) {
            ItemGunBaseNT.setPrimary(stack, config, firing);
        }
    }

    /** Snapshots native Sedna projectiles and queues two matching follow-up shots 0.4 seconds apart. */
    @SubscribeEvent
    public void onBurstProjectileSpawn(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityBulletBaseMK4)) return;
        EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) event.getEntity();
        if (bullet.getEntityData().getBoolean("LSPECIALBurstClone") || bullet.getBulletConfig() == null) return;
        EntityLivingBase owner = bullet.getThrower();
        if (!(owner instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) owner;
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || held.getItem().getRegistryName() == null || !held.hasTagCompound()
                || !held.getTagCompound().getBoolean("LSPECIALSecondaryMode")) return;
        String id = held.getItem().getRegistryName().toString();
        int requiredLevel;
        boolean enabled;
        if ("hbm:gun_lag".equals(id)) {
            requiredLevel = 5;
            enabled = LSPECIALConfig.lagSecondaryBurst;
        } else if ("hbm:gun_henry_lincoln".equals(id)) {
            requiredLevel = 15;
            enabled = LSPECIALConfig.henryLincolnSecondaryBurst;
        } else return;
        if (!enabled || !LSPECIALConfig.modifyGuns || !LSPECIALConfig.intelligence || inst == null
                || inst.getPlayerScore(player, inst.INTELLIGENCE_BOARD) == null
                || inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) < requiredLevel) return;

        List<PendingProjectile> pending = pendingProjectileBursts.computeIfAbsent(player, k -> new ArrayList<>());
        pending.add(new PendingProjectile(bullet, 5));
        pending.add(new PendingProjectile(bullet, 10));
    }

    private static void lspecial$tickPendingProjectiles(EntityPlayer player) {
        List<PendingProjectile> pending = pendingProjectileBursts.get(player);
        if (pending == null || pending.isEmpty()) return;
        Iterator<PendingProjectile> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingProjectile shot = iterator.next();
            if (--shot.ticksRemaining > 0) continue;
            if (!player.isDead && player.world != null && !player.world.isRemote) {
                player.world.spawnEntity(shot.spawn(player));
            }
            iterator.remove();
        }
        if (pending.isEmpty()) pendingProjectileBursts.remove(player);
    }

    /** starts the Lag self-kill countdown and plays its inspect animation. */
    public static boolean startLagSelfKill(EntityPlayer player, ItemStack stack) {
        if (player == null || player.world.isRemote || player.isDead || stack == null || stack.isEmpty()
                || player.getHeldItemMainhand() != stack
                || !LSPECIALConfig.modifyGuns || !LSPECIALConfig.lagSecondarySelfKill
                || !lspecial$isLag(stack) || !lspecial$hasLagSelfKillPerk(player)) return false;

        pendingSelfKills.put(player, new PendingSelfKill(stack, 20));
        if (stack.getItem() instanceof ItemGunBaseNT) {
            ItemGunBaseNT.playAnimation(player, stack, GunAnimation.INSPECT, 0);
        }
        return true;
    }

    /** checks the Lag's Intelligence requirement using the same lesser-perk setting as its other actions. */
    private static boolean lspecial$hasLagSelfKillPerk(EntityPlayer player) {
        return LSPECIALConfig.intelligence && LSPECIALConfig.pepperboxPerk && inst != null
                && inst.getPlayerScore(player, inst.INTELLIGENCE_BOARD) != null
                && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= 5;
    }

    /** identifies the Lag by registry id rather than its translated item name. */
    private static boolean lspecial$isLag(ItemStack stack) {
        return stack.getItem().getRegistryName() != null
                && "hbm:gun_lag".equals(stack.getItem().getRegistryName().toString());
    }

    /** plays the Lag firing cue on tick 19 and applies the self-kill on tick 20. */
    private void lspecial$tickPendingSelfKill(EntityPlayer player) {
        PendingSelfKill pending = pendingSelfKills.get(player);
        if (pending == null) return;
        if (player.isDead || player.world == null || player.world.isRemote
                || player.getHeldItemMainhand() != pending.stack) {
            pendingSelfKills.remove(player);
            return;
        }
        if (--pending.ticksRemaining == 1 && !pending.firedCue) {
            pending.firedCue = true;
            lspecial$playLagFireCue(player, pending.stack);
        }
        if (pending.ticksRemaining <= 0) {
            pendingSelfKills.remove(player);
            player.onKillCommand();
        }
    }

    /** plays the gun's native fire sound and consumes one loaded round for the final cue. */
    private static void lspecial$playLagFireCue(EntityPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        GunConfig config = gun.getConfig(stack, 0);
        if (config == null) return;
        Receiver[] receivers = config.getReceivers(stack);
        if (receivers == null) return;

        SoundEvent sound = null;
        for (Receiver receiver : receivers) {
            if (receiver == null) continue;
            if (sound == null) sound = receiver.getFireSound(stack);
            IMagazine magazine = receiver.getMagazine(stack);
            if (magazine == null) continue;
            int amount = magazine.getAmount(stack, null);
            if (amount > 0) {
                magazine.setAmount(stack, amount - 1);
                break;
            }
        }
        if (sound != null) {
            player.world.playSound(null, player.getPosition(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        lspecial$tickPendingSelfKill(player);
        lspecial$tickPendingProjectiles(player);
        lspecial$tickPoweredArmorRecovery(player);

    }

    /** restores equipped NTM power armor after each five-minute interval when its charge is nearly depleted. */
    private static void lspecial$tickPoweredArmorRecovery(EntityPlayer player) {
        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorFailsafe
                || player.ticksExisted % 6000 != 0) return;

        int luck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);
        if (luck < LSPECIALConfig.luckArmorFailsafeLevel) return;

        for (ItemStack stack : player.inventory.armorInventory) {
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorFSBPowered)) continue;

            ArmorFSBPowered poweredArmor = (ArmorFSBPowered) stack.getItem();
            long maxCharge = poweredArmor.getMaxCharge(stack);
            if (maxCharge <= 0L) continue;

            long charge = poweredArmor.getCharge(stack);
            if (charge <= maxCharge * 0.01D) {
                long restoredCharge = maxCharge / 2L;
                poweredArmor.setCharge(stack, restoredCharge);
                LSPECIALMod.info("[LuckArmor] POWER_ARMOR_RECHARGED item={} player={} luck={} chargeBefore={} maxCharge={} chargeAfter={}",
                        stack.getItem().getRegistryName(), player.getName(), luck, charge, maxCharge, restoredCharge);
            }
        }
    }
}
