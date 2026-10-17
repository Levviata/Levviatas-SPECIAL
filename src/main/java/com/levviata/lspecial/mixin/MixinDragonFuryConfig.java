package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Converts the Topaz flamer's native two-pellet spread configs into a single,
 * tight Dragon's Fury-style fireball. This uses NTM's actual config fields and
 * leaves the standard hbm:gun_flamer configs unchanged.
 */
@Mixin(value = XFactoryFlamer.class, remap = false)
public abstract class MixinDragonFuryConfig {
    @Inject(method = "init()V", at = @At("RETURN"), remap = false)
    private static void lspecial$configureDragonFuryFireballs(CallbackInfo callback) {
        lspecial$singleFireball(XFactoryFlamer.flame_topaz_diesel);
        lspecial$singleFireball(XFactoryFlamer.flame_topaz_gas);
        lspecial$singleFireball(XFactoryFlamer.flame_topaz_napalm);
        lspecial$singleFireball(XFactoryFlamer.flame_topaz_balefire);
    }

    @Unique
    private static void lspecial$singleFireball(BulletConfig config) {
        if (config != null) {
            config.setProjectiles(1);
            config.setSpread(0.0F);
        }
    }
}
