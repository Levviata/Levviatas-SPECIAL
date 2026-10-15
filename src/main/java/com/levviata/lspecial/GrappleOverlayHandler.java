package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the local player's in-range grapple target marker. The chain itself is a synced entity. */
@SideOnly(Side.CLIENT)
public final class GrappleOverlayHandler {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (!isEnabled() || player == null || minecraft.fontRenderer == null) return;

        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !isDoubleBarrel(held)) return;

        RayTraceResult hit = minecraft.objectMouseOver;
        Entity target = hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY ? hit.entityHit : null;
        if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()
                || target.getDistance(player) > LSPECIALConfig.doubleBarrelGrappleRange) return;

        RenderManager renderManager = minecraft.getRenderManager();
        double partialTicks = event.getPartialTicks();
        double targetX = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
        double targetY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + target.height + 0.15D;
        double targetZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;
        drawWorldText(minecraft, renderManager, targetX, targetY, targetZ, "v", 0xFFFFFFFF, 0.05F);
    }

    private static void drawWorldText(Minecraft minecraft, RenderManager renderManager,
            double x, double y, double z, String text, int color, float scale) {
        net.minecraft.client.gui.FontRenderer font = minecraft.fontRenderer;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x - renderManager.viewerPosX, y - renderManager.viewerPosY, z - renderManager.viewerPosZ);
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        font.drawStringWithShadow(text, (float) -font.getStringWidth(text) / 2, 0, color);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static boolean isEnabled() {
        return LSPECIALConfig.modifyGuns && LSPECIALConfig.doubleBarrelGrappleVisuals
                && LSPECIALConfig.doubleBarrelGrapple && LSPECIALConfig.intelligence
                && LSPECIALConfig.revolverPerk && SPECIALScoreboard.inst != null
                && SPECIALScoreboard.inst.getIntelligenceScore() != null
                && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() >= 15;
    }

    private static boolean isDoubleBarrel(ItemStack stack) {
        return stack.getItem().getRegistryName() != null
                && "hbm:gun_double_barrel".equals(stack.getItem().getRegistryName().toString());
    }
}
