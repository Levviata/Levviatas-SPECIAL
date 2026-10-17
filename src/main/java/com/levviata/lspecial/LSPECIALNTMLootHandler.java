package com.levviata.lspecial;

import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandom;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.Arrays;
import java.util.Random;

/** applies the NTM wand-loot pool bonuses to crates tagged during wand-loot replacement. */
public final class LSPECIALNTMLootHandler {
    public static final String CONTAINER_TAG = "LSPECIALContainer";
    public static final String POOL_TAG = "LSPECIALootPool";
    public static final String PERCEPTION_BONUS_TAG = "LSPECIALPerceptionBonus";
    public static final String LUCK_PERCEPTION_BONUS_TAG = "LSPECIALLuckPerceptionBonus";
    public static final String MIN_ITEMS_TAG = "LSPECIALLootMinItems";
    public static final String MAX_ITEMS_TAG = "LSPECIALLootMaxItems";

    private LSPECIALNTMLootHandler() {
    }

    /** stores the exact pool and roll range used by the NTM wand-loot block. */
    public static void tagContainer(net.minecraft.tileentity.TileEntity tile, String poolName, int minItems, int maxItems) {
        if (tile == null) {
            LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Cannot tag wand-loot target: tile entity is null (pool={})", poolName);
            return;
        }
        if (tile.getWorld() == null) {
            LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Cannot tag target {}: world is null (pool={})", tile.getClass().getName(), poolName);
            return;
        }
        if (tile.getWorld().isRemote) return;
        if (poolName == null || poolName.trim().isEmpty()) {
            LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Cannot tag target {} at {}: pool name is empty", tile.getClass().getName(), tile.getPos());
            return;
        }
        NBTTagCompound data = tile.getTileData();
        data.setBoolean(CONTAINER_TAG, true);
        data.setString(POOL_TAG, poolName);
        data.setInteger(MIN_ITEMS_TAG, Math.max(0, minItems));
        data.setInteger(MAX_ITEMS_TAG, Math.max(Math.max(0, minItems), maxItems));
        tile.markDirty();
        LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Tagged {} at {} with pool={} rolls={}..{}", tile.getClass().getName(), tile.getPos(), poolName, data.getInteger(MIN_ITEMS_TAG), data.getInteger(MAX_ITEMS_TAG));
    }

    /** applies each eligible perk independently, preserving existing stacks and never overwriting slots. */
    public static void onContainerOpened(TileEntity tile, EntityPlayer player) {
        if (tile == null) {
            LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Container-open handler received a null tile entity");
            return;
        }
        if (player == null || player.world == null) {
            LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Container-open handler received a null player or player world for {} at {}", tile.getClass().getName(), tile.getPos());
            return;
        }
        if (player.world.isRemote || !(player.world instanceof net.minecraft.world.WorldServer)) return;
        NBTTagCompound data = tile.getTileData();
        LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Open callback: player={} tile={} pos={} tagged={} pool={} min={} max={}", player.getName(), tile.getClass().getName(), tile.getPos(), data.getBoolean(CONTAINER_TAG), data.getString(POOL_TAG), data.getInteger(MIN_ITEMS_TAG), data.getInteger(MAX_ITEMS_TAG));
        if (!data.getBoolean(CONTAINER_TAG)) {
            LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Ignoring untagged container at {} ({})", tile.getPos(), tile.getClass().getName());
            return;
        }

        int perception = getScore(player, "Perception");
        int luck = getScore(player, "Luck");
        boolean duplicateEligible = LSPECIALConfig.perception && LSPECIALConfig.perceptionLootDuplication
                && perception >= LSPECIALConfig.perceptionLootDuplicationLevel;
        boolean bonusEligible = LSPECIALConfig.perception && LSPECIALConfig.luck && LSPECIALConfig.luckPerceptionLootBonus
                && perception >= LSPECIALConfig.luckPerceptionLootBonusLevel
                && luck >= LSPECIALConfig.luckPerceptionLootBonusLevel;
        LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Eligibility: player={} Perception={} Luck={} perceptionEnabled={} rerollEnabled={} rerollThreshold={} duplicateEligible={} luckEnabled={} bonusEnabled={} bonusThreshold={} bonusPool={} bonusEligible={}",
                player.getName(), perception, luck, LSPECIALConfig.perception, LSPECIALConfig.perceptionLootDuplication,
                LSPECIALConfig.perceptionLootDuplicationLevel, duplicateEligible, LSPECIALConfig.luck,
                LSPECIALConfig.luckPerceptionLootBonus, LSPECIALConfig.luckPerceptionLootBonusLevel,
                LSPECIALConfig.luckPerceptionLootPool, bonusEligible);
        if (!duplicateEligible && !bonusEligible) {
            LSPECIALMod.LOGGER.info("[LSPECIAL Loot] No loot perk qualifies for player={} at {}", player.getName(), tile.getPos());
            return;
        }

