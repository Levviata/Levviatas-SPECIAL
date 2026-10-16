package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class GrappleClientHandler {
    private static int targetId = -1;
    private static float savedYaw;
    private static float savedPitch;
    private static boolean saved;
    private static boolean jumpWasDown;
    private static int cooldownTicks;
    private static int fangRefreshTicks = 6;
    private static boolean fangClinkPending;
    private static GrappleCycleSound cycleSound;

    public GrappleClientHandler() { }
    public static int getCooldownTicks() { return cooldownTicks; }
    public static int getFangRefreshTicks() { return fangRefreshTicks; }

    public static void resetCooldown() {
        cooldownTicks = 0;
        fangRefreshTicks = 0;
        fangClinkPending = true;
        playOneShot(LSPECIALSounds.GRAPPLE_RESET, 1.0F, 1.0F);
    }

    public static void setTarget(int id) {
        int previousTargetId = targetId;
        if (id >= 0) {
            cooldownTicks = 60;
            fangRefreshTicks = 6;
            fangClinkPending = false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (id >= 0 && mc.player != null && !saved) {
            savedYaw = mc.player.rotationYaw;
            savedPitch = mc.player.rotationPitch;
            saved = true;
        }
        targetId = id;
        if (id >= 0 && previousTargetId < 0) {
            playOneShot(LSPECIALSounds.GRAPPLE_START, 0.9F, 1.0F);
            startCycleSound(mc);
        } else if (id < 0 && previousTargetId >= 0) {
            stopCycleSound(mc);
            playOneShot(LSPECIALSounds.GRAPPLE_OUT, 0.9F, 1.0F);
        }
        if (id < 0) saved = false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int previousCooldownTicks = cooldownTicks;
        if (cooldownTicks > 0) cooldownTicks--;
        if (previousCooldownTicks > 0 && cooldownTicks == 0) {
            fangRefreshTicks = 0;
            fangClinkPending = true;
            playOneShot(LSPECIALSounds.GRAPPLE_RESET, 1.0F, 1.0F);
        } else if (cooldownTicks == 0 && fangRefreshTicks < 6) {
            fangRefreshTicks++;
            if (fangRefreshTicks >= 6 && fangClinkPending) {
                fangClinkPending = false;
                playOneShot(LSPECIALSounds.GRAPPLE_RESETCLINK, 0.9F, 1.0F);
            }
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            stopCycleSound(mc);
            jumpWasDown = false;
            return;
        }
        if (targetId < 0) { jumpWasDown = false; return; }
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

    private static void playOneShot(net.minecraft.util.SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getMinecraft();
        if (sound != null && mc.player != null) mc.player.playSound(sound, volume, pitch);
    }

    private static void startCycleSound(Minecraft mc) {
        stopCycleSound(mc);
        if (mc.player == null || LSPECIALSounds.GRAPPLE_CYCLE == null) return;
        cycleSound = new GrappleCycleSound(mc.player);
        mc.getSoundHandler().playSound(cycleSound);
    }

    private static void stopCycleSound(Minecraft mc) {
        if (cycleSound != null) {
            cycleSound.setDonePlaying(true);
            mc.getSoundHandler().stopSound(cycleSound);
            cycleSound = null;
        }
    }

    private static final class GrappleCycleSound extends MovingSound {
        private final EntityPlayerSP player;

        private GrappleCycleSound(EntityPlayerSP player) {
            super(LSPECIALSounds.GRAPPLE_CYCLE, SoundCategory.PLAYERS);
            this.player = player;
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 0.7F;
            this.pitch = 1.0F;
            this.attenuationType = ISound.AttenuationType.NONE;
            update();
        }

        @Override
        public void update() {
            if (player == null || player.isDead) {
                donePlaying = true;
                return;
            }
            xPosF = (float) player.posX;
            yPosF = (float) player.posY;
            zPosF = (float) player.posZ;
        }

        public void setDonePlaying(boolean b) {
            this.donePlaying = b;
        }
     }
}
