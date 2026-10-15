package com.levviata.lspecial.mixin;

import com.hbm.config.BombConfig;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.projectile.EntityRailgunBlast;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

/** AMAT-only Quackos homing, projectile impact, and explosion cleanup. */
@Mixin(value = EntityRailgunBlast.class, remap = false)
public abstract class MixinEntityRailgunBlast {

    @Unique
    private static final String LSPECIAL_MARKER = "LSPECIAL_AMAT_QUACKOS_RAILGUN";
    @Unique
    private static final double PROJECTILE_SPEED = 10.0D;
    @Unique
    private static final double HOMING_RANGE = 128.0D;

    @Inject(method = "onUpdate()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$homeAndHitQuackos(CallbackInfo ci) {
        EntityRailgunBlast blast = (EntityRailgunBlast) (Object) this;
        if (blast.world.isRemote || !blast.getEntityData().getBoolean(LSPECIAL_MARKER)) return;

        Vec3d start = blast.getPositionVector();
        EntityQuackos target = blast.world.getEntitiesWithinAABB(EntityQuackos.class,
                        blast.getEntityBoundingBox().grow(HOMING_RANGE))
                .stream()
                .filter(candidate -> !candidate.isDead)
                .min(Comparator.comparingDouble(blast::getDistanceSq))
                .orElse(null);

        if (target != null) {
            Vec3d destination = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);
            Vec3d direction = destination.subtract(start).normalize();
            blast.motionX = direction.x * PROJECTILE_SPEED;
            blast.motionY = direction.y * PROJECTILE_SPEED;
            blast.motionZ = direction.z * PROJECTILE_SPEED;
        } else {
            // Keep the faster launch velocity if there is no Quackos nearby.
            Vec3d motion = new Vec3d(blast.motionX, blast.motionY, blast.motionZ);
            if (motion.length() > 1.0E-5D) {
                Vec3d direction = motion.normalize();
                blast.motionX = direction.x * PROJECTILE_SPEED;
                blast.motionY = direction.y * PROJECTILE_SPEED;
                blast.motionZ = direction.z * PROJECTILE_SPEED;
            }
        }

        Vec3d end = start.add(blast.motionX, blast.motionY, blast.motionZ);
        AxisAlignedBB swept = blast.getEntityBoundingBox()
                .expand(blast.motionX, blast.motionY, blast.motionZ).grow(0.75D);
        EntityQuackos hit = blast.world.getEntitiesWithinAABB(EntityQuackos.class, swept).stream()
                .filter(candidate -> !candidate.isDead)
                .filter(candidate -> candidate.getEntityBoundingBox().grow(0.5D)
                        .calculateIntercept(start, end) != null)
                .min(Comparator.comparingDouble(blast::getDistanceSq))
                .orElse(null);

        if (hit != null) {
            lspecial$killQuackos(hit);
            blast.setDead();
            ci.cancel();
        }
    }

    /** Runs immediately before NTM spawns the native explosion from this blast. */
    @Inject(method = "onUpdate()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z", shift = At.Shift.BEFORE), remap = false)
    private void lspecial$killQuackosInExplosion(CallbackInfo ci) {
        EntityRailgunBlast blast = (EntityRailgunBlast) (Object) this;
        if (blast.world.isRemote || !blast.getEntityData().getBoolean(LSPECIAL_MARKER)) return;

        double radius = Math.max(1.0D, BombConfig.missileRadius / 2.0D);
        AxisAlignedBB area = new AxisAlignedBB(
                blast.posX - radius, blast.posY - radius, blast.posZ - radius,
                blast.posX + radius, blast.posY + radius, blast.posZ + radius);
        for (EntityQuackos quackos : blast.world.getEntitiesWithinAABB(EntityQuackos.class, area)) {
            if (!quackos.isDead && quackos.getDistanceSq(blast.posX, blast.posY, blast.posZ) <= radius * radius) {
                lspecial$killQuackos(quackos);
            }
        }
    }

    @Unique
    private static void lspecial$killQuackos(EntityQuackos quackos) {
        quackos.setDead();
        quackos.world.removeEntity(quackos);
    }
}
