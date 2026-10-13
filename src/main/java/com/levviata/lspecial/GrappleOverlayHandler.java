package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** renders the grapple's lightweight text chain and its extra crosshair marker. */
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
            if (ticks > 0) stack.getTagCompound().setInteger(VISUAL_TICKS_TAG, ticks - 1);
        }
    }

    /** draws the v marker when aiming at a living entity and the chain for a short time after firing. */
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || !LSPECIALConfig.modifyGuns
                || !LSPECIALConfig.doubleBarrelGrappleVisuals || !LSPECIALConfig.doubleBarrelGrapple
                || !LSPECIALConfig.intelligence
                || !LSPECIALConfig.revolverPerk || SPECIALScoreboard.inst == null
                || SPECIALScoreboard.inst.getIntelligenceScore() == null
                || SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() < 15) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || minecraft.fontRenderer == null) return;
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !isDoubleBarrel(held)) return;

        RayTraceResult hit = minecraft.objectMouseOver;
        Entity target = hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY ? hit.entityHit : null;
        if (!(target instanceof EntityLivingBase) || !target.isEntityAlive()
                || target.getDistance(player) > LSPECIALConfig.doubleBarrelGrappleRange) return;

        FontRenderer font = minecraft.fontRenderer;
        int centerX = event.getResolution().getScaledWidth() / 2;
        int centerY = event.getResolution().getScaledHeight() / 2;
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        font.drawStringWithShadow("v", centerX + 5, centerY + 5, 0xFFFFFF);

        int chainTicks = held.hasTagCompound() ? held.getTagCompound().getInteger(VISUAL_TICKS_TAG) : 0;
        if (chainTicks > 0) {
            int glyphCount = Math.max(1, Math.min(32, (int) Math.ceil(player.getDistance(target))));
            StringBuilder chain = new StringBuilder(glyphCount * 2);
            for (int i = 0; i < glyphCount; i++) {
                if (i > 0) chain.append('-');
                chain.append('o');
            }
            String text = chain.toString();
            font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2, centerY + 17, 0xFFFFFF);
        }

        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    /** checks the native double-barrel registry id without depending on its translated name. */
    private static boolean isDoubleBarrel(ItemStack stack) {
        return stack.getItem().getRegistryName() != null
                && "hbm:gun_double_barrel".equals(stack.getItem().getRegistryName().toString());
    }
}
