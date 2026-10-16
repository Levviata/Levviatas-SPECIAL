package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

/** applies per-player loot bonuses only to containers marked during NTM structure generation. */
@Mixin(TileEntityLockableLoot.class)
public abstract class MixinLootContainer {
    @Shadow public abstract int getSizeInventory();
    @Shadow public abstract ItemStack getStackInSlot(int index);
    @Shadow public abstract void setInventorySlotContents(int index, ItemStack stack);

    @Inject(method = "openInventory(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("RETURN"))
    private void lspecial$applyContainerBonuses(EntityPlayer player, CallbackInfo callback) {
        if (player == null || player.world.isRemote || !(player instanceof EntityPlayerMP)) return;
        TileEntityLockableLoot container = (TileEntityLockableLoot) (Object) this;
        NBTTagCompound data = container.getTileData();
        if (!data.getBoolean("LSPECIALContainer")) return;

        String poolName = data.getString("LSPECIALootPool");
        if (poolName.isEmpty()) return;
        NBTTagList bonuses = data.getTagList("LSPECIALContainerBonus", 8);
        String playerId = player.getUniqueID().toString();
        for (int i = 0; i < bonuses.tagCount(); i++) {
            if (playerId.equals(bonuses.getStringTagAt(i))) return;
        }

        int perception = lspecial$getScore(player, "Perception");
        int luck = lspecial$getScore(player, "Luck");
        boolean duplicateEligible = LSPECIALConfig.perception && LSPECIALConfig.perceptionContainerLootBonus
                && perception >= LSPECIALConfig.perceptionContainerLootLevel;
        boolean injectionEligible = LSPECIALConfig.luck && LSPECIALConfig.perception
                && LSPECIALConfig.luckPerceptionContainerLootInjection
                && luck >= LSPECIALConfig.luckPerceptionContainerLootLevel
                && perception >= LSPECIALConfig.luckPerceptionContainerLootLevel
                && !LSPECIALConfig.luckPerceptionContainerLootPool.trim().isEmpty();
        if (!duplicateEligible && !injectionEligible) return;
        Random random = player.world.rand;

        if (duplicateEligible) lspecial$rollPool(container, poolName, player, random);
        if (injectionEligible) lspecial$rollPool(container, LSPECIALConfig.luckPerceptionContainerLootPool.trim(), player, random);

        bonuses.appendTag(new NBTTagString(playerId));
        data.setTag("LSPECIALContainerBonus", bonuses);
        container.markDirty();
    }

    @Unique
    private boolean lspecial$rollPool(TileEntityLockableLoot container, String poolName, EntityPlayer player, Random random) {
        ResourceLocation location;
        try {
            location = new ResourceLocation(poolName);
        } catch (RuntimeException exception) {
            return false;
        }

        if (!(player.world instanceof WorldServer)) {
            return false;
        }

        LootTable table = player.world.getLootTableManager().getLootTableFromLocation(location);
        if (table == null) {
            return false;
        }
        List<ItemStack> generated;

        WorldServer world = (WorldServer) player.world;
        try {
            generated = table.generateLootForPools(random, new LootContext.Builder(world).withPlayer(player).build());
        } catch (RuntimeException exception) {
            return false;
        }
        if (generated == null || generated.isEmpty()) return false;
        boolean changed = false;
        for (ItemStack generatedStack : generated) {
            if (generatedStack == null || generatedStack.isEmpty()) continue;
            ItemStack remaining = generatedStack.copy();
            for (int slot = 0; slot < container.getSizeInventory() && !remaining.isEmpty(); slot++) {
                ItemStack current = container.getStackInSlot(slot);
                if (current == null || current.isEmpty()) {
                    int amount = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    ItemStack inserted = remaining.splitStack(amount);
                    container.setInventorySlotContents(slot, inserted);
                    changed = true;
                } else if (ItemStack.areItemsEqual(current, remaining) && ItemStack.areItemStackTagsEqual(current, remaining)
                        && current.getCount() < current.getMaxStackSize()) {
                    int amount = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
                    current.grow(amount);
                    remaining.shrink(amount);
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Unique
    private static int lspecial$getScore(EntityPlayer player, String objectiveName) {
        Scoreboard scoreboard = player.world.getScoreboard();
        ScoreObjective objective = scoreboard.getObjective(objectiveName);
        if (objective == null || !scoreboard.entityHasObjective(player.getName(), objective)) return 0;
        return scoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
    }
}
