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
        damages.clear();

        if (indices == null || values == null) {
            return;
        }

        int size = Math.min(indices.length, values.length);

        for (int i = 0; i < size; i++) {
            damages.put(indices[i], values[i]);
        }
    }

    public static boolean matches(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && !gun.isEmpty()
                && stack.getItem() == gun.getItem()
                && stack.getMetadata() == gun.getMetadata();
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
