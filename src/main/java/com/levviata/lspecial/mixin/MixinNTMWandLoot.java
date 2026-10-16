package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALNTMLootHandler;
import com.hbm.blocks.generic.BlockWandLoot;
import com.hbm.handler.WeightedRandomChestContentFrom1710;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** captures the original NTM pool when a wand-loot block replaces itself with its target container. */
@Mixin(targets = "com.hbm.blocks.generic.BlockWandLoot$TileEntityWandLoot", remap = false)
public abstract class MixinNTMWandLoot {
    @Shadow(remap = false) private String poolName;
    @Shadow(remap = false) private int minItems;
    @Shadow(remap = false) private int maxItems;

    @Inject(method = "applyLoot(Lnet/minecraft/world/World;Lnet/minecraft/tileentity/TileEntity;[Lcom/hbm/handler/WeightedRandomChestContentFrom1710;)V", at = @At("HEAD"), remap = false)
    private void lspecial$tagWandLootContainer(World world, TileEntity tile, WeightedRandomChestContentFrom1710[] pool, CallbackInfo callback) {
        // The target can be a vanilla chest as well as an NTM crate or other loot container.
        // applyLoot is called specifically while a wand-loot block populates its target.
        if (tile != null) {
            LSPECIALNTMLootHandler.tagContainer(tile, poolName, minItems, maxItems);
        }
    }
}
