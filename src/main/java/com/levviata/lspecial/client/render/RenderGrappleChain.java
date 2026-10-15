package com.levviata.lspecial.client.render;

import com.levviata.lspecial.entity.EntityGrappleChain;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** Textured ribbon renderer for the synchronized grapple-chain entity. */
public class RenderGrappleChain extends Render<EntityGrappleChain> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("lspecial", "textures/entity/grapple_chain.png");

    public RenderGrappleChain(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(EntityGrappleChain chain, double x, double y, double z, float entityYaw, float partialTicks) {
        World world = chain.world;
        Entity owner = world.getEntityByID(chain.getOwnerId());
        Entity target = world.getEntityByID(chain.getTargetId());
        if (owner == null || target == null || owner.isDead || target.isDead) return;

        Vec3d start = new Vec3d(
                owner.lastTickPosX + (owner.posX - owner.lastTickPosX) * partialTicks,
                owner.lastTickPosY + (owner.posY - owner.lastTickPosY) * partialTicks + owner.getEyeHeight(),
                owner.lastTickPosZ + (owner.posZ - owner.lastTickPosZ) * partialTicks);
        Vec3d end = new Vec3d(
                target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks,
                target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + target.height * 0.5D,
                target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks);

        Vec3d direction = end.subtract(start);
        double length = direction.length();
        if (length < 0.001D) return;
        Vec3d view = new Vec3d(renderManager.viewerPosX - (start.x + end.x) * 0.5D,
                renderManager.viewerPosY - (start.y + end.y) * 0.5D,
                renderManager.viewerPosZ - (start.z + end.z) * 0.5D).normalize();
        Vec3d side = direction.normalize().crossProduct(view).normalize().scale(0.055D);
        if (side.length() < 0.001D) side = new Vec3d(0.04D, 0.0D, 0.0D);

        bindTexture(TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableCull();

        double sx = start.x - renderManager.viewerPosX;
        double sy = start.y - renderManager.viewerPosY;
        double sz = start.z - renderManager.viewerPosZ;
        double ex = end.x - renderManager.viewerPosX;
        double ey = end.y - renderManager.viewerPosY;
        double ez = end.z - renderManager.viewerPosZ;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(sx - side.x, sy - side.y, sz - side.z).tex(0.0D, 0.0D).endVertex();
        buffer.pos(sx + side.x, sy + side.y, sz + side.z).tex(1.0D, 0.0D).endVertex();
        buffer.pos(ex + side.x, ey + side.y, ez + side.z).tex(1.0D, length * 2.0D).endVertex();
        buffer.pos(ex - side.x, ey - side.y, ez - side.z).tex(0.0D, length * 2.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityGrappleChain entity) {
        return TEXTURE;
    }
}
