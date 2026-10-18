package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.HbmLivingCapability;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.effect.EntityFireLingering;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

// todo
/** TF2-style secondary airblast for NTM's standard Sedna flamethrower. */
public final class TF2FlamethrowerHandler {
    private static final int AIRBLAST_AMMO_COST = 20;
    private static final double AIRBLAST_RANGE = 5.0D;
    private static final double AIRBLAST_CONE_DOT = 0.45D;

    /** The only knob to turn for flame speed - everything else below derives from it, see {@link #ticksToReachRange}. */
    private static final float LSPECIAL_FLAME_VELOCITY = 0.8F;
    /** Hard flame range in blocks - must match the "8.0D" literals in MixinTF2FlameProjectile. */
    public static final double LSPECIAL_FLAME_RANGE_BLOCKS = 8.0D;
    /** Extra ticks of headroom tacked onto the computed range-time before it's used as the expires floor. */
    private static final int LSPECIAL_FLAME_EXPIRES_MARGIN = 20;

    /**
     * Ticks it takes a flame to decelerate from 100% to 10% speed across {@link #LSPECIAL_FLAME_RANGE_BLOCKS},
     * i.e. how long it takes to (almost) reach the hard range cutoff in MixinTF2FlameProjectile. Solving
     * dx/dt = v*(1 - 0.9*x/range) for x(t)=range gives t = range*ln(10)/(0.9*v); this is a continuous
     * approximation but lines up with the real discrete tick simulation to within ~1%.
     * <p>
     * TF2FlameRenderer uses this (with the live {@code BulletConfig#velocity}) to size its 40%->120% growth
     * curve, so it always finishes growing right around when the flame reaches 8 blocks - whatever velocity
     * is actually configured. That means {@link #LSPECIAL_FLAME_VELOCITY} is a single-number knob: change it
     * here and the growth curve, the speed curve (already distance-based, not velocity-based) and the
     * expires floor below all re-sync automatically, nothing else needs touching.
     */
    public static int ticksToReachRange(float velocity) {
        if (velocity <= 0.0001F) return 100; // guard against a misconfigured/zero velocity
        return (int) Math.ceil(LSPECIAL_FLAME_RANGE_BLOCKS * Math.log(10.0D) / (0.9D * velocity));
    }

    /**
     * Re-tunes the four standard flamer BulletConfigs so the TF2-style rework (no gravity, distance-based
     * speed/size/color curve capped at 8 blocks) has room to actually play out.
     * <p>
     * Native NTM speeds (velocity=1F) cover 8 blocks in roughly a second, which finishes the flight long
     * before a flame could ever visibly grow, so it'd always disappear tiny. Slowing the base velocity down
     * via {@code LSPECIAL_FLAME_VELOCITY} fixes that - see {@link #ticksToReachRange} for how
     * {@link com.levviata.lspecial.client.render.TF2FlameRenderer} derives its growth-curve length from
     * this same velocity, so the two always stay in sync no matter what it's set to.
     * <p>
     * Native per-config lifetimes also differ wildly (flame_gas expires after just 10 ticks), which would
     * kill that ammo type's flame almost immediately regardless of the above; bumping every standard config's
     * expires to {@code ticksToReachRange(LSPECIAL_FLAME_VELOCITY) + LSPECIAL_FLAME_EXPIRES_MARGIN} makes the
     * 8-block distance check (and not NTM's built-in timer) the thing that actually ends the flame's life.
     * <p>
     * Must run after {@code XFactoryFlamer.init()} (i.e. after all mods' init phase), so this is called from
     * {@link com.levviata.lspecial.LSPECIALMod#postInit}. Dragon's Fury (gun_flamer_topaz) uses its own
     * flame_topaz_* configs and is untouched here.
     */
    public static void initFlameTuning() {
        lspecial$tuneFlame(XFactoryFlamer.flame_diesel);
        lspecial$tuneFlame(XFactoryFlamer.flame_gas);
        lspecial$tuneFlame(XFactoryFlamer.flame_napalm);
        lspecial$tuneFlame(XFactoryFlamer.flame_balefire);
        lspecial$tuneFlame(XFactoryFlamer.flame_nograv);
    }

    private static void lspecial$tuneFlame(BulletConfig config) {
        if (config == null) return;
        config.setVel(LSPECIAL_FLAME_VELOCITY);
        int neededTicks = ticksToReachRange(LSPECIAL_FLAME_VELOCITY) + LSPECIAL_FLAME_EXPIRES_MARGIN;
        config.setLife(Math.max(config.expires, neededTicks));
    }

