package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.LSPECIALNTMLootHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies tagged loot bonuses when a vanilla chest is opened.
 */
@Mixin(TileEntityChest.class)
public abstract class MixinVanillaChestLootBonus {
    @Inject(method = "openInventory(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("RETURN"))
    private void lspecial$applyLootBonus(EntityPlayer player, CallbackInfo callback) {
        LSPECIALMod.info("[LSPECIAL Loot] vanilla chest openInventory RETURN mixin fired for player={}", player == null ? "<null>" : player.getName());
        try {
            LSPECIALNTMLootHandler.onContainerOpened((TileEntityChest) (Object) this, player);
        } catch (ClassCastException e) {
            LSPECIALMod.logError("Couldn't cast TileEntityChest when calling onContainerOpened() as it is null");
        }

    }
}
