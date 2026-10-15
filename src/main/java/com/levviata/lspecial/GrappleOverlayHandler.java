package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Draws a screen-space grapple reticle. Its position eases from the screen center
 * toward the currently selected living entity, and eases back to center when no
 * target is selected (including when a target dies or changes).
 */
@SideOnly(Side.CLIENT)
public final class GrappleOverlayHandler {
    private static final ResourceLocation RETICLE_TEXTURE =
            new ResourceLocation("lspecial", "textures/gui/meathook.png");
    private static final float FOLLOW_SPEED = 0.18F;
    private static final float RETICLE_SIZE = 22.0F;

    private float reticleX = -1.0F;
    private float reticleY = -1.0F;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null) {
            reset();
            return;
        }

        ItemStack held = player.getHeldItemMainhand();
        if (!isEnabled() || held.isEmpty() || !isDoubleBarrel(held)) {
            reset();
            return;
        }

        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;
        float centerX = screenWidth / 2.0F;
        float centerY = screenHeight / 2.0F;
        float targetX = centerX;
        float targetY = centerY;

        Entity target = findGrappleTarget(player, LSPECIALConfig.doubleBarrelGrappleRange);
        if (target instanceof EntityLivingBase && target.isEntityAlive()) {
            RenderManager rm = mc.getRenderManager();
            double pt = event.getPartialTicks();
            double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * pt - rm.viewerPosX;
            double y = target.lastTickPosY + (target.posY - target.lastTickPosY) * pt
                    + target.height * 0.5D - rm.viewerPosY;
            double z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * pt - rm.viewerPosZ;

            FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
            FloatBuffer projection = BufferUtils.createFloatBuffer(16);
            IntBuffer viewport = BufferUtils.createIntBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

            FloatBuffer winPos = BufferUtils.createFloatBuffer(3);

            if (GLU.gluProject(
                    (float) x, (float) y, (float) z,
                    modelView, projection, viewport, winPos)
                    && winPos.get(2) >= 0.0F
                    && winPos.get(2) <= 1.0F) {
                targetX = winPos.get(0);
                targetY = screenHeight - winPos.get(1);
            }
        }

        if (reticleX < 0.0F || reticleY < 0.0F) {
            reticleX = centerX;
            reticleY = centerY;
        }
        // Frame-rate-independent easing keeps transitions smooth, including target swaps.
        float amount = 1.0F - (float)Math.pow(1.0F - FOLLOW_SPEED, Math.max(1.0F, event.getPartialTicks() + 1.0F));
        reticleX += (targetX - reticleX) * amount;
        reticleY += (targetY - reticleY) * amount;

        drawReticle(mc, reticleX, reticleY, RETICLE_SIZE);
    }

    private static void drawReticle(Minecraft mc, float x, float y, float size) {
        TextureManager textures = mc.getTextureManager();
        textures.bindTexture(RETICLE_TEXTURE);

        // Switch to pixel coordinates after world projection has been captured.
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, mc.displayWidth, mc.displayHeight, 0.0D, -1000.0D, 1000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex3f(x - size / 2.0F, y + size / 2.0F, 0.0F);
        GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex3f(x + size / 2.0F, y + size / 2.0F, 0.0F);
        GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex3f(x + size / 2.0F, y - size / 2.0F, 0.0F);
        GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex3f(x - size / 2.0F, y - size / 2.0F, 0.0F);
        GL11.glEnd();

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void reset() {
        reticleX = reticleY = -1.0F;
    }

    /** Ray-traces living entities within grapple range, respecting blocks in the way. */
    private static Entity findGrappleTarget(EntityPlayer player, double range) {
        if (range <= 0.0D) return null;

        net.minecraft.util.math.Vec3d eye = player.getPositionEyes(1.0F);
        net.minecraft.util.math.Vec3d look = player.getLook(1.0F).normalize();
        net.minecraft.util.math.Vec3d end = eye.add(look.scale(range));
        RayTraceResult blockHit = player.world.rayTraceBlocks(eye, end, false, true, false);
        double maxDistanceSq = blockHit == null ? range * range : eye.squareDistanceTo(blockHit.hitVec);
        net.minecraft.util.math.AxisAlignedBB searchBox = player.getEntityBoundingBox()
                .expand(look.x * range, look.y * range, look.z * range).grow(1.0D);
        Entity closest = null;

        for (Entity candidate : player.world.getEntitiesWithinAABBExcludingEntity(player, searchBox)) {
            if (!(candidate instanceof EntityLivingBase) || !candidate.isEntityAlive()) continue;
            net.minecraft.util.math.AxisAlignedBB bounds = candidate.getEntityBoundingBox()
                    .grow(candidate.getCollisionBorderSize());
            RayTraceResult intercept = bounds.calculateIntercept(eye, end);
            if (intercept == null) {
                if (!bounds.contains(eye)) continue;
                closest = candidate;
                maxDistanceSq = 0.0D;
                continue;
            }
            double distanceSq = eye.squareDistanceTo(intercept.hitVec);
            if (distanceSq < maxDistanceSq) {
                maxDistanceSq = distanceSq;
                closest = candidate;
            }
        }
        return closest;
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
