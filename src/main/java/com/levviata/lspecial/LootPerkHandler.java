package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** records loaded loot-table identities so generation hooks can recognize configured rare tables. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class LootPerkHandler {
    private static final Map<LootTable, ResourceLocation> TABLE_NAMES = Collections.synchronizedMap(new WeakHashMap<LootTable, ResourceLocation>());
    private static final ThreadLocal<EntityPlayer> ACTIVE_LOOTER = new ThreadLocal<EntityPlayer>();

    private LootPerkHandler() {
    }

    /** stores each table's resource location without keeping unloaded tables alive. */
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (event != null && event.getTable() != null && event.getName() != null) {
            TABLE_NAMES.put(event.getTable(), event.getName());
        }
    }

    /** exposes the player opening a loot container while its table is being generated. */
    public static EntityPlayer getActiveLootingPlayer() {
        return ACTIVE_LOOTER.get();
    }

    /** scopes the current player to the synchronous loot-container generation call. */
    public static void setActiveLootingPlayer(EntityPlayer player) {
        if (player == null) {
            ACTIVE_LOOTER.remove();
        } else {
            ACTIVE_LOOTER.set(player);
        }
    }

    /** checks the configured path fragment instead of assuming a single mod's rare-table naming scheme. */
    public static boolean isConfiguredRareTable(LootTable table) {
        if (table == null || LSPECIALConfig.luckRareLootTablePattern == null
                || LSPECIALConfig.luckRareLootTablePattern.trim().isEmpty()) {
            return false;
        }

        ResourceLocation name = TABLE_NAMES.get(table);
        return name != null && name.getPath().toLowerCase(java.util.Locale.ROOT)
                .contains(LSPECIALConfig.luckRareLootTablePattern.trim().toLowerCase(java.util.Locale.ROOT));
    }
}