        IItemHandlerModifiable inventory = getInventory(tile);
        if (inventory == null) {
            LSPECIALMod.LOGGER.error("[LSPECIAL Loot] Cannot add bonus: no supported inventory for tile={} at {}", tile.getClass().getName(), tile.getPos());
            return;
        }
        if (inventory.getSlots() == 0) {
            LSPECIALMod.LOGGER.error("[LSPECIAL Loot] Cannot add bonus: inventory has zero slots for tile={} at {}", tile.getClass().getName(), tile.getPos());
            return;
        }
        LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Inventory resolved: slots={} tile={} pos={}", inventory.getSlots(), tile.getClass().getName(), tile.getPos());

        String uuid = player.getUniqueID().toString();
        Random random = player.world.rand;

        // reroll again the current loot pool
        if (duplicateEligible) {
            if (hasPlayer(data, PERCEPTION_BONUS_TAG, uuid)) {
                LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Perception reroll already claimed: player={} pos={}", player.getName(), tile.getPos());
            } else {
                String originalPool = data.getString(POOL_TAG);
                if (isValidPool(originalPool)) {
                    int min = Math.max(0, data.getInteger(MIN_ITEMS_TAG));
                    int max = Math.max(min, data.getInteger(MAX_ITEMS_TAG));
                    int rolls = max > min ? min + random.nextInt(max - min + 1) : min;
                    LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Starting Perception reroll: pool={} rolls={} player={} pos={}", originalPool, rolls, player.getName(), tile.getPos());
                    if (addPoolItems(inventory, originalPool, rolls, random, false)) {
                        recordPlayer(data, PERCEPTION_BONUS_TAG, uuid);
                        LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Perception reroll inserted items; recorded UUID for player={} pos={}", player.getName(), tile.getPos());
                    } else {
                        LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Perception reroll inserted no items; UUID not recorded. pool={} pos={}", originalPool, tile.getPos());
                    }
                } else {
                    LSPECIALMod.LOGGER.error("[LSPECIAL Loot] Invalid original pool '{}' on tagged container at {}", originalPool, tile.getPos());
                }
            }
        }

        // fill to the brim with the og loot pool
        if (bonusEligible) {
            if (hasPlayer(data, LUCK_PERCEPTION_BONUS_TAG, uuid)) {
                LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Luck+Perception bonus already claimed: player={} pos={}", player.getName(), tile.getPos());
            } else if (addPoolItems(inventory, LSPECIALConfig.luckPerceptionLootPool, 1, random, true)) {
                recordPlayer(data, LUCK_PERCEPTION_BONUS_TAG, uuid);
                LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Luck+Perception bonus inserted items; recorded UUID for player={} pos={}", player.getName(), tile.getPos());
            } else {
                LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Luck+Perception bonus inserted no items; UUID not recorded. pool={} pos={}", LSPECIALConfig.luckPerceptionLootPool, tile.getPos());
            }
        }

