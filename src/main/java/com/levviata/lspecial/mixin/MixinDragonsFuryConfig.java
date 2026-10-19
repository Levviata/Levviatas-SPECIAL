package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.levviata.lspecial.LSPECIALConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// todo
// rationale: NTM's Topaz configs have no public addon hook for these projectile settings, so this adjusts only the four native Dragon's Fury configs after init. The mixin risks conflict with another addon changing XFactoryFlamer.init; it can be avoided if NTM exposes a config callback.
@Mixin(value = XFactoryFlamer.class, remap = false)
public abstract class MixinDragonsFuryConfig {
    @Unique
    private static final float LSPECIAL_DRAGONS_FURY_SPEED = 206.0F / 3.6F / 20.0F;
    @Unique
    private static final double LSPECIAL_DRAGONS_FURY_RANGE = 8.0D;

    @Inject(method = "init()V", at = @At("RETURN"), remap = false)
    private static void lspecial$configureDragonsFuryFireballs(CallbackInfo callback) {
        if (!LSPECIALConfig.dragonsFuryPerk) return;
        lspecial$singleFireball(XFactoryFlamer.flame_topaz_diesel);
        lspecial$singleFireball(XFactoryFlamer.flame_topaz_gas);
        lspecial$singleFireball(XFactoryFlamer.flame_topaz_napalm);
        lspecial$singleFireball(XFactoryFlamer.flame_topaz_balefire);
    }

    @Unique
    private static void lspecial$singleFireball(BulletConfig config) {
        if (config == null) return;
        config.setProjectiles(1);
        config.setSpread(0.0F);
        config.setVel(LSPECIAL_DRAGONS_FURY_SPEED);
        config.setLife(Math.max(1, (int) Math.ceil(LSPECIAL_DRAGONS_FURY_RANGE / LSPECIAL_DRAGONS_FURY_SPEED)));
    }
}
