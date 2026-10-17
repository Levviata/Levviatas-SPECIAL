package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Narrow reflective bridge to Sedna's receiver list, avoiding assumptions about its private field names. */
public final class SednaAmmoAccess {
    private SednaAmmoAccess() { }

    public static IMagazine<?> getMagazine(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return null;
        Object found = findReceiver(stack.getItem(), 0, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
        if (!(found instanceof Receiver)) return null;
        try {
            return ((Receiver) found).getMagazine(stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int getLoaded(ItemStack stack, EntityPlayer player) {
        IMagazine<?> magazine = getMagazine(stack);
        if (magazine == null) return -1;
        try {
            return Math.max(0, magazine.getAmount(stack, player == null ? null : player.inventory));
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static int getCapacity(ItemStack stack) {
        IMagazine<?> magazine = getMagazine(stack);
        if (magazine == null) return -1;
        try {
            return Math.max(0, magazine.getCapacity(stack));
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static boolean consume(ItemStack stack, EntityPlayer player, int amount) {
        if (amount <= 0) return true;
        IMagazine<?> magazine = getMagazine(stack);
        if (magazine == null || player == null) return false;
        try {
            int loaded = magazine.getAmount(stack, player.inventory);
            if (loaded < amount) return false;
            magazine.useUpAmmo(stack, player.inventory, amount);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object findReceiver(Object object, int depth, Set<Object> visited) {
        if (object == null || depth > 4 || !visited.add(object)) return null;
        if (object instanceof Receiver) return object;
        Class<?> type = object.getClass();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    if (value == null || value == object) continue;
                    if (value instanceof Receiver) return value;
                    if (value instanceof Iterable<?>) {
                        for (Object element : (Iterable<?>) value) {
                            Object result = findReceiver(element, depth + 1, visited);
                            if (result != null) return result;
                        }
                    } else if (value.getClass().isArray()) {
                        int length = Array.getLength(value);
                        for (int i = 0; i < length; i++) {
                            Object result = findReceiver(Array.get(value, i), depth + 1, visited);
                            if (result != null) return result;
                        }
                    } else if (depth < 2 && value.getClass().getName().startsWith("com.hbm.items.weapon.sedna")) {
                        Object result = findReceiver(value, depth + 1, visited);
                        if (result != null) return result;
                    }
                } catch (Throwable ignored) {
                    // A denied or unrelated field must not interrupt HUD rendering or airblast.
                }
            }
        }
        return null;
    }
}
