package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.render.entity.projectile.RenderBulletMK4;
import com.levviata.lspecial.client.render.TF2FlameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;

// ass
// rationale: NTM's confirmed renderer delegates visuals to BulletConfig.renderer, but standard flamer configs leave it null. On the client only, clone each standard flame config once and assign a dedicated renderer to the clone; do not change shared NTM configs or the synced config ID. This avoids global gameplay changes and keeps NTM's push/pop matrix path intact. The clone is cached per original config to avoid unbounded registration; client-only config registration is local and never written to the entity data manager.
@Mixin(value = RenderBulletMK4.class, remap = false)
public abstract class MixinTF2FlameRender {
    @Unique private static final Map<BulletConfig, BulletConfig> lspecial$renderConfigs = new IdentityHashMap<>();

    @Inject(method = "doRender(Lcom/hbm/entity/projectile/EntityBulletBaseMK4;DDDFF)V", at = @At("HEAD"), remap = false)
    private void lspecial$installDedicatedRenderer(EntityBulletBaseMK4 bullet, double x, double y, double z, float yaw, float partialTicks, CallbackInfo callback) {
        if (bullet.world == null || !bullet.world.isRemote) return;
        BulletConfig active = bullet.config != null ? bullet.config : bullet.getBulletConfig();
        if (active == null) return;
        if (lspecial$isStandardFlame(active)) {
            BulletConfig renderConfig = lspecial$renderConfigs.get(active);
            if (renderConfig == null) {
                renderConfig = active.clone();
                if (renderConfig == null) return;
                renderConfig.setRenderer(TF2FlameRenderer::render);
                renderConfig.renderRotations = false;
                lspecial$renderConfigs.put(active, renderConfig);
            }
            bullet.config = renderConfig;
        }
    }

    @Unique private static boolean lspecial$isStandardFlame(BulletConfig active) {
        return active == XFactoryFlamer.flame_nograv || active == XFactoryFlamer.flame_diesel
                || active == XFactoryFlamer.flame_gas || active == XFactoryFlamer.flame_napalm
                || active == XFactoryFlamer.flame_balefire;
    }
}
