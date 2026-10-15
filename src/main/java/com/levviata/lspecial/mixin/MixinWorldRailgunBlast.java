package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.entity.projectile.EntityRailgunBlast;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.levviata.lspecial.AMATRailgun;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces the projectile at the actual World.spawnEntity boundary.
 *
 * The previous onUpdate-based approach could miss the shot if the active config
 * was resolved differently or if the gun fired a hitscan EntityBulletBeamBase.
 * This hook sees both ordinary Sedna bullets and beam/hitscan entities before
 * the original entity is inserted into the world.
 */
@Mixin(World.class)
public abstract class MixinWorldRailgunBlast {

    @ModifyVariable(
            method = "spawnEntity(Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Entity lspecial$replaceAmatShotWithRailgunBlast(Entity original) {
        if (original == null) return null;

        World world = (World) (Object) this;
        BulletConfig config = null;
        EntityLivingBase shooter = null;
        double dx = original.motionX;
        double dy = original.motionY;
        double dz = original.motionZ;

        if (original instanceof EntityBulletBaseMK4) {
            EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) original;
            config = bullet.config;
            shooter = bullet.getThrower();
        } else if (original instanceof EntityBulletBeamBase) {
            EntityBulletBeamBase beam = (EntityBulletBeamBase) original;
            config = beam.config;
            shooter = beam.thrower;
            dx = beam.headingX;
            dy = beam.headingY;
            dz = beam.headingZ;
        } else {
            return original;
        }

        if (!lspecial$isAmatCapsuleShot(config, shooter)) return original;

        // Beam heading is usually normalized; give the native railgun entity
        // enough speed to travel as a projectile instead of behaving like a ray.
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5D && shooter != null) {
            double yaw = Math.toRadians(shooter.rotationYaw);
            double pitch = Math.toRadians(shooter.rotationPitch);
            dx = -Math.sin(yaw) * Math.cos(pitch);
            dy = -Math.sin(pitch);
            dz = Math.cos(yaw) * Math.cos(pitch);
            length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        if (length < 1.0E-5D) return original;

        double speed = 10.0D;
        EntityRailgunBlast blast = new EntityRailgunBlast(world);
        blast.setPosition(original.posX, original.posY, original.posZ);
        blast.motionX = dx / length * speed;
        blast.motionY = dy / length * speed;
        blast.motionZ = dz / length * speed;
        blast.rotationYaw = original.rotationYaw;
        blast.rotationPitch = original.rotationPitch;
        blast.getEntityData().setBoolean("LSPECIAL_AMAT_RAILGUN", true);
        return blast;
    }

    private static boolean lspecial$isAmatCapsuleShot(BulletConfig config, EntityLivingBase shooter) {
        if (AMATRailgun.isRailgunConfig(config)) return true;

        // Defensive fallback: some NTM magazine paths resolve the registered
        // ammo config rather than returning the custom BulletConfig instance.
        // Keep this fallback limited to the AMAT actually being held.
        if (shooter == null || config == null || config.ammo == null) return false;
        if (config.ammo.item != ModItems.charge_railgun) return false;
        ItemStack main = shooter.getHeldItemMainhand();
        ItemStack off = shooter.getHeldItemOffhand();
        return lspecial$isAmat(main) || lspecial$isAmat(off);
    }

    private static boolean lspecial$isAmat(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && "hbm".equals(id.getNamespace()) && "gun_amat".equals(id.getPath());
    }
}
