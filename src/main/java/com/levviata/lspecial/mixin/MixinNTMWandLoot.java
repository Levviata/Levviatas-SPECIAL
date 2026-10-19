package com.levviata.lspecial.mixin;

import com.hbm.blocks.generic.BlockWandLoot;
import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.LSPECIALNTMLootHandler;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the original NTM pool when a wand-loot block replaces itself with its target container.
 */
@Mixin(targets = "com.hbm.blocks.generic.BlockWandLoot$TileEntityWandLoot", remap = false)
public abstract class MixinNTMWandLoot {
    @Shadow(remap = false)
    private String poolName;
    @Shadow(remap = false)
    private int minItems;
    @Shadow(remap = false)
    private int maxItems;
    @Shadow(remap = false)
    private Block replaceBlock;
    @Shadow(remap = false)
    private boolean triggerReplace;

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
            LSPECIALMod.logWarn(
                    "[LSPECIAL Loot] Wand-loot replace did not produce a target tile entity at {}: pool={} target={}",
                    source.getPos(), poolName, target == null ? "null" : target.getClass().getName());
            lspecial$queueDeferredTag(world);
            return;
        }

        LSPECIALMod.info(
                "LSPECIAL Loot", "Tagging replaced wand-loot target: target={} pos={} pool={} rolls={}..{}",
                target.getClass().getName(), target.getPos(), poolName, minItems, maxItems);
        LSPECIALNTMLootHandler.tagContainer(target, poolName, minItems, maxItems);
    }

    @Inject(method = "transformTE(Lnet/minecraft/world/World;I)V", at = @At("RETURN"), remap = false)
    private void lspecial$tagAfterStructureTransform(World world, int rotation, CallbackInfo callback) {
        // Structure templates can replace wand-loot blocks without reaching the normal replace hook.
        lspecial$queueDeferredTag(world);
    }

    @Inject(method = "update()V", at = @At("HEAD"), remap = false)
    private void lspecial$queueTagBeforeReplacement(CallbackInfo callback) {
        if (triggerReplace) {
            TileEntity source = (TileEntity) (Object) this;
            lspecial$queueDeferredTag(source.getWorld());
        }
    }

    @Unique
    private void lspecial$queueDeferredTag(World world) {
        TileEntity source = (TileEntity) (Object) this;
        if (world == null || world.isRemote || source.getPos() == null || poolName == null || poolName.isEmpty()) {
            return;
        }

        final BlockPos pos = source.getPos();
        final String expectedPool = poolName;
        final int expectedMin = minItems;
        final int expectedMax = maxItems;
        final Block expectedBlock = replaceBlock;

        com.hbm.util.DelayedTick.nextWorldTickEnd(world, scheduledWorld -> {
            if (scheduledWorld == null || scheduledWorld.isRemote) {
                return;
            }
            if (expectedBlock != null && scheduledWorld.getBlockState(pos).getBlock() != expectedBlock) {
                return;
            }

            TileEntity target = scheduledWorld.getTileEntity(pos);
            if (target == null || target instanceof BlockWandLoot.TileEntityWandLoot) {
                return;
            }

            NBTTagCompound data = target.getTileData();
            if (data.getBoolean(LSPECIALNTMLootHandler.CONTAINER_TAG)
                    && expectedPool.equals(data.getString(LSPECIALNTMLootHandler.POOL_TAG))
                    && data.getInteger(LSPECIALNTMLootHandler.MIN_ITEMS_TAG) == Math.max(0, expectedMin)
                    && data.getInteger(LSPECIALNTMLootHandler.MAX_ITEMS_TAG) == Math.max(Math.max(0, expectedMin), expectedMax)) {
                return;
            }

            LSPECIALMod.info(
                    "LSPECIAL Loot", "Deferred structure tagging: target={} pos={} pool={} rolls={}..{}",
                    target.getClass().getName(), pos, expectedPool, expectedMin, expectedMax);
            LSPECIALNTMLootHandler.tagContainer(target, expectedPool, expectedMin, expectedMax);
        });
    }

    @Inject(method = "update()V", at = @At("TAIL"), remap = false)
    private void lspecial$tagTargetAfterUpdate(CallbackInfo callback) {
        // fallback for structure placement paths where the replacement hook did not retain the tag
        TileEntity source = (TileEntity) (Object) this;
        World world = source.getWorld();
        if (world == null || world.isRemote || source.getPos() == null) {
            return;
        }

        TileEntity target = world.getTileEntity(source.getPos());
        if (target == null || target == source || target instanceof BlockWandLoot.TileEntityWandLoot) {
            return;
        }

        NBTTagCompound data = target.getTileData();
        if (data.getBoolean(LSPECIALNTMLootHandler.CONTAINER_TAG)
                && poolName != null && poolName.equals(data.getString(LSPECIALNTMLootHandler.POOL_TAG))) {
            return;
        }

        LSPECIALMod.logWarn(
                "[LSPECIAL Loot] Update fallback tagging target={} pos={} pool={} rolls={}..{}",
                target.getClass().getName(), target.getPos(), poolName, minItems, maxItems);
        LSPECIALNTMLootHandler.tagContainer(target, poolName, minItems, maxItems);
    }

    @Inject(method = "applyLoot(Lnet/minecraft/world/World;Lnet/minecraft/tileentity/TileEntity;[Lcom/hbm/handler/WeightedRandomChestContentFrom1710;)V", at = @At("HEAD"), remap = false)
    private void lspecial$tagWandLootContainer(World world, TileEntity tile, WeightedRandomChestContentFrom1710[] pool, CallbackInfo callback) {
        // Keep this as a second tagging point for targets resolved by the loot-population path.
        if (tile == null) {
            LSPECIALMod.logWarn("[LSPECIAL Loot] Wand-loot applyLoot called with null target: pool={} rolls={}..{}", poolName, minItems, maxItems);
            return;
        }
        LSPECIALMod.info("LSPECIAL Loot", "Wand-loot applyLoot hook fired: target={} pos={} pool={} rolls={}..{}", tile.getClass().getName(), tile.getPos(), poolName, minItems, maxItems);
        LSPECIALNTMLootHandler.tagContainer(tile, poolName, minItems, maxItems);
    }
}
