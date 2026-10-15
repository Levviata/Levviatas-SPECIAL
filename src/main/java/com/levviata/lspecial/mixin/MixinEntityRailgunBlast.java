package com.levviata.lspecial.mixin;

import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.projectile.EntityRailgunBlast;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

/**
 * Adds AMAT-only Quackos homing/collision to EntityRailgunBlast.
 * Unmarked native railgun blasts keep NTM's original update behavior.
 */
@Mixin(value = EntityRailgunBlast.class, remap = false)
public abstract class MixinEntityRailgunBlast {

    @Inject(method = "onUpdate()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$homeAndHitQuackos(CallbackInfo ci) {
        EntityRailgunBlast blast = (EntityRailgunBlast) (Object) this;
        if (blast.world.isRemote || !blast.getEntityData().getBoolean("LSPECIAL_AMAT_QUACKOS_RAILGUN")) return;

        Vec3d start = blast.getPositionVector();
        EntityQuackos target = blast.world.getEntitiesWithinAABB(EntityQuackos.class,
                        blast.getEntityBoundingBox().grow(128.0D))
                .stream()
                .filter(candidate -> !candidate.isDead)
                .min(Comparator.comparingDouble(blast::getDistanceSq))
                .orElse(null);

        if (target != null) {
            Vec3d destination = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);
            Vec3d direction = destination.subtract(start).normalize();
            double speed = Math.sqrt(blast.motionX * blast.motionX
                    + blast.motionY * blast.motionY + blast.motionZ * blast.motionZ);
            if (speed < 0.1D) speed = 3.5D;
            blast.motionX = direction.x * speed;
            blast.motionY = direction.y * speed;
            blast.motionZ = direction.z * speed;
        }

        Vec3d end = start.add(blast.motionX, blast.motionY, blast.motionZ);
        AxisAlignedBB swept = blast.getEntityBoundingBox()
                .expand(blast.motionX, blast.motionY, blast.motionZ).grow(0.5D);
        EntityQuackos hit = blast.world.getEntitiesWithinAABB(EntityQuackos.class, swept).stream()
                .filter(candidate -> !candidate.isDead)
                .filter(candidate -> candidate.getEntityBoundingBox().grow(0.3D)
                        .calculateIntercept(start, end) != null)
                .min(Comparator.comparingDouble(blast::getDistanceSq))
                .orElse(null);

        if (hit != null) {
            hit.setDead();
            hit.world.removeEntity(hit);
            blast.setDead();
            ci.cancel();
        }
    }
}
