package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adjusts only the third-person camera distance to match the player's rendered Endurance scale.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow
    private float thirdPersonDistance;
    @Unique
    private float lspecial$originalThirdPersonDistance;
    @Unique
    private boolean lspecial$adjustedCameraDistance;

    @Inject(method = "orientCamera(F)V", at = @At("HEAD"))
    private void lspecial$scaleThirdPersonCamera(float partialTicks, CallbackInfo callback) {
        lspecial$adjustedCameraDistance = false;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || !LSPECIALConfig.enduranceModelScaling || !LSPECIALConfig.endurance
                || SPECIALScoreboard.inst == null || SPECIALScoreboard.inst.getClientEndurance() < 5) {
            return;
        }
        double hearts = player.getMaxHealth() / 2.0D;
        double excessHearts = Math.max(0.0D, hearts - LSPECIALConfig.enduranceModelScaleThresholdHearts);
        float scale = (float) Math.min(5.0D, 1.0D + excessHearts * LSPECIALConfig.enduranceModelScalePerHeart);
        if (scale <= 1.0F) {
            return;
        }

        lspecial$originalThirdPersonDistance = thirdPersonDistance;
        thirdPersonDistance *= scale;
        lspecial$adjustedCameraDistance = true;
    }

    @Inject(method = "orientCamera(F)V", at = @At("RETURN"))
    private void lspecial$restoreThirdPersonCamera(float partialTicks, CallbackInfo callback) {
        if (lspecial$adjustedCameraDistance) {
            thirdPersonDistance = lspecial$originalThirdPersonDistance;
            lspecial$adjustedCameraDistance = false;
        }
    }
}
