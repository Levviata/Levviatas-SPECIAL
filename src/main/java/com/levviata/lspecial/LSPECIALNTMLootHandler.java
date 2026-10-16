package com.levviata.lspecial;

import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandom;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.spi.LoggerRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.levviata.lspecial.LSPECIALMod.LOGGER;

/** applies the NTM wand-loot pool bonuses to crates tagged during wand-loot replacement. */
public final class LSPECIALNTMLootHandler {
    public static final String CONTAINER_TAG = "LSPECIALContainer";
    public static final String POOL_TAG = "LSPECIALootPool";
    public static final String BONUS_TAG = "LSPECIALContainerBonus";
    public static final String MIN_ITEMS_TAG = "LSPECIALLootMinItems";
    public static final String MAX_ITEMS_TAG = "LSPECIALLootMaxItems";

    private LSPECIALNTMLootHandler() {
    }

    /** stores the exact pool and roll range used by the NTM wand-loot block. */
    public static void tagContainer(net.minecraft.tileentity.TileEntity tile, String poolName, int minItems, int maxItems) {
        if (tile == null || tile.getWorld() == null || tile.getWorld().isRemote || poolName == null || poolName.trim().isEmpty()) return;
        NBTTagCompound data = tile.getTileData();
        data.setBoolean(CONTAINER_TAG, true);
        data.setString(POOL_TAG, poolName);
        data.setInteger(MIN_ITEMS_TAG, Math.max(0, minItems));
        data.setInteger(MAX_ITEMS_TAG, Math.max(Math.max(0, minItems), maxItems));
        tile.markDirty();
    }

    /** applies each eligible player's bonus once, preserving existing stacks and never overwriting slots. */
    public static void onCrateOpened(TileEntityCrateBase crate, EntityPlayer player) {
        LOGGER.info("crate opened");
        if (crate == null || player == null || player.world == null || player.world.isRemote || !(player.world instanceof net.minecraft.world.WorldServer)) return;
        NBTTagCompound data = crate.getTileData();
        if (!data.getBoolean(CONTAINER_TAG)) return;
        LOGGER.info("we passed the conditions");

        int perception = getScore(player, "Perception");
        int luck = getScore(player, "Luck");
        boolean duplicate = LSPECIALConfig.perception && LSPECIALConfig.perceptionLootDuplication
                && perception >= LSPECIALConfig.perceptionLootDuplicationLevel;
        boolean bonus = LSPECIALConfig.perception && LSPECIALConfig.luck && LSPECIALConfig.luckPerceptionLootBonus
                && perception >= LSPECIALConfig.luckPerceptionLootBonusLevel && luck >= LSPECIALConfig.luckPerceptionLootBonusLevel;
        if (!duplicate && !bonus) return;

        String uuid = player.getUniqueID().toString();
        NBTTagList players = data.getTagList(BONUS_TAG, 8);
        for (int i = 0; i < players.tagCount(); i++) {
            if (uuid.equals(players.getStringTagAt(i))) return;
        }

        ItemStackHandler inventory = crate.inventory;
        if (inventory == null || inventory.getSlots() == 0) return;
        Random random = player.world.rand;
        if (duplicate) {
            String originalPool = data.getString(POOL_TAG);
            if (isValidPool(originalPool)) {
                int min = Math.max(0, data.getInteger(MIN_ITEMS_TAG));
                int max = Math.max(min, data.getInteger(MAX_ITEMS_TAG));
                int rolls = max > min ? min + random.nextInt(max - min + 1) : min;
                addPoolItems(inventory, originalPool, rolls, random, false);
            }
        }
        if (bonus && isValidPool(LSPECIALConfig.luckPerceptionLootPool)) {
            addPoolItems(inventory, LSPECIALConfig.luckPerceptionLootPool, 1, random, true);
        }

        players.appendTag(new NBTTagString(uuid));
        data.setTag(BONUS_TAG, players);
        crate.markDirty();
    }

    /** reads the player's stat score without creating missing scoreboard objectives. */
    private static int getScore(EntityPlayer player, String name) {
        Scoreboard scoreboard = player.world.getScoreboard();
        ScoreObjective objective = scoreboard.getObjective(name);
        if (objective == null || !scoreboard.entityHasObjective(player.getName(), objective)) return 0;
        return scoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
    }

    /** avoids ItemPool.getPool's fallback pool when a configured pool name is invalid. */
    private static boolean isValidPool(String name) {
        if (name == null || name.trim().isEmpty() || ItemPool.pools == null) return false;
        ItemPool pool = ItemPool.pools.get(name);
        return pool != null && pool.pool != null && pool.pool.length > 0;
    }

    /** generates NTM weighted items and inserts them safely without replacing existing inventory slots. */
    private static void addPoolItems(ItemStackHandler inventory, String poolName, int rolls, Random random, boolean fillUntilFull) {
        if (!isValidPool(poolName) || inventory == null || rolls <= 0) return;
        WeightedRandomChestContentFrom1710[] pool = ItemPool.pools.get(poolName).pool;
        int attempts = 0;
        int remainingRolls = rolls;
        while ((fillUntilFull || remainingRolls-- > 0) && attempts++ < 4096) {
            if (isFull(inventory)) return;
            WeightedRandomChestContentFrom1710 entry = WeightedRandom.getRandomItem(random, Arrays.asList(pool));
            if (entry == null) return;
            ItemStack[] stacks = entry.generateChestContent(random, inventory);
            boolean insertedAny = false;
            if (stacks != null) {
                for (ItemStack stack : stacks) {
                    if (stack == null || stack.isEmpty()) continue;
                    ItemStack remainder = ItemHandlerHelper.insertItem(inventory, stack.copy(), false);
                    if (remainder.getCount() < stack.getCount()) insertedAny = true;
                }
            }
            if (!insertedAny && !fillUntilFull) continue;
            if (!insertedAny) return;
        }
    }

    private static boolean isFull(ItemStackHandler inventory) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxStackSize(), inventory.getSlotLimit(i))) return false;
        }
        return true;
    }
}
