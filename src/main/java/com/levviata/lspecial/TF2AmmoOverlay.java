package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
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

/** TF2-style loaded-ammo counter for configured NTM weapons. */
public final class TF2AmmoOverlay {
    private static final ResourceLocation BADGE = new ResourceLocation("lspecial", "textures/gui/gui_badge.png");
    private static final int TEXTURE_WIDTH = 1024;
    private static final int TEXTURE_HEIGHT = 384;
    private static final int BADGE_TEXTURE_WIDTH = 2088;
    private static final int BADGE_TEXTURE_HEIGHT = 1208;

    private Font tf2Font;
    private int lastAmmo = Integer.MIN_VALUE;
    private int lastShadowOffsetX = Integer.MIN_VALUE;
    private int lastShadowOffsetY = Integer.MIN_VALUE;
    private ResourceLocation ammoTexture;
    private static int lspecial$renderEvents;
    private static boolean lspecial$loggedOverlayReady;
    private static boolean lspecial$loggedWeapon;

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        // HOTBAR is a reliably emitted post-overlay element in Forge 1.12.2; ALL is not a dependable post hook here.
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        lspecial$renderEvents++;
        if (!lspecial$loggedOverlayReady) {
            lspecial$loggedOverlayReady = true;
            LSPECIALMod.logWarn("[TF2AmmoGUI] render hook active: event={}, side=client", event.getType());
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null) return;
        ItemStack held = player.getHeldItemMainhand();
        if (!isConfiguredWeapon(held)) {
            if (held != null && !held.isEmpty() && lspecial$renderEvents % 200 == 1) {
                LSPECIALMod.logWarn("[TF2AmmoGUI] held item not allowlisted: item={}, configuredList={}",
                        held.getItem().getRegistryName(), LSPECIALConfig.tf2AmmoGuiWeaponList);
            }
            return;
        }
        if (!lspecial$loggedWeapon) {
            lspecial$loggedWeapon = true;
            LSPECIALMod.logWarn("[TF2AmmoGUI] allowlisted weapon detected: item={}", held.getItem().getRegistryName());
        }

        int ammo = getLoadedAmmo(held, player);
        if (ammo < 0) {
            if (lspecial$renderEvents % 200 == 1) LSPECIALMod.logWarn("[TF2AmmoGUI] ammo lookup failed for item={} (not an ItemGunBaseNT or no receiver magazine)", held.getItem().getRegistryName());
            return;
        }
        if (ammo != lastAmmo || ammoTexture == null
                || lastShadowOffsetX != LSPECIALConfig.tf2AmmoGuiShadowOffsetX
                || lastShadowOffsetY != LSPECIALConfig.tf2AmmoGuiShadowOffsetY) rebuildAmmoTexture(mc, ammo);

        ScaledResolution resolution = new ScaledResolution(mc);
        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        double requestedScale = Math.max(0.10D, LSPECIALConfig.tf2AmmoGuiScale);
        double fitScale = Math.min(1.0D, Math.min(screenWidth / 130.0D, screenHeight / 75.0D));
        double uiScale = Math.max(0.10D, Math.min(requestedScale, fitScale));
        int badgeWidth = Math.max(1, Math.min(screenWidth, (int) Math.round(130.0D * uiScale)));
        int badgeHeight = Math.max(1, Math.min(screenHeight, (int) Math.round(75.0D * uiScale)));
        int left;
        int top;
        String position = LSPECIALConfig.tf2AmmoGuiPosition == null ? "BOTTOM_RIGHT"
                : LSPECIALConfig.tf2AmmoGuiPosition.trim().toUpperCase(java.util.Locale.ROOT);
        switch (position) {
            case "BOTTOM_CENTER":
                left = (screenWidth - badgeWidth) / 2 + LSPECIALConfig.tf2AmmoGuiOffsetX;
                top = screenHeight - badgeHeight - LSPECIALConfig.tf2AmmoGuiOffsetY;
                break;
            case "BOTTOM_LEFT":
                left = LSPECIALConfig.tf2AmmoGuiOffsetX;
                top = screenHeight - badgeHeight - LSPECIALConfig.tf2AmmoGuiOffsetY;
                break;
            case "CENTER_LEFT":
                left = LSPECIALConfig.tf2AmmoGuiOffsetX;
                top = (screenHeight - badgeHeight) / 2 + LSPECIALConfig.tf2AmmoGuiOffsetY;
                break;
            case "CENTER_RIGHT":
                left = screenWidth - badgeWidth - LSPECIALConfig.tf2AmmoGuiOffsetX;
                top = (screenHeight - badgeHeight) / 2 + LSPECIALConfig.tf2AmmoGuiOffsetY;
                break;
            case "TOP_LEFT":
                left = LSPECIALConfig.tf2AmmoGuiOffsetX;
                top = LSPECIALConfig.tf2AmmoGuiOffsetY;
                break;
            case "TOP_CENTER":
                left = (screenWidth - badgeWidth) / 2 + LSPECIALConfig.tf2AmmoGuiOffsetX;
                top = LSPECIALConfig.tf2AmmoGuiOffsetY;
                break;
            case "TOP_RIGHT":
                left = screenWidth - badgeWidth - LSPECIALConfig.tf2AmmoGuiOffsetX;
                top = LSPECIALConfig.tf2AmmoGuiOffsetY;
                break;
            default:
                left = screenWidth - badgeWidth - LSPECIALConfig.tf2AmmoGuiOffsetX;
                top = screenHeight - badgeHeight - LSPECIALConfig.tf2AmmoGuiOffsetY;
                break;
        }
        left = clamp(left, 0, Math.max(0, screenWidth - badgeWidth));
        top = clamp(top, 0, Math.max(0, screenHeight - badgeHeight));
        int textWidth = Math.max(1, Math.min(Math.max(1, badgeWidth - 4), (int) Math.round(84.0D * uiScale)));
        int textHeight = Math.max(1, Math.min(Math.max(1, badgeHeight - 4), (int) Math.round(42.0D * uiScale)));
        int textX = clamp(left + badgeWidth - textWidth - (int) Math.round(8.0D * uiScale), left,
                Math.max(left, left + badgeWidth - textWidth));
        int textY = clamp(top + badgeHeight - textHeight - (int) Math.round(7.0D * uiScale), top,
                Math.max(top, top + badgeHeight - textHeight));

