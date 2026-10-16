package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

/** applies the same per-player bonus to NTM's native crate inventory. */
@Mixin(targets = "com.hbm.tileentity.machine.storage.TileEntityCrateBase", remap = false)
public abstract class MixinNTMCrateBase {
    @Shadow public ItemStackHandler inventory;

    @Inject(method = "openInventory(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("RETURN"))
    private void lspecial$applyCrateBonuses(EntityPlayer player, CallbackInfo callback) {
        if (player == null || player.world.isRemote || !(player instanceof EntityPlayerMP) || inventory == null) return;
        TileEntity tile = (TileEntity) (Object) this;
        NBTTagCompound data = tile.getTileData();
        if (!data.getBoolean("LSPECIALContainer")) return;
        String originalPool = data.getString("LSPECIALootPool");
        if (originalPool.isEmpty()) return;

        NBTTagList bonuses = data.getTagList("LSPECIALContainerBonus", 8);
        String playerId = player.getUniqueID().toString();
        for (int i = 0; i < bonuses.tagCount(); i++) if (playerId.equals(bonuses.getStringTagAt(i))) return;

        int perception = lspecial$getScore(player, "Perception");
        int luck = lspecial$getScore(player, "Luck");
        boolean duplicateEligible = LSPECIALConfig.perception && LSPECIALConfig.perceptionContainerLootBonus
                && perception >= LSPECIALConfig.perceptionContainerLootLevel;
        boolean injectionEligible = LSPECIALConfig.perception && LSPECIALConfig.luck
                && LSPECIALConfig.luckPerceptionContainerLootInjection
                && perception >= LSPECIALConfig.luckPerceptionContainerLootLevel
                && luck >= LSPECIALConfig.luckPerceptionContainerLootLevel
                && !LSPECIALConfig.luckPerceptionContainerLootPool.trim().isEmpty();
        if (!duplicateEligible && !injectionEligible) return;
        Random random = player.world.rand;
        if (duplicateEligible) lspecial$rollPool(originalPool, player, random);
        if (injectionEligible) lspecial$rollPool(LSPECIALConfig.luckPerceptionContainerLootPool.trim(), player, random);
        bonuses.appendTag(new NBTTagString(playerId));
        data.setTag("LSPECIALContainerBonus", bonuses);
        tile.markDirty();
    }

    @Unique
    private void lspecial$rollPool(String poolName, EntityPlayer player, Random random) {
        ResourceLocation location;
        try {
            location = new ResourceLocation(poolName);
        } catch (RuntimeException exception) {
            return;
        }

        if (!(player.world instanceof WorldServer)) {
            return;
        }

        LootTable table = player.world.getLootTableManager().getLootTableFromLocation(location);
        List<ItemStack> generated;

        WorldServer world = (WorldServer) player.world;
        try {
            generated = table.generateLootForPools(random, new LootContext.Builder(world).withPlayer(player).build());
        }
        catch (RuntimeException exception) {
            return;
        }
        if (generated == null) {
            return;
        }
        for (ItemStack result : generated) {
            if (result == null || result.isEmpty()) {
                continue;
            }
            ItemStack remaining = result.copy();
            for (int slot = 0; slot < inventory.getSlots() && !remaining.isEmpty(); slot++) {
                ItemStack current = inventory.getStackInSlot(slot);
                if (current.isEmpty()) {
                    int amount = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    inventory.setStackInSlot(slot, remaining.splitStack(amount));
                } else if (ItemStack.areItemsEqual(current, remaining) && ItemStack.areItemStackTagsEqual(current, remaining)
                        && current.getCount() < current.getMaxStackSize()) {
                    int amount = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
                    current.grow(amount);
                    remaining.shrink(amount);
                    inventory.setStackInSlot(slot, current);
                }
            }
        }
    }

    @Unique
    private static int lspecial$getScore(EntityPlayer player, String objectiveName) {
        Scoreboard scoreboard = player.world.getScoreboard();
        ScoreObjective objective = scoreboard.getObjective(objectiveName);
        if (objective == null || !scoreboard.entityHasObjective(player.getName(), objective)) return 0;
        return scoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
    }
}
