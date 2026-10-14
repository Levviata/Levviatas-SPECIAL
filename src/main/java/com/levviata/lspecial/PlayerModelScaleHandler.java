package com.levviata.lspecial;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

/** client-only visual scaling. It never changes the entity's bounding box or attributes. */
public final class PlayerModelScaleHandler {
    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!LSPECIALConfig.enduranceModelScaling || !LSPECIALConfig.endurance) return;
        if (inst.getClientEndurance() < 5) return;

        double hearts = event.getEntityPlayer().getMaxHealth() / 2.0D;
        double excessHearts = Math.max(0.0D, hearts - LSPECIALConfig.enduranceModelScaleThresholdHearts);
        double scale = 1.0D + excessHearts * LSPECIALConfig.enduranceModelScalePerHeart;
        // prevent accidental extreme model scaling from unusual health/config values.
        scale = Math.min(scale, 5.0D);
        if (scale > 1.0D) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, scale);
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!LSPECIALConfig.enduranceModelScaling || !LSPECIALConfig.endurance
                || inst.getClientEndurance() < 5) return;

        double hearts = event.getEntityPlayer().getMaxHealth() / 2.0D;
        double excessHearts = Math.max(0.0D, hearts - LSPECIALConfig.enduranceModelScaleThresholdHearts);
        double scale = Math.min(5.0D, 1.0D + excessHearts * LSPECIALConfig.enduranceModelScalePerHeart);
        if (scale > 1.0D) {
            GlStateManager.popMatrix();
        }
    }
}
