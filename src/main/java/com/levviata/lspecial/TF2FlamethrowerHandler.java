package com.levviata.lspecial;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** TF2-style secondary airblast for NTM's standard Sedna flamethrower. */
public final class TF2FlamethrowerHandler {
    private static final int AIRBLAST_AMMO_COST = 20;
    private static final double AIRBLAST_RANGE = 4.0D;
    private static final double AIRBLAST_CONE_DOT = 0.45D;

    /** Called on the server from the Sedna secondary-key hook. */
    public static boolean airblast(EntityPlayer player, ItemStack weapon) {
        if (player == null || weapon == null || weapon.isEmpty() || player.world.isRemote || player.isDead) {
            return false;
        }
        String itemId = weapon.getItem().getRegistryName() == null ? "" : weapon.getItem().getRegistryName().toString();
        boolean dragonsFury = "hbm:gun_flamer_topaz".equals(itemId);
        int ammoCost = dragonsFury ? 5 : AIRBLAST_AMMO_COST;
        if (!SednaAmmoAccess.consume(weapon, player, ammoCost)) return false;
        if (dragonsFury) {
            net.minecraft.nbt.NBTTagCompound tag = weapon.getTagCompound();
            if (tag == null) {
                tag = new net.minecraft.nbt.NBTTagCompound();
                weapon.setTagCompound(tag);
            }
            tag.setInteger("LSPECIALDragonFuryPressureLock", 32);
            tag.setBoolean("LSPECIALDragonFuryNextShotFast", false);
        }

        Vec3d look = player.getLookVec().normalize();
        Vec3d origin = player.getPositionEyes(1.0F);
        AxisAlignedBB search = player.getEntityBoundingBox().grow(AIRBLAST_RANGE + 1.0D);
        for (Entity target : player.world.getEntitiesWithinAABB(Entity.class, search,
                entity -> entity != player && !entity.isDead)) {
            Vec3d center = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);
            Vec3d delta = center.subtract(origin);
            double distance = delta.lengthVector();
            if (distance > AIRBLAST_RANGE || distance < 0.001D) continue;
            if (look.dotProduct(delta.scale(1.0D / distance)) < AIRBLAST_CONE_DOT) continue;

            if (target instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) target;
                boolean wasBurning = living.isBurning();
                living.extinguish();
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
                double speed = Math.max(1.2D, target.getPositionVector().subtract(player.getPositionVector()).lengthVector() > 0
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

        player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.ENTITY_BLAZE_SHOOT,
                net.minecraft.util.SoundCategory.PLAYERS, 0.75F, 1.35F);
        return true;
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
        int lock = tag.getInteger("LSPECIALDragonFuryPressureLock");
        if (lock > 0) tag.setInteger("LSPECIALDragonFuryPressureLock", lock - 1);
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
