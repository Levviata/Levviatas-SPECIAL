package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** renders the grapple's text chain and target marker in world space. */
@SideOnly(Side.CLIENT)
public final class GrappleOverlayHandler {
    private static final String VISUAL_TICKS_TAG = "LSPECIALGrappleVisualTicks";

    /** expires the temporary chain indicator stored on the held weapon. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null) return;

        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.isEmpty() || !isDoubleBarrel(stack) || !stack.hasTagCompound()) continue;
            int ticks = stack.getTagCompound().getInteger(VISUAL_TICKS_TAG);
            if (ticks > 0) {
                stack.getTagCompound().setInteger(VISUAL_TICKS_TAG, ticks - 1);
                if (!stack.getTagCompound().hasKey("LSPECIALGrappleTargetId")) {
                    RayTraceResult hit = minecraft.objectMouseOver;
                    if (hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
                            && hit.entityHit instanceof EntityLivingBase) {
                        stack.getTagCompound().setInteger("LSPECIALGrappleTargetId", hit.entityHit.getEntityId());
                    }
                }
                if (ticks == 1) stack.getTagCompound().removeTag("LSPECIALGrappleTargetId");
            }
        }
    }

    /** draws a world-space target marker and a visible text chain along the grapple path. */
    @SubscribeEvent
    public void onRenderWorldLast(net.minecraftforge.client.event.RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (!isEnabled() || player == null || minecraft.fontRenderer == null) {
            return;
        }

        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !isDoubleBarrel(held)) {
            return;
        }

        int chainTicks = held.hasTagCompound() ? held.getTagCompound().getInteger(VISUAL_TICKS_TAG) : 0;
        Entity target = null;
        if (chainTicks > 0 && held.getTagCompound().hasKey("LSPECIALGrappleTargetId")) {
            target = player.world.getEntityByID(held.getTagCompound().getInteger("LSPECIALGrappleTargetId"));
        }
        if (target == null) {
            RayTraceResult hit = minecraft.objectMouseOver;
            target = hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY ? hit.entityHit : null;
        }
        if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()
                || target.getDistance(player) > LSPECIALConfig.doubleBarrelGrappleRange) {
            return;
        }

        net.minecraft.client.renderer.entity.RenderManager renderManager = minecraft.getRenderManager();
        double partialTicks = event.getPartialTicks();
        double targetX = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks;
        double targetY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + target.height * 0.5D;
        double targetZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks;

        if (chainTicks > 0) {
            double startX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
            double startY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.getEyeHeight();
            double startZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
            double dx = targetX - startX, dy = targetY - startY, dz = targetZ - startZ;
            int links = Math.max(2, Math.min(48, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 1.5D)));
            for (int i = 1; i < links; i++) {
                double fraction = i / (double) links;
                lspecial$drawWorldText(minecraft, renderManager,
                        startX + dx * fraction, startY + dy * fraction, startZ + dz * fraction,
                        (i % 2 == 0) ? "o" : "-", 0xFFFFC45A, 0.045F);
            }
        }

        lspecial$drawWorldText(minecraft, renderManager, targetX, targetY, targetZ, "v", 0xFFFFFFFF, 0.05F);
    }

    /** draws a camera-facing glyph at a world coordinate without depth occlusion. */
    private static void lspecial$drawWorldText(Minecraft minecraft,
            net.minecraft.client.renderer.entity.RenderManager renderManager,
            double x, double y, double z, String text, int color, float scale) {
        net.minecraft.client.gui.FontRenderer font = minecraft.fontRenderer;
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(x - renderManager.viewerPosX,
                y - renderManager.viewerPosY, z - renderManager.viewerPosZ);
        net.minecraft.client.renderer.GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        net.minecraft.client.renderer.GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        net.minecraft.client.renderer.GlStateManager.scale(-scale, -scale, scale);
        net.minecraft.client.renderer.GlStateManager.disableLighting();
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        font.drawStringWithShadow(text, -font.getStringWidth(text) / 2, 0, color);
        net.minecraft.client.renderer.GlStateManager.enableDepth();
        net.minecraft.client.renderer.GlStateManager.enableLighting();
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    private static boolean isEnabled() {
        return LSPECIALConfig.modifyGuns && LSPECIALConfig.doubleBarrelGrappleVisuals
                && LSPECIALConfig.doubleBarrelGrapple && LSPECIALConfig.intelligence
                && LSPECIALConfig.revolverPerk && SPECIALScoreboard.inst != null
                && SPECIALScoreboard.inst.getIntelligenceScore() != null
                && SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() >= 15;
    }

    /** checks the native double-barrel registry id without depending on its translated name. */
    private static boolean isDoubleBarrel(ItemStack stack) {
        return stack.getItem().getRegistryName() != null
                && "hbm:gun_double_barrel".equals(stack.getItem().getRegistryName().toString());
    }
}