        int background = (LSPECIALConfig.tf2AmmoGuiBackgroundAlpha << 24)
                | (LSPECIALConfig.tf2AmmoGuiBackgroundRed << 16)
                | (LSPECIALConfig.tf2AmmoGuiBackgroundGreen << 8)
                | LSPECIALConfig.tf2AmmoGuiBackgroundBlue;
        mc.getTextureManager().bindTexture(BADGE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.80F);
        Gui.drawModalRectWithCustomSizedTexture(left, top, 0, 0, badgeWidth, badgeHeight,
                BADGE_TEXTURE_WIDTH, BADGE_TEXTURE_HEIGHT);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (LSPECIALConfig.tf2AmmoGuiBackgroundAlpha > 0) {
            Gui.drawRect(Math.max(0, textX - 2), Math.max(0, textY - 2),
                    Math.min(screenWidth, textX + textWidth + 2),
                    Math.min(screenHeight, textY + textHeight + 2), background);
        }
        if (ammoTexture != null && textWidth > 0 && textHeight > 0) {
            mc.getTextureManager().bindTexture(ammoTexture);
            Gui.drawModalRectWithCustomSizedTexture(textX, textY, 0, 0,
                    textWidth, textHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        GlStateManager.disableBlend();
        if (lspecial$renderEvents % 200 == 1) {
            LSPECIALMod.logWarn("[TF2AmmoGUI] rendered: ammo={}, screen={}x{}, badge={}x{} at {},{} scale={} preset={}",
                    ammo, screenWidth, screenHeight, badgeWidth, badgeHeight, left, top, uiScale, position);
        }
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

    private static boolean isConfiguredWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return false;
        String id = stack.getItem().getRegistryName().toString();
        String list = LSPECIALConfig.tf2AmmoGuiWeaponList;
        if (list == null || list.trim().isEmpty()) return false;
        for (String allowed : list.split(",")) {
            if (id.equals(allowed.trim())) return true;
        }
        return false;
    }

    private void rebuildAmmoTexture(Minecraft mc, int ammo) {
        lastAmmo = ammo;
        lastShadowOffsetX = LSPECIALConfig.tf2AmmoGuiShadowOffsetX;
        lastShadowOffsetY = LSPECIALConfig.tf2AmmoGuiShadowOffsetY;
        BufferedImage image = new BufferedImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = loadFont();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.BOLD, 34);
        font = font.deriveFont(Font.BOLD, 264.0F);
        graphics.setFont(font);
        String value = Integer.toString(ammo);
        int baseline = 284;
        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.drawString(value, 40 + LSPECIALConfig.tf2AmmoGuiShadowOffsetX * 2,
                baseline + LSPECIALConfig.tf2AmmoGuiShadowOffsetY * 2);
        graphics.setColor(Color.WHITE);
        graphics.drawString(value, 40, baseline);
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
