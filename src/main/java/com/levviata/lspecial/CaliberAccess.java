package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides which alternate calibers a player may use on a gun and cleans up after a player loses access.<br>
 * <br>
 * The gate mirrors the crouch + secondary ammo cycling rules: the cycling config option, Intelligence 5,
 * and for the AMAT also the railgun projectile option with Intelligence 15. Everything here is read-only
 * towards NTM's shared magazine objects; the per-player list lives in {@link PlayerAcceptedBullets}.
 */
public final class CaliberAccess {

    public static final String SELECTED_CALIBER_PREFIX = "LSPECIALSelectedCaliber_";
    public static final String SELECTED_BELT_KEY = "LSPECIALSelectedBeltCaliber";

    private static final Map<String, List<BulletConfig>> FAMILY_CACHE = new ConcurrentHashMap<>();

    private static final class GateCache {
        WeakReference<EntityPlayer> player = new WeakReference<>(null);
        Item item;
        long tick = Long.MIN_VALUE;
        List<BulletConfig> result = Collections.emptyList();
    }

    // one cache per thread, the client and server threads must never share a decision
    private static final ThreadLocal<GateCache> GATE_CACHE = ThreadLocal.withInitial(GateCache::new);

    private CaliberAccess() {
    }

    public static boolean isStarF(String itemId) {
        return "hbm:gun_star_f".equals(itemId) || "hbm:gun_star_f_akimbo".equals(itemId);
    }

    public static boolean isUzi(String itemId) {
        return "hbm:gun_uzi".equals(itemId) || "hbm:gun_uzi_akimbo".equals(itemId);
    }

    public static boolean isG3(String itemId) {
        return "hbm:gun_g3".equals(itemId) || "hbm:gun_g3_zebra".equals(itemId);
    }

    public static boolean isMinigun(String itemId) {
        return "hbm:gun_minigun".equals(itemId) || "hbm:gun_minigun_dual".equals(itemId);
    }

    /**
     * Identifies the guns with a defined alternate caliber, excluding the Minigun's energy modes.
     */
    public static boolean supportsAmmoCycling(String itemId) {
        return "hbm:gun_pepperbox".equals(itemId) || isStarF(itemId) || isUzi(itemId)
                || isG3(itemId) || "hbm:gun_amat".equals(itemId);
    }

    /**
     * Returns whether the player may currently use the alternate calibers of the given gun.
     */
    public static boolean isCyclingAllowed(EntityPlayer player, String itemId) {
        if (itemId == null || !LSPECIALConfig.weaponSecondaryAmmoCycling) {
            return false;
        }
        if (!supportsAmmoCycling(itemId) && !isMinigun(itemId)) {
            return false;
        }
        if (!LSPECIALMod.hasIntelligenceLevel(player, 5)) {
            return false;
        }
        return !"hbm:gun_amat".equals(itemId)
                || (LSPECIALConfig.AMATRailgunProjectile && LSPECIALMod.hasIntelligenceLevel(player, 15));
    }

    /**
     * Returns every alternate caliber of the gun, whether or not any player may use them.
     */
    public static List<BulletConfig> alternatesForItem(String itemId) {
        if (isMinigun(itemId)) {
            return getEnergyCapacitors();
        }
        return getAlternateCalibers(itemId);
    }

    /**
     * Returns the alternate calibers the player may use on this gun right now, cached per player and tick.
     */
    public static List<BulletConfig> getAllowedAlternates(EntityPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return Collections.emptyList();
        }
        Item item = stack.getItem();
        long tick = player.world.getTotalWorldTime();
        GateCache cache = GATE_CACHE.get();
        if (cache.player.get() == player && cache.item == item && cache.tick == tick) {
            return cache.result;
        }

