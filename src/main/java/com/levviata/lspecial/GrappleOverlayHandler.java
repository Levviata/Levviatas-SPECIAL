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
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Draws a centered shotgun reticle and a target-following meathook indicator.
 */
@SideOnly(Side.CLIENT)
public final class GrappleOverlayHandler {
    private static final ResourceLocation RETICLE_TEXTURE =
            new ResourceLocation("hbm", "textures/misc/shotgun_crosshair.png");
    private static final float FOLLOW_SPEED = 0.18F;
    private static final float RETICLE_SIZE = 192.0F;

    private float reticleX = -1.0F;
    private float reticleY = -1.0F;

    @SubscribeEvent
    public void onCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.player.getHeldItemMainhand().isEmpty() || !isDoubleBarrel(mc.player.getHeldItemMainhand()) || !isEnabled()) return;
        event.setCanceled(true);
    }

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
        boolean cooldown = GrappleClientHandler.isOnCooldown();
        Entity grappleTarget = cooldown ? null : findGrappleTarget(player, LSPECIALConfig.doubleBarrelGrappleRange);
        if (grappleTarget != null) {
            float[] projected = projectEntity(grappleTarget, event.getPartialTicks(), mc);
            if (projected != null) { targetX = projected[0]; targetY = projected[1]; }
        }

        if (reticleX < 0.0F || reticleY < 0.0F) { reticleX = centerX; reticleY = centerY; }
        float amount = 1.0F - (float)Math.pow(1.0F - FOLLOW_SPEED, Math.max(1.0F, event.getPartialTicks() + 1.0F));
        reticleX += (targetX - reticleX) * amount;
        reticleY += (targetY - reticleY) * amount;

        // the crosshair stays centered; only the meathook indicator tracks targets
        drawReticle(mc, centerX, centerY, RETICLE_SIZE, cooldown);
        if (cooldown) {
            float displacement = Math.max(14.0F, RETICLE_SIZE * 0.5F);
            drawHookHalf(mc, centerX - displacement, centerY + RETICLE_SIZE * 0.5F + 7.0F, 14.0F, false);
            drawHookHalf(mc, centerX + displacement, centerY + RETICLE_SIZE * 0.5F + 7.0F, 14.0F, true);
        } else {
            drawHookIcon(mc, reticleX, reticleY + RETICLE_SIZE * 0.5F + 7.0F, 14.0F, false);
        }
    }

    private static void drawHookIcon(Minecraft mc, float x, float y, float size, boolean red) {
        mc.getTextureManager().bindTexture(new ResourceLocation("lspecial", "textures/gui/meathook.png"));
        GL11.glPushMatrix();
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
        GlStateManager.color(red ? 1.0F : 1.0F, red ? 0.12F : 1.0F, red ? 0.12F : 1.0F, 1.0F);
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
        GL11.glPopMatrix();
    }

    private static void drawHookHalf(Minecraft mc, float x, float y, float size, boolean rightHalf) {
        mc.getTextureManager().bindTexture(new ResourceLocation("lspecial", "textures/gui/meathook.png"));
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, mc.displayWidth, mc.displayHeight, 0.0D, -1000.0D, 1000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GlStateManager.disableLighting(); GlStateManager.disableDepth(); GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 0.12F, 0.12F, 1.0F);
        float u0 = rightHalf ? 0.5F : 0.0F, u1 = rightHalf ? 1.0F : 0.5F;
        float x0 = rightHalf ? x : x - size / 2.0F, x1 = rightHalf ? x + size / 2.0F : x;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u0, 1.0F); GL11.glVertex3f(x0, y + size / 2.0F, 0.0F);
        GL11.glTexCoord2f(u1, 1.0F); GL11.glVertex3f(x1, y + size / 2.0F, 0.0F);
        GL11.glTexCoord2f(u1, 0.0F); GL11.glVertex3f(x1, y - size / 2.0F, 0.0F);
        GL11.glTexCoord2f(u0, 0.0F); GL11.glVertex3f(x0, y - size / 2.0F, 0.0F);
        GL11.glEnd();
        GlStateManager.color(1, 1, 1, 1); GlStateManager.disableBlend(); GlStateManager.enableDepth(); GlStateManager.enableLighting();
        GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
    }

    private static void drawReticle(Minecraft mc, float x, float y, float size, boolean red) {
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
        GlStateManager.color(1.0F, red ? 0.12F : 1.0F, red ? 0.12F : 1.0F, 1.0F);
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

    private static float[] projectEntity(Entity target, float partialTicks, Minecraft mc) {
        FloatBuffer model = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, model);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + target.height * 0.5D - mc.getRenderManager().viewerPosY;
        double z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
        FloatBuffer screen = BufferUtils.createFloatBuffer(3);
        if (!GLU.gluProject((float)x, (float)y, (float)z, model, projection, viewport, screen)) return null;
        float px = screen.get(0);
        float py = mc.displayHeight - screen.get(1);
        if (screen.get(2) < 0.0F || screen.get(2) > 1.0F || px < 0 || py < 0 || px > mc.displayWidth || py > mc.displayHeight) return null;
        return new float[]{px, py};
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
