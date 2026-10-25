package com.levviata.lspecial.client;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class ClientGunState {
    private static ItemStack gun = ItemStack.EMPTY;
    private static int currentGunIndex = -1;
    private static final Map<Integer, Float> damages = new HashMap<>();

    private ClientGunState() {
    }

    public static void set(ItemStack stack, int currentGunIndex, int[] indices, float[] values) {
        gun = stack == null ? ItemStack.EMPTY : stack.copy();
        ClientGunState.currentGunIndex = currentGunIndex;

        com.levviata.lspecial.LSPECIALMod.info("GunDebug",
                "Client received gun state: gun={}, registry={}, meta={}, gunIndex={}, entries={}",
                gun.getDisplayName(),
                gun.isEmpty() || gun.getItem().getRegistryName() == null ? "null" : gun.getItem().getRegistryName(),
                gun.isEmpty() ? -1 : gun.getMetadata(),
                currentGunIndex,
                indices == null ? 0 : indices.length);
        damages.clear();

        if (indices == null || values == null) {
            return;
        }

        int size = Math.min(indices.length, values.length);

        for (int i = 0; i < size; i++) {
            damages.put(indices[i], values[i]);
            com.levviata.lspecial.LSPECIALMod.info("GunDebug",
                    "Client stored gun config: index={}, damage={}", indices[i], values[i]);
        }
    }

    public static boolean matches(ItemStack stack) {
        boolean matches = stack != null
                && !stack.isEmpty()
                && !gun.isEmpty()
                && ItemStack.areItemsEqual(stack, gun);

        if (stack != null && !stack.isEmpty()) {
            com.levviata.lspecial.LSPECIALMod.info("GunDebug",
                    "Client gun match: heldTooltipGun={}, storedGun={}, matches={}, storedIndex={}",
                    stack.getDisplayName(),
                    gun.getDisplayName(),
                    matches,
                    currentGunIndex);
        }

        return matches;
    }

    public static ItemStack getGun() {
        return gun;
    }

    public static int getCurrentGunIndex() {
        return currentGunIndex;
    }

    public static float getDamage(int gunIndex) {
        Float damage = damages.get(gunIndex);
        return damage == null ? Float.NaN : damage;
    }

    public static void clear() {
        gun = ItemStack.EMPTY;
        currentGunIndex = -1;
        damages.clear();
    }
}