    /** Called on the server from the Sedna secondary-key hook. */
    public static boolean airblast(EntityPlayer player, ItemStack weapon) {
        if (player == null || weapon == null || weapon.isEmpty() || player.world.isRemote || player.isDead) {
            return false;
        }
        String itemId = weapon.getItem().getRegistryName() == null ? "" : weapon.getItem().getRegistryName().toString();
        boolean dragonsFury = "hbm:gun_flamer_topaz".equals(itemId);
        int ammoCost = dragonsFury ? 50 : AIRBLAST_AMMO_COST;
        if (!consumeAmmo(weapon, player, ammoCost)) return false;
        if (dragonsFury) {
            net.minecraft.nbt.NBTTagCompound tag = weapon.getTagCompound();
            if (tag == null) {
                tag = new net.minecraft.nbt.NBTTagCompound();
                weapon.setTagCompound(tag);
            }
            tag.setInteger("LSPECIALDragonsFuryPressureLock", 32);
            tag.setBoolean("LSPECIALDragonsFuryNextShotFast", false);
        }

        Vec3d look = player.getLookVec().normalize();
        Vec3d origin = player.getPositionEyes(1.0F);
        AxisAlignedBB search = player.getEntityBoundingBox().grow(AIRBLAST_RANGE + 1.0D);
        for (Entity target : player.world.getEntitiesWithinAABB(Entity.class, search,
                entity -> entity != player && !entity.isDead)) {
            Vec3d center = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);
            Vec3d delta = center.subtract(origin);
            double distance = delta.length();
            if (distance > AIRBLAST_RANGE || distance < 0.001D) continue;
            if (look.dotProduct(delta.scale(1.0D / distance)) < AIRBLAST_CONE_DOT) continue;

            if (target instanceof EntityFireLingering) {
                target.setDead();
                continue;
            }

            if (target instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) target;
                boolean wasBurning = living.isBurning();
                living.extinguish();
                HbmLivingCapability.IEntityHbmProps fireProps = HbmLivingProps.getData(living);
                if (fireProps != null) {
                    fireProps.setFire(0);
                    fireProps.setBalefire(0);
                    fireProps.setPhosphorus(0);
                }
                if (wasBurning && player.isOnSameTeam(living)) player.heal(20.0F);
                living.motionX += look.x * 1.25D;
                living.motionY += Math.max(0.25D, look.y * 1.25D + 0.15D);
                living.motionZ += look.z * 1.25D;
                living.velocityChanged = true;
                continue;
            }

