package com.levviata.lspecial.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** marks discounted villager trade-cost counts in the inventory-style item overlay. */
@Mixin(RenderItem.class)
public abstract class MixinRenderItemOverlay {
    @Unique
    private ItemStack lspecial$currentOverlayStack;

    @Inject(method = "renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), remap = false)
    private void lspecial$captureOverlayStack(FontRenderer fontRenderer, TextureManager textureManager, ItemStack stack, int x, int y, String text, CallbackInfo callback) {
        lspecial$currentOverlayStack = stack;
        if (text == null && lspecial$isDiscounted(stack) && stack.getCount() == 1) {
            String count = "1*";
            fontRenderer.drawStringWithShadow(count,
                    x + 19 - 2 - fontRenderer.getStringWidth(count), y + 9, 16777215);
        }
    }

    @ModifyArg(method = "renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I", ordinal = 0), index = 0, remap = false)
    private String lspecial$markDiscountedCount(String text) {
        ItemStack stack = lspecial$currentOverlayStack;
        return lspecial$isDiscounted(stack) && text != null && text.matches("\\d+") ? text + "*" : text;
    }

    @Inject(method = "renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"), remap = false)
    private void lspecial$clearOverlayStack(FontRenderer fontRenderer, TextureManager textureManager,
            ItemStack stack, int x, int y, String text, CallbackInfo callback) {
        lspecial$currentOverlayStack = null;
    }

    @Unique
    private static boolean lspecial$isDiscounted(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) return false;
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey("LSPECIALTradeDiscount");
    }
}
