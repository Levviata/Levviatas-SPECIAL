package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** TF2-style loaded-ammo counter for the two Sedna flame weapons. */
public final class TF2AmmoOverlay {
    private static final ResourceLocation BADGE = new ResourceLocation("lspecial", "textures/gui/gui_badge.png");
    private static final int TEXTURE_WIDTH = 512;
    private static final int TEXTURE_HEIGHT = 192;
    private static final int BADGE_TEXTURE_WIDTH = 1044;
    private static final int BADGE_TEXTURE_HEIGHT = 604;

    private Font tf2Font;
    private int lastAmmo = Integer.MIN_VALUE;
    private ResourceLocation ammoTexture;

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null) return;
        ItemStack held = player.getHeldItemMainhand();
        if (!isFlameWeapon(held)) return;

        int ammo = getLoadedAmmo(held, player);
        if (ammo < 0) return;
        if (ammo != lastAmmo || ammoTexture == null) rebuildAmmoTexture(mc, ammo);

        int screenWidth = event.getResolution().getScaledWidth();
        int screenHeight = event.getResolution().getScaledHeight();
        double uiScale = Math.min(screenWidth / 960.0D, screenHeight / 540.0D);
        uiScale = Math.max(0.45D, Math.min(1.35D, uiScale));
        int badgeWidth = Math.max(1, Math.min(screenWidth, (int) Math.round(130.0D * uiScale)));
        int badgeHeight = Math.max(1, Math.min(screenHeight, (int) Math.round(75.0D * uiScale)));
        int marginX = (int) Math.round(LSPECIALConfig.tf2AmmoGuiOffsetX * uiScale);
        int marginY = (int) Math.round(LSPECIALConfig.tf2AmmoGuiOffsetY * uiScale);
        int left = clamp(screenWidth - badgeWidth - marginX, 0, Math.max(0, screenWidth - badgeWidth));
        int top = clamp(screenHeight - badgeHeight - marginY, 0, Math.max(0, screenHeight - badgeHeight));
        int textWidth = Math.min(badgeWidth - 8, (int) Math.round(84.0D * uiScale));
        int textHeight = Math.min(badgeHeight - 8, (int) Math.round(42.0D * uiScale));
        int textX = clamp(left + badgeWidth - textWidth - (int) Math.round(8.0D * uiScale), 0,
                Math.max(0, screenWidth - textWidth));
        int textY = clamp(top + badgeHeight - textHeight - (int) Math.round(7.0D * uiScale), 0,
                Math.max(0, screenHeight - textHeight));

        mc.getTextureManager().bindTexture(BADGE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.80F);
        Gui.drawModalRectWithCustomSizedTexture(left, top, 0, 0, badgeWidth, badgeHeight,
                BADGE_TEXTURE_WIDTH, BADGE_TEXTURE_HEIGHT);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (ammoTexture != null && textWidth > 0 && textHeight > 0) {
            mc.getTextureManager().bindTexture(ammoTexture);
            Gui.drawModalRectWithCustomSizedTexture(textX, textY, 0, 0,
                    textWidth, textHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        GlStateManager.disableBlend();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int getLoadedAmmo(ItemStack stack, EntityPlayer player) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return -1;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            GunConfig config;
            try {
                config = gun.getConfig(stack, configIndex);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (config == null) continue;
            Receiver[] receivers = config.getReceivers(stack);
            if (receivers == null) continue;
            for (Receiver receiver : receivers) {
                if (receiver == null) continue;
                IMagazine<?> magazine = receiver.getMagazine(stack);
                if (magazine == null) continue;
                try {
                    return Math.max(0, magazine.getAmount(stack, player == null ? null : player.inventory));
                } catch (RuntimeException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static boolean isFlameWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return false;
        String id = stack.getItem().getRegistryName().toString();
        return "hbm:gun_flamer".equals(id) || "hbm:gun_flamer_topaz".equals(id);
    }

    private void rebuildAmmoTexture(Minecraft mc, int ammo) {
        lastAmmo = ammo;
        BufferedImage image = new BufferedImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = loadFont();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.BOLD, 34);
        font = font.deriveFont(Font.BOLD, 132.0F);
        graphics.setFont(font);
        String value = Integer.toString(ammo);
        int baseline = 142;
        graphics.setColor(new Color(0, 0, 0, 220));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) graphics.drawString(value, 20 + dx * 2, baseline + dy * 2);
            }
        }
        graphics.setColor(Color.WHITE);
        graphics.drawString(value, 20, baseline);
        graphics.dispose();

        if (ammoTexture != null) {
            mc.getTextureManager().deleteTexture(ammoTexture);
        }
        DynamicTexture dynamic = new DynamicTexture(image);
        ammoTexture = mc.getTextureManager().getDynamicTextureLocation("lspecial_tf2_ammo", dynamic);
    }

    private Font loadFont() {
        if (tf2Font != null) return tf2Font;
        try (InputStream stream = TF2AmmoOverlay.class.getResourceAsStream("/assets/lspecial/fonts/TF2.ttf")) {
            if (stream == null) return null;
            tf2Font = Font.createFont(Font.TRUETYPE_FONT, stream);
            return tf2Font;
        } catch (Exception ignored) {
            return null;
        }
    }
}
