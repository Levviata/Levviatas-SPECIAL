package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.render.entity.projectile.RenderBulletMK4;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// rationale: NTM renders MK4 bullets through each BulletConfig.renderer, so adding tinted particles alone leaves the original projectile renderer visible. Cancel only the standard flamer render and let the client-side TF2 trail represent it; this can conflict with other render mixins, and is avoidable if NTM adds a configurable renderer toggle.
@Mixin(value = RenderBulletMK4.class, remap = false)
public abstract class MixinTF2FlameRender {
    @Inject(method = "doRender(Lcom/hbm/entity/projectile/EntityBulletBaseMK4;DDDFF)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$replaceStandardFlamerRender(EntityBulletBaseMK4 bullet, double x, double y, double z, float yaw, float partialTicks, CallbackInfo callback) {
        BulletConfig config = bullet.config != null ? bullet.config : bullet.getBulletConfig();
        if (lspecial$isStandardFlame(config) || lspecial$isHeldStandardFlamer(bullet)) callback.cancel();
    }

    private static boolean lspecial$isHeldStandardFlamer(EntityBulletBaseMK4 bullet) {
        EntityLivingBase shooter = bullet.getThrower();
        return shooter != null && (lspecial$isStandardFlamerItem(shooter.getHeldItemMainhand()) || lspecial$isStandardFlamerItem(shooter.getHeldItemOffhand()));
    }

    private static boolean lspecial$isStandardFlamerItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getRegistryName() != null
                && "hbm:gun_flamer".equals(stack.getItem().getRegistryName().toString());
    }

    private static boolean lspecial$isStandardFlame(BulletConfig config) {
        return config != null && (config == XFactoryFlamer.flame_nograv || config == XFactoryFlamer.flame_diesel
                || config == XFactoryFlamer.flame_gas || config == XFactoryFlamer.flame_napalm
                || config == XFactoryFlamer.flame_balefire);
    }
}
