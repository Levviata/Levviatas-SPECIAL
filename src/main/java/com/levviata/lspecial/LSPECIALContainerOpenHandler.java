package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** invokes tagged-container loot perks from the server-side container-open event. */
public final class LSPECIALContainerOpenHandler {

    @SubscribeEvent
    public void onContainerOpened(PlayerContainerEvent.Open event) {
        if (event == null) return;

        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) return;

        Container container = event.getContainer();
        if (container == null) {
            LSPECIALMod.logWarn("[LSPECIAL Loot] Server container-open event had no container for player={}", player.getName());
            return;
        }

        Set<TileEntity> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collectTiles(container, candidates, visited, 0);

        // Slot inventories catch vanilla chest inventories and inventory wrappers such as double chests.
        for (Slot slot : container.inventorySlots) {
            if (slot != null) collectTiles(slot.inventory, candidates, visited, 0);
        }

        boolean handled = false;
        for (TileEntity tile : candidates) {
            if (tile == null || tile.getWorld() != player.world || tile.isInvalid()) continue;
            if (!tile.getTileData().getBoolean(LSPECIALNTMLootHandler.CONTAINER_TAG)) continue;

            handled = true;
            LSPECIALMod.info("[LSPECIAL Loot] Server container-open event found tagged tile: player={} container={} tile={} pos={} pool={}",
                    player.getName(), container.getClass().getName(), tile.getClass().getName(), tile.getPos(),
                    tile.getTileData().getString(LSPECIALNTMLootHandler.POOL_TAG));
            LSPECIALNTMLootHandler.onContainerOpened(tile, player);
        }

        if (!handled && container.getClass().getName().startsWith("com.hbm.inventory.container.")) {
            StringBuilder found = new StringBuilder();
            for (TileEntity tile : candidates) {
                if (found.length() > 0) found.append(", ");
                found.append(tile.getClass().getName()).append('@').append(tile.getPos())
                        .append(" tagged=").append(tile.getTileData().getBoolean(LSPECIALNTMLootHandler.CONTAINER_TAG));
            }
            LSPECIALMod.logWarn("[LSPECIAL Loot] Server container-open event found no tagged tile for NTM container: player={} container={} candidates=[{}]",
                    player.getName(), container.getClass().getName(), found);
        }
    }

    /** finds tile entities referenced directly by a container or through IInventory wrappers. */
    private static void collectTiles(Object value, Set<TileEntity> found, Set<Object> visited, int depth) {
        if (value == null || depth > 3 || visited.contains(value)) return;
        if (value instanceof TileEntity) {
            found.add((TileEntity) value);
            return;
        }
        if (!(value instanceof Container) && !(value instanceof IInventory)) return;
        visited.add(value);

        Class<?> type = value.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object nested = field.get(value);
                    if (nested instanceof TileEntity) {
                        found.add((TileEntity) nested);
                    } else if (nested instanceof IInventory) {
                        collectTiles(nested, found, visited, depth + 1);
                    }
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    LSPECIALMod.info("[LSPECIAL Loot] Could not inspect field {}.{} while resolving an opened container: {}",
                            type.getName(), field.getName(), exception.toString());
                }
            }
            type = type.getSuperclass();
        }
    }
}
