package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

/** client-only visual scaling. It never changes the entity's bounding box or attributes. */
public final class PlayerModelScaleHandler {
    private boolean lspecial$matrixPushed;
    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        lspecial$matrixPushed = false;
        if (!LSPECIALConfig.enduranceModelScaling || !LSPECIALConfig.endurance) return;
        if (lspecial$getEndurance(event.getEntityPlayer()) < 5) return;

        double hearts = event.getEntityPlayer().getMaxHealth() / 2.0D;
        double excessHearts = Math.max(0.0D, hearts - LSPECIALConfig.enduranceModelScaleThresholdHearts);
        double scale = 1.0D + excessHearts * LSPECIALConfig.enduranceModelScalePerHeart;
        // prevent accidental extreme model scaling from unusual health/config values.
        scale = Math.min(scale, 5.0D);
        if (scale > 1.0D) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, scale);
            lspecial$matrixPushed = true;
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (lspecial$matrixPushed) {
            GlStateManager.popMatrix();
            lspecial$matrixPushed = false;
        }
    }

    /** uses the client cache for the local player and the player's scoreboard for everyone else. */
    private static int lspecial$getEndurance(EntityPlayer player) {
        if (player == Minecraft.getMinecraft().player && inst != null) return inst.getClientEndurance();
        return inst == null ? 0 : inst.getPlayerScorePoints(player, inst.ENDURANCE_BOARD);
    }
}
