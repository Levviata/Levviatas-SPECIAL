package com.levviata.lspecial.mixin;

import com.hbm.config.BombConfig;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.projectile.EntityRailgunBlast;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

/** AMAT-only Quackos homing, projectile impact, and explosion cleanup. */
@Mixin(value = EntityRailgunBlast.class, remap = false)
public abstract class MixinEntityRailgunBlast {

    private static final String LSPECIAL_MARKER = "LSPECIAL_AMAT_QUACKOS_RAILGUN";
    private static final double PROJECTILE_SPEED = 10.0D;
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

    /**
     * NTM creates the actual railgun explosion through EntityNukeExplosionMK5.statFac.
     * Hook that exact call rather than the later spawnEntity call, so the cleanup
     * runs at the explosion's final impact coordinates.
     */
    @Inject(
            method = "onUpdate()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hbm/entity/logic/EntityNukeExplosionMK5;statFac(Lnet/minecraft/world/World;IDDD)Lcom/hbm/entity/logic/EntityNukeExplosionMK5;",
                    shift = At.Shift.BEFORE
            ),
            remap = false
    )
    private void lspecial$removeQuackosInTwentyBlockExplosionRadius(CallbackInfo ci) {
        EntityRailgunBlast blast = (EntityRailgunBlast) (Object) this;
        if (blast.world.isRemote || !blast.getEntityData().getBoolean(LSPECIAL_MARKER)) return;

        final double radius = 20.0D;
        final double radiusSq = radius * radius;
        final double x = blast.posX;
        final double y = blast.posY;
        final double z = blast.posZ;
        AxisAlignedBB area = new AxisAlignedBB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);

        for (EntityQuackos quackos : blast.world.getEntitiesWithinAABB(EntityQuackos.class, area)) {
            if (quackos != null && !quackos.isDead
                    && quackos.getDistanceSq(x, y, z) <= radiusSq) {
                lspecial$killQuackos(quackos);
            }
        }
    }

    private static void lspecial$killQuackos(EntityQuackos quackos) {
        // NTM's EntityQuackos overrides setDead() and only calls the parent on
        // the client, so calling setDead() on the server does not mark it dead.
        // Set the inherited flag directly, then remove it from the server world.
        quackos.isDead = true;
        quackos.world.removeEntity(quackos);
    }
}
