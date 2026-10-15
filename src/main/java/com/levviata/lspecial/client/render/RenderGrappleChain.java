package com.levviata.lspecial.client.render;

import com.levviata.lspecial.entity.EntityGrappleChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Renders the supplied OBJ chain links with a movement-driven, damped curve. */
public class RenderGrappleChain extends Render<EntityGrappleChain> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("hbm", "textures/models/misc/universaldark.png");
    private static final ResourceLocation MODEL = new ResourceLocation("lspecial", "models/n45_chain.obj");
    private static final List<Vec3d> VERTICES = new ArrayList<>();
    private static final List<float[]> UVS = new ArrayList<>();
    private static final List<int[]> FACES = new ArrayList<>();
    private static boolean modelLoaded;
    private static final java.util.Map<Integer, Double> BEND_STATE = new java.util.HashMap<>();

    public RenderGrappleChain(RenderManager manager) { super(manager); }

    private static void loadModel() {
        if (modelLoaded) return;
        modelLoaded = true;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(MODEL).getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] a = line.split("\\s+");
                    VERTICES.add(new Vec3d(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
                } else if (line.startsWith("vt ")) {
                    String[] a = line.split("\\s+");
                    UVS.add(new float[]{Float.parseFloat(a[1]), Float.parseFloat(a[2])});
                } else if (line.startsWith("f ")) {
                    String[] a = line.substring(2).trim().split("\\s+");
                    for (int i = 1; i + 1 < a.length; i++) {
                        FACES.add(face(a[0], a[i], a[i + 1]));
                    }
                }
            }
        } catch (Exception ignored) {
            VERTICES.clear(); UVS.clear(); FACES.clear();
        }
    }

    private static int[] face(String a, String b, String c) {
        String[] values = {a, b, c};
        int[] result = new int[6];
        for (int i = 0; i < 3; i++) {
            String[] pair = values[i].split("/");
            result[i * 2] = Integer.parseInt(pair[0]) - 1;
            result[i * 2 + 1] = pair.length > 1 && !pair[1].isEmpty() ? Integer.parseInt(pair[1]) - 1 : -1;
        }
        return result;
    }

    @Override
    public void doRender(EntityGrappleChain chain, double x, double y, double z, float entityYaw, float partialTicks) {
        World world = chain.world;
        Entity owner = world.getEntityByID(chain.getOwnerId());
        Entity target = world.getEntityByID(chain.getTargetId());
        if (owner == null || target == null || owner.isDead || target.isDead) return;
        loadModel();
        if (VERTICES.isEmpty() || FACES.isEmpty()) return;

        Vec3d ownerPos = interpolated(owner, partialTicks);
        Vec3d look = owner.getLook(partialTicks).normalize();
        Vec3d right = look.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D)).normalize();
        if (right.lengthSquared() < 1.0E-4D) right = new Vec3d(1.0D, 0.0D, 0.0D);
        Vec3d start = ownerPos.add(0.0D, owner.getEyeHeight(), 0.0D)
                .add(look.scale(0.42D)).add(right.scale(0.22D)).add(0.0D, -0.12D, 0.0D);
        Vec3d end = interpolated(target, partialTicks).add(0.0D, target.height * 0.5D, 0.0D);
        Vec3d direction = end.subtract(start);
        double length = direction.length();
        if (length < 0.05D) return;
        Vec3d axis = direction.normalize();
        Vec3d side = axis.crossProduct(new Vec3d(0, 1, 0)).normalize();
        if (side.lengthSquared() < 0.001D) side = new Vec3d(1, 0, 0);
        Vec3d playerMotion = new Vec3d(owner.motionX, 0, owner.motionZ);
        double desiredBend = Math.max(-0.9D, Math.min(0.9D, playerMotion.dotProduct(side) * 0.8D));
        double bendState = BEND_STATE.containsKey(chain.getEntityId()) ? BEND_STATE.get(chain.getEntityId()) : 0.0D;
        bendState += (desiredBend - bendState) * 0.12D;
        BEND_STATE.put(chain.getEntityId(), bendState);
        int links = Math.max(1, Math.min(100, (int)Math.ceil(length / 0.38D)));

        bindTexture(TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        for (int i = 0; i < links; i++) {
            double t = (i + 0.5D) / links;
            double bend = bendState * Math.sin(Math.PI * t) * Math.min(1.0D, t * 3.0D);
            Vec3d point = start.add(direction.scale(t)).add(side.scale(bend));
            Vec3d tangent = direction.add(side.scale(bendState * Math.PI * Math.cos(Math.PI * t)));
            renderLink(point, tangent.normalize(), side);
        }
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldRender(EntityGrappleChain chain, net.minecraft.client.renderer.culling.ICamera camera, double camX, double camY, double camZ) {
        Entity owner = chain.world.getEntityByID(chain.getOwnerId());
        Entity target = chain.world.getEntityByID(chain.getTargetId());
        if (owner == null || target == null || owner.isDead || target.isDead || chain.isDead) return false;
        return true;
    }

    private void renderLink(Vec3d point, Vec3d direction, Vec3d side) {
        Vec3d yAxis = new Vec3d(0, 1, 0);
        Vec3d rotationAxis = yAxis.crossProduct(direction);
        double dot = Math.max(-1.0D, Math.min(1.0D, yAxis.dotProduct(direction)));
        double angle = Math.toDegrees(Math.acos(dot));
        GlStateManager.pushMatrix();
        GlStateManager.translate(point.x - renderManager.viewerPosX, point.y - renderManager.viewerPosY, point.z - renderManager.viewerPosZ);
        if (rotationAxis.lengthSquared() > 1.0E-6D) GlStateManager.rotate((float)angle, (float)rotationAxis.x, (float)rotationAxis.y, (float)rotationAxis.z);
        GlStateManager.translate(0.0D, -0.43D, 0.0D);
        GlStateManager.scale(0.42F, 0.42F, 0.42F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);
        for (int[] face : FACES) {
            for (int j = 0; j < 3; j++) {
                Vec3d v = VERTICES.get(face[j * 2]);
                float u = 0.0F, vv = 0.0F;
                int uv = face[j * 2 + 1];
                if (uv >= 0 && uv < UVS.size()) { u = UVS.get(uv)[0]; vv = UVS.get(uv)[1]; }
                buffer.pos(v.x, v.y, v.z).tex(u, vv).endVertex();
            }
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private static Vec3d interpolated(Entity entity, float partialTicks) {
        return new Vec3d(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityGrappleChain entity) { return TEXTURE; }
}
