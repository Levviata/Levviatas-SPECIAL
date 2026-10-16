package com.levviata.lspecial.mixin;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** marks loot-table containers placed by NTM's NBT structure components. */
@Mixin(targets = "com.hbm.world.gen.nbt.NBTStructure$Component", remap = false)
public abstract class MixinNTMStructureComponent {
    @Inject(method = "addComponentParts(Lnet/minecraft/world/World;Ljava/util/Random;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)Z", at = @At("RETURN"))
    private void lspecial$tagGeneratedLootContainers(World world, java.util.Random random, StructureBoundingBox bounds,
                                                       CallbackInfoReturnable<Boolean> callback) {
        if (world == null || world.isRemote || bounds == null || !Boolean.TRUE.equals(callback.getReturnValue())) return;
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            for (int y = bounds.minY; y <= bounds.maxY; y++) {
                for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                    TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
                    if (!(tile instanceof TileEntityLockableLoot)
                            && !tile.getClass().getName().equals("com.hbm.tileentity.machine.storage.TileEntityCrateBase")) continue;
                    NBTTagCompound serialized = tile.writeToNBT(new NBTTagCompound());
                    if (!serialized.hasKey("LootTable", 8)) continue;
                    NBTTagCompound data = tile.getTileData();
                    data.setBoolean("LSPECIALContainer", true);
                    data.setString("LSPECIALootPool", serialized.getString("LootTable"));
                    tile.markDirty();
                }
            }
        }
    }
}