        List<BulletConfig> result = Collections.emptyList();
        if (item.getRegistryName() != null) {
            String itemId = item.getRegistryName().toString();
            if (isCyclingAllowed(player, itemId)) {
                result = alternatesForItem(itemId);
            }
        }
        cache.player = new WeakReference<>(player);
        cache.item = item;
        cache.tick = tick;
        cache.result = result;
        return result;
    }

    /**
     * Returns every native NTM bullet configuration in the alternate caliber family.
     */
    public static List<BulletConfig> getAlternateCalibers(String itemId) {
        if ("hbm:gun_amat".equals(itemId)) {
            return Collections.singletonList(AMATRailgun.getBulletConfig());
        }
        List<BulletConfig> cached = FAMILY_CACHE.get(itemId);
        if (cached != null) {
            return cached;
        }

        String prefix;
        Class<?> factory;
        if ("hbm:gun_pepperbox".equals(itemId)) {
            factory = XFactory44.class;
            prefix = "m44_";
        } else if (isStarF(itemId)) {
            factory = XFactory50.class;
            prefix = "bmg50_";
        } else if (isUzi(itemId)) {
            factory = XFactory556mm.class;
            prefix = "r556_";
        } else if (isG3(itemId)) {
            factory = XFactory10ga.class;
            prefix = "g10";
        } else {
            return Collections.emptyList();
        }

        List<BulletConfig> result = new ArrayList<>();
        boolean complete = true;
        try {
            for (java.lang.reflect.Field field : factory.getFields()) {
                if (field.getType() != BulletConfig.class || !field.getName().startsWith(prefix)) continue;
                BulletConfig config = (BulletConfig) field.get(null);
                if (config == null) {
                    complete = false;
                } else if (!result.contains(config)) {
                    result.add(config);
                }
            }
        } catch (IllegalAccessException ignored) {
            // public factory fields are expected, an inaccessible field just isn't a usable caliber
        }
        result.sort(Comparator.comparingInt(config -> config.id));
        List<BulletConfig> unmodifiable = Collections.unmodifiableList(result);
        // don't cache a family while NTM is still filling its factory fields in
        if (complete && !result.isEmpty()) {
            FAMILY_CACHE.put(itemId, unmodifiable);
        }
        return unmodifiable;
    }

    /**
     * Returns the native capacitor configurations used by NTM's laser, Tesla, and emerald energy weapons.
     */
    public static List<BulletConfig> getEnergyCapacitors() {
        return Arrays.asList(XFactoryEnergy.energy_las, XFactoryEnergy.energy_las_overcharge,
                XFactoryEnergy.energy_las_ir, XFactoryEnergy.energy_tesla,
                XFactoryEnergy.energy_tesla_overcharge, XFactoryEnergy.energy_tesla_ir,
                XFactoryEnergy.energy_emerald,
                XFactoryEnergy.energy_emerald_overcharge, XFactoryEnergy.energy_emerald_ir);
    }

    public static BulletConfig findBulletConfig(int id) {
        for (BulletConfig config : BulletConfig.configs) {
            if (config != null && config.id == id) return config;
        }
        return null;
    }

    /**
     * Returns old-caliber ammunition to inventory, dropping every leftover stack beside the player.
     */
    public static boolean refundAmmo(EntityPlayer player, BulletConfig oldConfig, int count) {
        if (count <= 0) return true;
        if (oldConfig.ammo == null) return false;
        ItemStack template = oldConfig.ammo.getStack();
        if (template.isEmpty()) return false;

        int remaining = count;
        while (remaining > 0) {
            ItemStack refund = template.copy();
            int stackCount = Math.min(remaining, refund.getMaxStackSize());
            refund.setCount(stackCount);
            giveToPlayer(player, refund);
            remaining -= stackCount;
        }
        return true;
    }

    /**
     * Gives the stack to the player, whatever doesn't fit drops beside them.<br>
     * Creative mode can't be trusted with addItemStackToInventory: a full inventory there swallows the stack.
     */
    public static void giveToPlayer(EntityPlayer player, ItemStack stack) {
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.inventory), stack, false);
        if (!leftover.isEmpty()) {
            player.dropItem(leftover, false);
        }
    }

    /**
     * Takes back an alternate caliber the player is no longer allowed to use, refunding the loaded rounds.<br>
     * <br>
     * Runs on the server only. Rounds that can't be refunded (a config without an ammo item) are dropped,
     * because leaving them loaded would fire them as the base caliber.
     */
    public static void validateSelection(EntityPlayer player, ItemStack stack) {
        if (player == null || player.world.isRemote || stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!hasSelection(tag) || !(stack.getItem() instanceof ItemGunBaseNT gun)) {
            return;
        }

        List<BulletConfig> allowed = getAllowedAlternates(player, stack);
        List<BulletConfig> managed = stack.getItem().getRegistryName() == null
                ? Collections.emptyList() : alternatesForItem(stack.getItem().getRegistryName().toString());
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            GunConfig config = gun.getConfig(stack, configIndex);
            if (config == null) continue;
            Receiver[] receivers = config.getReceivers(stack);
            if (receivers == null) continue;
            for (Receiver receiver : receivers) {
                if (receiver == null) continue;
                IMagazine magazine = receiver.getMagazine(stack);
                while (magazine instanceof RefundMagazine) {
                    magazine = ((RefundMagazine) magazine).getOriginalMagazine();
                }
                if (magazine == null || !visited.add(magazine)) continue;
                if (magazine instanceof MagazineSingleTypeBase single) {
                    revokeSingle(player, stack, single, allowed, managed);
                } else if (magazine instanceof MagazineBelt belt) {
                    revokeBelt(stack, belt, allowed, managed);
                }
            }
        }
    }

    private static boolean hasSelection(NBTTagCompound tag) {
        if (tag.hasKey(SELECTED_BELT_KEY)) {
            return true;
        }
        for (String key : tag.getKeySet()) {
            if (key.startsWith(SELECTED_CALIBER_PREFIX)) return true;
        }
        return false;
    }

    private static boolean isRevoked(BulletConfig config, List<BulletConfig> base, List<BulletConfig> allowed,
                                     List<BulletConfig> managed) {
        return config != null && managed.contains(config) && !base.contains(config) && !allowed.contains(config);
    }

    private static void revokeSingle(EntityPlayer player, ItemStack stack, MagazineSingleTypeBase single,
                                     List<BulletConfig> allowed, List<BulletConfig> managed) {
        NBTTagCompound tag = stack.getTagCompound();
        String key = SELECTED_CALIBER_PREFIX + single.index;
        List<BulletConfig> base = PlayerAcceptedBullets.baseOf(single.acceptedBullets);
        if (!tag.hasKey(key) || base == null || base.isEmpty()) return;

        BulletConfig selected = findBulletConfig(tag.getInteger(key));
        int rawType = MagazineSingleTypeBase.getMagType(stack, single.index);
        BulletConfig loadedType = rawType >= 0 && rawType < BulletConfig.configs.size()
                ? BulletConfig.configs.get(rawType) : null;
        boolean selectedBad = isRevoked(selected, base, allowed, managed);
        boolean loadedBad = isRevoked(loadedType, base, allowed, managed);
        if (!selectedBad && !loadedBad) return;

        if (loadedBad) {
            int loaded = single.getAmount(stack, null);
            if (loaded > 0) {
                int perItem = Math.max(1, loadedType.ammoReloadCount);
                if (!refundAmmo(player, loadedType, loaded / perItem)) {
                    LSPECIALMod.LOGGER.warn("LSPECIAL could not refund {} rounds of caliber {}, dropping them", loaded, loadedType.id);
                }
                single.setAmount(stack, 0);
            }
            single.setType(stack, base.get(0));
            single.setAmountBeforeReload(stack, 0);
            single.setAmountAfterReload(stack, 0);
        }
        tag.removeTag(key);
    }

    private static void revokeBelt(ItemStack stack, MagazineBelt belt, List<BulletConfig> allowed,
                                   List<BulletConfig> managed) {
        NBTTagCompound tag = stack.getTagCompound();
        List<BulletConfig> base = PlayerAcceptedBullets.baseOf(belt.acceptedBullets);
        if (!tag.hasKey(SELECTED_BELT_KEY) || base == null || base.isEmpty()) return;

        BulletConfig selected = findBulletConfig(tag.getInteger(SELECTED_BELT_KEY));
        if (!isRevoked(selected, base, allowed, managed)) return;

        // the belt is inventory-fed, so there are no loaded rounds to refund
        tag.removeTag(SELECTED_BELT_KEY);
        int rawType = MagazineBelt.getMagType(stack);
        BulletConfig current = rawType >= 0 && rawType < BulletConfig.configs.size()
                ? BulletConfig.configs.get(rawType) : null;
        if (isRevoked(current, base, allowed, managed)) {
            belt.setType(stack, base.get(0));
        }
    }
}
