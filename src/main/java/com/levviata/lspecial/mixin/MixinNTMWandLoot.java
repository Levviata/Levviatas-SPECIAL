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

    @Inject(method = "replace()V", at = @At("RETURN"), remap = false)
    private void lspecial$tagReplacedContainer(CallbackInfo callback) {
        // replace() installs the real target tile entity before scheduling its loot population.
        TileEntity source = (TileEntity) (Object) this;
        World world = source.getWorld();
        if (world == null || world.isRemote || source.getPos() == null) {
            return;
        }

        TileEntity target = world.getTileEntity(source.getPos());
        if (target == null || target == source || target instanceof BlockWandLoot.TileEntityWandLoot) {
            com.levviata.lspecial.LSPECIALMod.LOGGER.warn(
                    "[LSPECIAL Loot] Wand-loot replace did not produce a target tile entity at {}: pool={} target={}",
                    source.getPos(), poolName, target == null ? "null" : target.getClass().getName());
            return;
        }

        com.levviata.lspecial.LSPECIALMod.LOGGER.info(
                "[LSPECIAL Loot] Tagging replaced wand-loot target: target={} pos={} pool={} rolls={}..{}",
                target.getClass().getName(), target.getPos(), poolName, minItems, maxItems);
        LSPECIALNTMLootHandler.tagContainer(target, poolName, minItems, maxItems);
    }

    @Inject(method = "applyLoot(Lnet/minecraft/world/World;Lnet/minecraft/tileentity/TileEntity;[Lcom/hbm/handler/WeightedRandomChestContentFrom1710;)V", at = @At("HEAD"), remap = false)
    private void lspecial$tagWandLootContainer(World world, TileEntity tile, WeightedRandomChestContentFrom1710[] pool, CallbackInfo callback) {
        // Keep this as a second tagging point for targets resolved by the loot-population path.
        if (tile == null) {
            com.levviata.lspecial.LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Wand-loot applyLoot called with null target: pool={} rolls={}..{}", poolName, minItems, maxItems);
            return;
        }
        com.levviata.lspecial.LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Wand-loot applyLoot hook fired: target={} pos={} pool={} rolls={}..{}", tile.getClass().getName(), tile.getPos(), poolName, minItems, maxItems);
        LSPECIALNTMLootHandler.tagContainer(tile, poolName, minItems, maxItems);
    }
}
