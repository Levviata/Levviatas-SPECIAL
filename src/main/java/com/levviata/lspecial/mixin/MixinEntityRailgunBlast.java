package com.levviata.lspecial.mixin;

import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.projectile.EntityRailgunBlast;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

/**
 * AMAT railgun projectile
 */
@Mixin(value = EntityRailgunBlast.class, remap = false)
public abstract class MixinEntityRailgunBlast {

    @Unique
    private static final String LSPECIAL_MARKER = "LSPECIAL_AMAT_RAILGUN";
    @Unique
    private static final double PROJECTILE_SPEED = 10.0D;
    @Unique
    private static final double HOMING_RANGE = 128.0D;

    @Inject(method = "onUpdate()V", at = @At("HEAD"), remap = false)
    private void lspecial$homeTowardQuackos(CallbackInfo ci) {
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
    }
}
