package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
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
            new ResourceLocation("lspecial", "textures/gui/shotgun_crosshair.png");
    private static final float FOLLOW_SPEED = 0.18F;
    private static final float RETICLE_HEIGHT_RATIO = 64F / 1080.0F;
    private static final float HOOK_TO_RETICLE_RATIO = 28.0F / 192.0F;
    private static final float DEFAULT_HOOK_OFFSET_RATIO = 70F / 192.0F;

    private double reticleX = -1.0D;
    private double reticleY = -1.0D;
    private double hookX = -1.0D;
    private double hookY = -1.0D;
    private boolean hookWasFollowingTarget;
    private int previousTargetId = -1;

    @SubscribeEvent
    public void onCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.player.getHeldItemMainhand().isEmpty() || isNotDoubleBarrel(mc.player.getHeldItemMainhand()) || isDisabled(mc.player)) return;
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
        if (isDisabled(player) || held.isEmpty() || isNotDoubleBarrel(held)) {
            reset();
            return;
        }

        ScaledResolution resolution = new ScaledResolution(mc);
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        float scaleFactor = mc.displayWidth / (float) screenWidth;
        float centerX = screenWidth / 2.0F;
        float centerY = screenHeight / 2.0F;
        float reticleSize = screenHeight * RETICLE_HEIGHT_RATIO * (float)LSPECIALConfig.doubleBarrelGrappleCrosshairScale;
        float hookSize = reticleSize * HOOK_TO_RETICLE_RATIO * 1.4F * (float)LSPECIALConfig.doubleBarrelGrappleHookScale;
        float defaultHookOffset = reticleSize * DEFAULT_HOOK_OFFSET_RATIO;
        float targetX = centerX;
        float targetY = centerY;
        int cooldownTicks = GrappleClientHandler.getCooldownTicks();
        boolean cooldown = cooldownTicks > 0;
        Entity grappleTarget = cooldown ? null : findGrappleTarget(player, LSPECIALConfig.doubleBarrelGrappleRange);
        boolean hasTarget = grappleTarget != null;
        boolean targetChanged = hasTarget && grappleTarget.getEntityId() != previousTargetId;
        if (targetChanged) previousTargetId = grappleTarget.getEntityId();
        if (!hasTarget) previousTargetId = -1;
        if (grappleTarget != null) {
            float[] projected = projectEntity(grappleTarget, event.getPartialTicks(), mc);
            if (projected != null) { targetX = projected[0] / scaleFactor; targetY = projected[1] / scaleFactor; }
        }

        if (reticleX < 0.0D || reticleY < 0.0D) { reticleX = centerX; reticleY = centerY; }
        if (hookX < 0.0D || hookY < 0.0D) { hookX = centerX; hookY = centerY + reticleSize * 0.5D + defaultHookOffset; }
        double amount = 1.0D - Math.pow(1.0D - FOLLOW_SPEED, Math.max(1.0D, event.getPartialTicks() + 1.0D));
        reticleX += (targetX - reticleX) * amount;
        reticleY += (targetY - reticleY) * amount;

        // the crosshair stays centered; the hook follows targets independently and smoothly
        drawReticle(mc, centerX, centerY, reticleSize, cooldownTicks, scaleFactor, screenHeight);
        float defaultY = centerY + reticleSize * 0.5F + defaultHookOffset;
        int refreshAnimationTicks = GrappleClientHandler.getFangRefreshTicks();
        float splitAmount = 0.675F;
        if (cooldown) {
            hookX = centerX;
            hookY = defaultY;
            hookWasFollowingTarget = false;
            float split = reticleSize * splitAmount;
            drawHookHalf(mc, centerX - split, centerY, hookSize, false, 0.0F, scaleFactor, screenWidth, screenHeight);
            drawHookHalf(mc, centerX + split, centerY, hookSize, true, 0.0F, scaleFactor, screenWidth, screenHeight);
        } else if (refreshAnimationTicks < 6) {
            hookX = centerX;
            hookY = defaultY;
            hookWasFollowingTarget = false;
            float progress = Math.min(1.0F, refreshAnimationTicks / 6.0F);
            float startSplit = reticleSize * splitAmount;
            float split = startSplit * (1.0F - progress);
            float hookY = centerY + (defaultY - centerY) * progress;
            // the two fang halves join the hook at a constant rate
            drawHookHalf(mc, centerX - split, hookY, hookSize, false, progress, scaleFactor, screenWidth, screenHeight);
            drawHookHalf(mc, centerX + split, hookY, hookSize, true, progress, scaleFactor, screenWidth, screenHeight);
        } else {
            if (!hasTarget) {
                hookWasFollowingTarget = false;
                hookX += (centerX - hookX) * 0.24D;
                hookY += (defaultY - hookY) * 0.24D;
            } else {
                double dx = reticleX - hookX;
                double dy = reticleY - hookY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (targetChanged) hookWasFollowingTarget = false;
                if (distance <= Math.max(1.0D, hookSize * 0.12D)) {
                    hookX = reticleX;
                    hookY = reticleY;
                    hookWasFollowingTarget = true;
                } else {
                    double normalizedDistance = Math.min(1.0D, distance / Math.max(1.0D, reticleSize * 2.0D));
                    double followAmount = 0.035D + 0.30D * normalizedDistance;
                    followAmount = Math.min(0.38D, followAmount);
                    hookX += dx * followAmount;
                    hookY += dy * followAmount;
                }
            }
            drawHookIcon(mc, (float)hookX, (float)hookY, hookSize, false, screenWidth, screenHeight);
        }
    }

    private static void drawHookIcon(Minecraft mc, float x, float y, float size, boolean red, int screenWidth, int screenHeight) {
        mc.getTextureManager().bindTexture(new ResourceLocation("lspecial", "textures/gui/meathook.png"));
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, screenWidth, screenHeight, 0.0D, -1000.0D, 1000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, red ? 0.12F : 1.0F, red ? 0.12F : 1.0F, red ? 1.0F : 0.85F);
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

    private static void drawHookHalf(Minecraft mc, float x, float y, float size, boolean rightHalf, float progress, float scaleFactor, int screenWidth, int screenHeight) {
        mc.getTextureManager().bindTexture(new ResourceLocation("lspecial", "textures/gui/meathook.png"));
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, screenWidth, screenHeight, 0.0D, -1000.0D, 1000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GlStateManager.disableLighting(); GlStateManager.disableDepth(); GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 0.12F + 0.88F * progress, 0.12F + 0.88F * progress, 1.0F);
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

    private static void drawReticle(Minecraft mc, float x, float y, float size, int cooldownTicks, float scaleFactor, int screenHeight) {
        TextureManager textures = mc.getTextureManager();
        textures.bindTexture(RETICLE_TEXTURE);

        // Switch to pixel coordinates after world projection has been captured.
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, mc.displayWidth / scaleFactor, mc.displayHeight / scaleFactor, 0.0D, -1000.0D, 1000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        boolean cooldownVisual = cooldownTicks > 0 || GrappleClientHandler.getFangRefreshTicks() == 0;
        if (!cooldownVisual) {
            drawReticleQuad(x, y, size, 1.0F, 1.0F, 1.0F, 0.85F);
        } else {
            drawReticleQuad(x, y, size, 1.0F, 0.12F, 0.12F);
            // gray fills from top to bottom as the cooldown expires
            float cooldownRemaining = Math.max(0.0F, Math.min(1.0F, cooldownTicks / 60.0F));
            float progress = 1.0F - cooldownRemaining;
            float grayHeight = size * progress;
            if (grayHeight > 0.0F) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                float reticleWidth = size * (245.0F / 130.0F);
                int sx = Math.round((x - reticleWidth / 2.0F) * scaleFactor);
                int sy = Math.round((screenHeight - (y - size / 2.0F + grayHeight)) * scaleFactor);
                int sw = Math.round(reticleWidth * scaleFactor);
                int sh = Math.round(grayHeight * scaleFactor);
                GL11.glScissor(sx, sy, sw, sh);
                drawReticleQuad(x, y, size, 0.62F, 0.62F, 0.62F);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private static void drawReticleQuad(float x, float y, float size, float red, float green, float blue) {
        drawReticleQuad(x, y, size, red, green, blue, 1.0F);
    }

    private static void drawReticleQuad(float x, float y, float size, float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        float width = size * (245.0F / 130.0F);
        GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex3f(x - width / 2.0F, y + size / 2.0F, 0.0F);
        GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex3f(x + width / 2.0F, y + size / 2.0F, 0.0F);
        GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex3f(x + width / 2.0F, y - size / 2.0F, 0.0F);
        GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex3f(x - width / 2.0F, y - size / 2.0F, 0.0F);
        GL11.glEnd();
    }

    private void reset() {
        reticleX = reticleY = -1.0D;
        hookX = hookY = -1.0D;
        hookWasFollowingTarget = false;
    }

    private static float[] projectEntity(Entity target, float partialTicks, Minecraft mc) {
        FloatBuffer model = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, model);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + target.height * 0.68D - mc.getRenderManager().viewerPosY;
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
        double coneCos = Math.cos(Math.toRadians(LSPECIALConfig.doubleBarrelGrappleTargetCone));
        net.minecraft.util.math.Vec3d end = eye.add(look.scale(range));
        RayTraceResult blockHit = player.world.rayTraceBlocks(eye, end, false, true, false);
        double maxDistance = blockHit == null ? range : eye.distanceTo(blockHit.hitVec);
        net.minecraft.util.math.AxisAlignedBB searchBox = player.getEntityBoundingBox().grow(range);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity candidate : player.world.getEntitiesWithinAABBExcludingEntity(player, searchBox)) {
            if (!(candidate instanceof EntityLivingBase) || !candidate.isEntityAlive()) continue;
            net.minecraft.util.math.Vec3d chest = candidate.getPositionVector().add(0.0D, candidate.height * 0.68D, 0.0D);
            net.minecraft.util.math.Vec3d toTarget = chest.subtract(eye);
            double distance = toTarget.length();
            if (distance <= 0.0D || distance > maxDistance) continue;
            if (look.dotProduct(toTarget.scale(1.0D / distance)) < coneCos) continue;
            RayTraceResult blockCheck = player.world.rayTraceBlocks(eye, chest, false, true, false);
            if (blockCheck != null && eye.distanceTo(blockCheck.hitVec) + 0.05D < distance) continue;
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    private static boolean isDisabled(EntityPlayer player) {
        return !LSPECIALConfig.modifyGuns || !LSPECIALConfig.doubleBarrelGrappleVisuals
                || !LSPECIALConfig.doubleBarrelGrapple || !LSPECIALConfig.intelligence
                || !LSPECIALConfig.revolverPerk || SPECIALScoreboard.inst == null
                || player == null
                || SPECIALScoreboard.inst.getPlayerScore(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) == null
                || SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) < 15;
    }

    private static boolean isNotDoubleBarrel(ItemStack stack) {
        return stack.getItem().getRegistryName() == null
                || !"hbm:gun_double_barrel".equals(stack.getItem().getRegistryName().toString());
    }
}
