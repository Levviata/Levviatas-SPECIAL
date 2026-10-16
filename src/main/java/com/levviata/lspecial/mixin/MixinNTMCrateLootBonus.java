package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALNTMLootHandler;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** applies tagged wand-loot bonuses when an NTM crate is opened. */
@Mixin(TileEntityCrateBase.class)
public abstract class MixinNTMCrateLootBonus {
    @Inject(method = "openInventory(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("RETURN"), remap = false)
    private void lspecial$applyLootBonus(EntityPlayer player, CallbackInfo callback) {
        com.levviata.lspecial.LSPECIALMod.LOGGER.info("[LSPECIAL Loot] NTM crate openInventory RETURN mixin fired for player={}", player == null ? "<null>" : player.getName());
        LSPECIALNTMLootHandler.onContainerOpened((TileEntityCrateBase) (Object) this, player);
    }
}