        tile.markDirty();
        LSPECIALMod.LOGGER.info("[LSPECIAL Loot] Finished open processing: player={} pos={} perceptionClaimed={} luckPerceptionClaimed={}", player.getName(), tile.getPos(), hasPlayer(data, PERCEPTION_BONUS_TAG, uuid), hasPlayer(data, LUCK_PERCEPTION_BONUS_TAG, uuid));
    }

    private static boolean hasPlayer(NBTTagCompound data, String tagName, String uuid) {
        NBTTagList players = data.getTagList(tagName, 8);
        for (int i = 0; i < players.tagCount(); i++) {
            if (uuid.equals(players.getStringTagAt(i))) return true;
        }
        return false;
    }

    private static void recordPlayer(NBTTagCompound data, String tagName, String uuid) {
        NBTTagList players = data.getTagList(tagName, 8);
        players.appendTag(new NBTTagString(uuid));
        data.setTag(tagName, players);
    }

    /** gets a modifiable inventory from either an NTM crate or a vanilla chest. */
    private static IItemHandlerModifiable getInventory(TileEntity tile) {
        if (tile instanceof com.hbm.tileentity.machine.storage.TileEntityCrateBase) {
            return ((com.hbm.tileentity.machine.storage.TileEntityCrateBase) tile).inventory;
        }
        if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            Object handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler instanceof IItemHandlerModifiable) return (IItemHandlerModifiable) handler;
        }
        // Vanilla TileEntityChest implements IInventory but does not expose Forge's item-handler capability.
        if (tile instanceof IInventory) {
            return new VanillaInventoryAdapter((IInventory) tile);
        }
        return null;
    }

    /** bridges vanilla inventories to the item-handler API used by the loot insertion code. */
    private static final class VanillaInventoryAdapter implements IItemHandlerModifiable {
        private final IInventory inventory;

        private VanillaInventoryAdapter(IInventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public int getSlots() {
            return inventory.getSizeInventory();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
            ItemStack stack = inventory.getStackInSlot(slot);
            return stack == null ? ItemStack.EMPTY : stack;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
            if (slot < 0 || slot >= getSlots() || !inventory.isItemValidForSlot(slot, stack)) return stack;

            ItemStack existing = getStackInSlot(slot);
            int limit = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
            if (limit <= 0) return stack;

            if (existing.isEmpty()) {
                int inserted = Math.min(stack.getCount(), limit);
                if (!simulate) {
                    ItemStack result = stack.copy();
                    result.setCount(inserted);
                    inventory.setInventorySlotContents(slot, result);
                    inventory.markDirty();
                }
                if (inserted >= stack.getCount()) return ItemStack.EMPTY;
                ItemStack remainder = stack.copy();
                remainder.shrink(inserted);
                return remainder;
            }

            if (!ItemHandlerHelper.canItemStacksStack(existing, stack)) return stack;
            int space = limit - existing.getCount();
            if (space <= 0) return stack;
            int inserted = Math.min(space, stack.getCount());
            if (!simulate) {
                ItemStack result = existing.copy();
                result.grow(inserted);
                inventory.setInventorySlotContents(slot, result);
                inventory.markDirty();
            }
            if (inserted >= stack.getCount()) return ItemStack.EMPTY;
            ItemStack remainder = stack.copy();
            remainder.shrink(inserted);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= getSlots() || amount <= 0) return ItemStack.EMPTY;
            ItemStack existing = getStackInSlot(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            int extracted = Math.min(amount, existing.getCount());
            ItemStack result = existing.copy();
            result.setCount(extracted);
            if (!simulate) {
                if (extracted >= existing.getCount()) {
                    inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
                } else {
                    ItemStack remainder = existing.copy();
                    remainder.shrink(extracted);
                    inventory.setInventorySlotContents(slot, remainder);
                }
                inventory.markDirty();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < 0 || slot >= getSlots() ? 0 : inventory.getInventoryStackLimit();
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException("slot " + slot);
            if (stack == null || stack.isEmpty()) {
                inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
            } else {
                ItemStack result = stack.copy();
                result.setCount(Math.min(result.getCount(), getSlotLimit(slot)));
                inventory.setInventorySlotContents(slot, result);
            }
            inventory.markDirty();
        }
    }

    /** reads the player's stat score without creating missing scoreboard objectives. */
    private static int getScore(EntityPlayer player, String name) {
        Scoreboard scoreboard = player.world.getScoreboard();
        ScoreObjective objective = scoreboard.getObjective(name);
        if (objective == null) {
            LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Scoreboard objective '{}' does not exist; treating {}'s score as 0", name, player.getName());
            return 0;
        }
        if (!scoreboard.entityHasObjective(player.getName(), objective)) {
            LSPECIALMod.LOGGER.warn("[LSPECIAL Loot] Player {} has no scoreboard score for '{}'; treating it as 0", player.getName(), name);
            return 0;
        }
        return scoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
    }

    /** avoids ItemPool.getPool's fallback pool when a configured pool name is invalid. */
    private static boolean isValidPool(String name) {
        if (name == null || name.trim().isEmpty() || ItemPool.pools == null) return false;
        ItemPool pool = ItemPool.pools.get(name);
        return pool != null && pool.pool != null && pool.pool.length > 0;
    }

    /** generates NTM weighted items and inserts them safely without replacing existing inventory slots. */
    // i didnt intend to make the AI fill up the chest fully for luck plus perception bonus but its staying
    private static boolean addPoolItems(IItemHandlerModifiable inventory, String poolName, int rolls, Random random, boolean fillUntilFull) {
        if (!isValidPool(poolName) || inventory == null || rolls <= 0) return false;
        WeightedRandomChestContentFrom1710[] pool = ItemPool.pools.get(poolName).pool;
        int attempts = 0;
        int remainingRolls = rolls;
        boolean insertedAnyTotal = false;
        while ((fillUntilFull || remainingRolls-- > 0) && attempts++ < 4096) {
            if (isFull(inventory)) return insertedAnyTotal;
            WeightedRandomChestContentFrom1710 entry = WeightedRandom.getRandomItem(random, Arrays.asList(pool));
            if (entry == null) return insertedAnyTotal;
            ItemStack[] stacks = entry.generateChestContent(random, inventory);
            boolean insertedAny = false;
            if (stacks != null) {
                for (ItemStack stack : stacks) {
                    if (stack == null || stack.isEmpty()) continue;
                    ItemStack remainder = ItemHandlerHelper.insertItem(inventory, stack.copy(), false);
                    if (remainder.getCount() < stack.getCount()) insertedAny = true;
                }
            }
            if (insertedAny) insertedAnyTotal = true;
            if (!insertedAny && !fillUntilFull) continue;
            if (!insertedAny) return insertedAnyTotal;
        }
        return insertedAnyTotal;
    }

    private static boolean isFull(IItemHandlerModifiable inventory) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxStackSize(), inventory.getSlotLimit(i))) return false;
        }
        return true;
    }
}
