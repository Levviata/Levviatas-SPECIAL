package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityRailgunBlast;
import com.levviata.lspecial.AMATRailgun;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bridges the AMAT capsule's Sedna bullet to NTM's actual EntityRailgunBlast.
 *
 * HEAD handles the usual case. RETURN handles entities whose BulletConfig is
 * populated during onUpdate rather than before the first tick.
 */
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinEntityBulletBaseMK4 {

    @Inject(method = "onUpdate()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$replaceAmatCapsuleAtHead(CallbackInfo ci) {
        EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) (Object) this;
        if (bullet.world.isRemote || !AMATRailgun.isRailgunConfig(bullet.config)) return;
        if (lspecial$spawnRailgunBlast(bullet)) ci.cancel();
    }

    @Inject(method = "onUpdate()V", at = @At("RETURN"), remap = false)
    private void lspecial$replaceAmatCapsuleAfterConfigResolution(CallbackInfo ci) {
        EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) (Object) this;
        if (bullet.world.isRemote || bullet.isDead || !AMATRailgun.isRailgunConfig(bullet.config)) return;
        lspecial$spawnRailgunBlast(bullet);
    }

    private boolean lspecial$spawnRailgunBlast(EntityBulletBaseMK4 bullet) {
        if (bullet.isDead || bullet.world.isRemote) return false;

        EntityRailgunBlast blast = new EntityRailgunBlast(bullet.world);
        blast.setPosition(bullet.posX, bullet.posY, bullet.posZ);
        blast.motionX = bullet.motionX;
        blast.motionY = bullet.motionY;
        blast.motionZ = bullet.motionZ;

        NBTTagCompound data = blast.getEntityData();
        data.setBoolean("LSPECIAL_AMAT_RAILGUN", true);

        boolean spawned = bullet.world.spawnEntity(blast);
        if (spawned) {
            bullet.setDead();
            return true;
        }
        return false;
    }
}
