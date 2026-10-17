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
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 48;

    private Font tf2Font;
    private int lastAmmo = Integer.MIN_VALUE;
    private ResourceLocation ammoTexture;
    private int ammoTextureWidth = 0;

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

        int x = event.getResolution().getScaledWidth();
        int y = event.getResolution().getScaledHeight();
        int badgeWidth = 130;
        int badgeHeight = 75;
        int left = x - badgeWidth - 8;
        int top = y - badgeHeight - 8;

        mc.getTextureManager().bindTexture(BADGE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.80F);
        Gui.drawModalRectWithCustomSizedTexture(left, top, 0, 0, badgeWidth, badgeHeight, 261.0F, 151.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (ammoTexture != null) {
            int textWidth = TEXTURE_WIDTH;
            int textHeight = 40;
            mc.getTextureManager().bindTexture(ammoTexture);
            Gui.drawModalRectWithCustomSizedTexture(x - textWidth - 13, y - 51, 0, 0,
                    textWidth, textHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        GlStateManager.disableBlend();
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
        font = font.deriveFont(Font.BOLD, 34.0F);
        graphics.setFont(font);
        String value = Integer.toString(ammo);
        int width = graphics.getFontMetrics().stringWidth(value);
        ammoTextureWidth = Math.min(TEXTURE_WIDTH, width + 8);
        int baseline = 36;
        graphics.setColor(new Color(0, 0, 0, 220));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) graphics.drawString(value, 4 + dx, baseline + dy);
            }
        }
        graphics.setColor(Color.WHITE);
        graphics.drawString(value, 4, baseline);
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
