package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public final class GrappleClientHandler {
    private static int targetId = -1;
    private static float savedYaw;
    private static float savedPitch;
    private static boolean saved;
    private static boolean jumpWasDown;
    private static int cooldownTicks;
    private static int fangRefreshTicks = 6;
    public GrappleClientHandler() { }
    public static boolean isOnCooldown() { return cooldownTicks > 0; }
    public static int getCooldownTicks() { return cooldownTicks; }
    public static int getFangRefreshTicks() { return fangRefreshTicks; }
    public static void resetCooldown() { cooldownTicks = 0; fangRefreshTicks = 0; }
    public static void setTarget(int id) {
        if (id >= 0) { cooldownTicks = 60; fangRefreshTicks = 6; }
        Minecraft mc = Minecraft.getMinecraft();
        if (id >= 0 && mc.player != null && !saved) { savedYaw = mc.player.rotationYaw; savedPitch = mc.player.rotationPitch; saved = true; }
        targetId = id;
        if (id < 0) saved = false;
    }
    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int previousCooldownTicks = cooldownTicks;
        if (cooldownTicks > 0) cooldownTicks--;
        if (previousCooldownTicks > 0 && cooldownTicks == 0) fangRefreshTicks = 0;
        else if (cooldownTicks == 0 && fangRefreshTicks < 6) fangRefreshTicks++;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || targetId < 0) { jumpWasDown = false; return; }
        boolean jumpDown = mc.gameSettings.keyBindJump.isKeyDown();
        if (jumpDown && !jumpWasDown) LSPECIALMod.NETWORK.sendToServer(new GrappleCameraPacket(-2, false));
        jumpWasDown = jumpDown;
        if (!LSPECIALConfig.doubleBarrelGrappleCamera) return;
        Entity target = mc.world.getEntityByID(targetId);
        if (target == null || target.isDead) return;
        double dx = target.posX - mc.player.posX;
        double dy = target.posY + target.height * 0.65D - (mc.player.posY + mc.player.getEyeHeight());
        double dz = target.posZ - mc.player.posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float)-Math.toDegrees(Math.atan2(dy, horizontal));
        mc.player.rotationYaw += MathHelper.wrapDegrees(yaw - mc.player.rotationYaw) * 0.22F;
        mc.player.rotationPitch += MathHelper.wrapDegrees(pitch - mc.player.rotationPitch) * 0.22F;
        mc.player.rotationPitch = MathHelper.clamp(mc.player.rotationPitch, -89.0F, 89.0F);
    }
}