            // Deflect NTM and other projectile entities; ignore dropped items and ordinary entities.
            String className = target.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            if (className.contains("projectile") || className.contains("bullet")
                    || className.contains("rocket") || className.contains("grenade")
                    || className.contains("fireball") || className.contains("throwable")) {
                double speed = Math.max(1.2D, target.getPositionVector().subtract(player.getPositionVector()).length() > 0
                        ? Math.sqrt(target.motionX * target.motionX + target.motionY * target.motionY + target.motionZ * target.motionZ)
                        : 1.2D);
                target.motionX = look.x * speed;
                target.motionY = look.y * speed;
                target.motionZ = look.z * speed;
                target.velocityChanged = true;
                target.getEntityData().setUniqueId("LSPECIALAirblastOwner", player.getUniqueID());
                target.getEntityData().setBoolean("LSPECIALAirblastMiniCrit", true);
                lspecial$redirectProjectileOwner(target, player);
            }
        }

        lspecial$removeLingeringFire(player, look, origin);
        lspecial$removeGroundFire(player, look, origin);
        player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.ENTITY_BLAZE_SHOOT,
                net.minecraft.util.SoundCategory.PLAYERS, 0.75F, 1.35F);
        // send a visible expanding blast plane to tracking clients; use WorldServer's packet path
        if (player.world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer serverWorld = (net.minecraft.world.WorldServer) player.world;
            Vec3d right = look.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));
            if (right.lengthSquared() < 0.0001D) right = new Vec3d(1.0D, 0.0D, 0.0D);
            right = right.normalize();
            Vec3d up = right.crossProduct(look).normalize();
            for (int ring = 1; ring <= 5; ring++) {
                double forward = ring * 1.35D;
                double radius = 0.30D + ring * 0.34D;
                int points = 18 + ring * 8;
                for (int i = 0; i < points; i++) {
                    double angle = (Math.PI * 2.0D * i) / points;
                    double x = origin.x + look.x * forward + (right.x * Math.cos(angle) + up.x * Math.sin(angle)) * radius;
                    double y = origin.y + look.y * forward + (right.y * Math.cos(angle) + up.y * Math.sin(angle)) * radius;
                    double z = origin.z + look.z * forward + (right.z * Math.cos(angle) + up.z * Math.sin(angle)) * radius;
                    serverWorld.spawnParticle(net.minecraft.util.EnumParticleTypes.FLAME, x, y, z, 0.0D, 0.015D, 0.0D);
                    serverWorld.spawnParticle(net.minecraft.util.EnumParticleTypes.CLOUD, x, y, z, look.x * 0.08D, look.y * 0.08D, look.z * 0.08D);
                }
            }
        }
        return true;
    }

    private static void lspecial$removeLingeringFire(EntityPlayer player, Vec3d look, Vec3d origin) {
        Vec3d horizontalLook = lspecial$horizontalLook(player, look);
        AxisAlignedBB area = player.getEntityBoundingBox().grow(AIRBLAST_RANGE + 0.5D, 3.0D, AIRBLAST_RANGE + 0.5D);
        for (EntityFireLingering fire : player.world.getEntitiesWithinAABB(EntityFireLingering.class, area)) {
            Vec3d delta = new Vec3d(fire.posX, fire.posY + fire.height * 0.5D, fire.posZ).subtract(origin);
            Vec3d horizontalDelta = new Vec3d(delta.x, 0.0D, delta.z);
            if (delta.length() <= AIRBLAST_RANGE + 0.5D && Math.abs(delta.y) <= 3.0D
                    && (horizontalDelta.lengthSquared() <= 0.25D
                    || horizontalLook.dotProduct(horizontalDelta.normalize()) >= 0.15D)) fire.setDead();
        }
    }

    private static Vec3d lspecial$horizontalLook(EntityPlayer player, Vec3d look) {
        Vec3d horizontalLook = new Vec3d(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSquared() < 0.0001D) {
            double yaw = Math.toRadians(player.rotationYaw);
            horizontalLook = new Vec3d(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        return horizontalLook.normalize();
    }

    private static void lspecial$removeGroundFire(EntityPlayer player, Vec3d look, Vec3d origin) {
        Vec3d horizontalLook = lspecial$horizontalLook(player, look);
        BlockPos min = new BlockPos(player.posX - AIRBLAST_RANGE, player.posY - 2.0D, player.posZ - AIRBLAST_RANGE);
        BlockPos max = new BlockPos(player.posX + AIRBLAST_RANGE, player.posY + 2.0D, player.posZ + AIRBLAST_RANGE);
        for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
            net.minecraft.block.Block block = player.world.getBlockState(pos).getBlock();
            if (block != Blocks.FIRE && block != ModBlocks.fire_digamma) continue;
            Vec3d center = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            Vec3d delta = center.subtract(origin);
            Vec3d horizontalDelta = new Vec3d(delta.x, 0.0D, delta.z);
            if (delta.length() <= AIRBLAST_RANGE + 0.5D && Math.abs(delta.y) <= 3.0D
                    && (horizontalDelta.lengthSquared() <= 0.25D
                    || horizontalLook.dotProduct(horizontalDelta.normalize()) >= 0.15D)) {
                player.world.setBlockToAir(pos);
            }
        }
    }


    private static boolean consumeAmmo(ItemStack stack, EntityPlayer player, int amount) {
        if (amount <= 0) return true;
        if (player == null || stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof ItemGunBaseNT)) return false;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            GunConfig config;
            try {
                config = gun.getConfig(stack, configIndex);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (config == null) continue;
            Receiver[] receivers = config.getReceivers(stack);
            if (receivers == null) continue;
            for (Receiver receiver : receivers) {
                if (receiver == null) continue;
                IMagazine<?> magazine = receiver.getMagazine(stack);
                if (magazine == null) continue;
                try {
                    int loaded = magazine.getAmount(stack, player.inventory);
                    if (loaded < amount) return false;
                    magazine.useUpAmmo(stack, player.inventory, amount);
                    return true;
                } catch (RuntimeException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void tickPressureLocks(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) event.player;
        for (ItemStack stack : player.inventory.mainInventory) lspecial$tickPressure(stack);
        for (ItemStack stack : player.inventory.armorInventory) lspecial$tickPressure(stack);
        lspecial$tickPressure(player.getHeldItemOffhand());
    }

    private static void lspecial$tickPressure(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()
                || stack.getItem().getRegistryName() == null
                || !"hbm:gun_flamer_topaz".equals(stack.getItem().getRegistryName().toString())) return;
        net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
        int lock = tag.getInteger("LSPECIALDragonsFuryPressureLock");
        if (lock > 0) tag.setInteger("LSPECIALDragonsFuryPressureLock", lock - 1);
    }


    private static void lspecial$redirectProjectileOwner(Entity projectile, EntityPlayer player) {
        try {
            for (java.lang.reflect.Method method : projectile.getClass().getMethods()) {
                if (!"setThrower".equals(method.getName()) || method.getParameterTypes().length != 1
                        || !method.getParameterTypes()[0].isAssignableFrom(player.getClass())) continue;
                method.invoke(projectile, player);
                return;
            }
        } catch (Throwable ignored) {
            // Some projectile classes do not expose a public thrower setter.
        }
        for (Class<?> type = projectile.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                String name = field.getName().toLowerCase(java.util.Locale.ROOT);
                if (!(name.contains("thrower") || name.contains("shootingentity") || name.equals("owner"))) continue;
                if (!field.getType().isAssignableFrom(player.getClass())) continue;
                try {
                    field.setAccessible(true);
                    field.set(projectile, player);
                    return;
                } catch (Throwable ignored) {
                    // Keep the redirected velocity even if a modded owner field cannot be changed.
                }
            }
        }
    }
}
