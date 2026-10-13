package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** prevents configured missile-family projectiles from impacting their own player while that launcher is held. */
@Mixin(value = EntityBulletBaseNT.class, remap = false)
public abstract class MixinStingerProjectile {

    /** cancels the self-impact before damage and impact effects are applied. */
    @Inject(method = "onEntityImpact(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$preventLauncherSelfImpact(Entity target, CallbackInfo callback) {
        if (!LSPECIALConfig.stingerSelfDamageProtection || !(target instanceof EntityPlayer)) return;

        EntityBulletBaseNT projectile = (EntityBulletBaseNT) (Object) this;
        EntityLivingBase shooter = projectile.getThrower();
        if (shooter != target) return;

        EntityPlayer player = (EntityPlayer) target;
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || held.getItem().getRegistryName() == null) return;

        String itemId = held.getItem().getRegistryName().toString();
        if ("hbm:gun_stinger".equals(itemId)
                || "hbm:gun_quadro".equals(itemId)
                || "hbm:gun_panzerschreck".equals(itemId)
                || "hbm:gun_missile_launcher".equals(itemId)) {
            callback.cancel();
        }
    }
}
