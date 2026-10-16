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
    @Inject(method = "openInventory(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("RETURN"))
    private void lspecial$applyLootBonus(EntityPlayer player, CallbackInfo callback) {
        LSPECIALNTMLootHandler.onCrateOpened((TileEntityCrateBase) (Object) this, player);
    }
}
