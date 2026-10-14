package com.levviata.lspecial;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.render.anim.sedna.AnimationEnums.GunAnimation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

/** server-side SPECIAL effects that need Forge events rather than per-frame GUI code. */
public final class SPECIALGameplayHandler {
    private static final String TEMP_UNBREAKABLE = "LSPECIALLuckTemporaryUnbreakable";
    private static final Map<EntityPlayer, Map<ItemStack, Boolean>> protectedArmor = new WeakHashMap<>();
    private static final Map<EntityPlayer, LockedGun> rapidFireLocks = new WeakHashMap<>();
    private static final Map<EntityPlayer, PendingSelfKill> pendingSelfKills = new WeakHashMap<>();
    private static final Map<EntityPlayer, PendingBurst> pendingBursts = new WeakHashMap<>();

    private static final class PendingBurst {
        private final ItemStack stack;
        private int ticksRemaining;

        private PendingBurst(ItemStack stack, int ticksRemaining) {
            this.stack = stack;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final class PendingSelfKill {
        private final ItemStack stack;
        private int ticksRemaining;
        private boolean firedCue;

        private PendingSelfKill(ItemStack stack, int ticksRemaining) {
            this.stack = stack;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final class LockedGun {
        private final ItemStack stack;
        private final int slot;
        private int ticksRemaining;
        private boolean firedCue;

        private LockedGun(ItemStack stack, int slot, int ticksRemaining) {
            this.stack = stack;
            this.slot = slot;
            this.ticksRemaining = ticksRemaining;
        }
    }

    /** keeps the selected light revolver locked while its triggered rapid-fire burst is active. */
    @SubscribeEvent
    public void onRapidFirePlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        LockedGun locked = rapidFireLocks.get(player);
        if (locked == null) return;

        if (!lspecial$canUseLightRevolverRapidFire(player)) {
            lspecial$releaseRapidFire(player, locked);
            return;
        }

        int actualSlot = lspecial$findHotbarStack(player, locked.stack);
        if (actualSlot < 0 || locked.stack.isEmpty() || --locked.ticksRemaining <= 0) {
            lspecial$releaseRapidFire(player, locked);
            return;
        }

        if (player.inventory.currentItem != actualSlot) {
            player.inventory.currentItem = actualSlot;
        }
        lspecial$setRapidFire(locked.stack, true);
    }

    /** starts a native rapid-fire burst and locks selection to the exact stack that triggered it. */
    public static boolean startLightRevolverRapidFire(EntityPlayer player, ItemStack stack) {
        if (!lspecial$canUseLightRevolverRapidFire(player) || !lspecial$isLightRevolver(stack)) return false;
        int slot = lspecial$findHotbarStack(player, stack);
        if (slot < 0) {
            ItemStack held = player.getHeldItemMainhand();
            if (!held.isEmpty() && ItemStack.areItemsEqual(held, stack)) {
                slot = player.inventory.currentItem;
            }
        }
        if (slot < 0) return false;

        // use the actual inventory object: keybind callbacks can supply an equivalent stack copy.
        ItemStack actualStack = player.inventory.getStackInSlot(slot);
        if (actualStack.isEmpty() || !lspecial$isLightRevolver(actualStack)) return false;
        LockedGun previous = rapidFireLocks.put(player,
                new LockedGun(actualStack, slot, LSPECIALConfig.lightRevolverRapidFireTicks));
        if (previous != null && previous.stack != actualStack) lspecial$setRapidFire(previous.stack, false);
        lspecial$setRapidFire(actualStack, true);
        return true;
    }

    /** reports whether this exact stack currently owns the player's rapid-fire burst. */
    public static boolean isLightRevolverRapidFireLocked(EntityPlayer player, ItemStack stack) {
        LockedGun locked = rapidFireLocks.get(player);
        return locked != null && locked.stack == stack && lspecial$canUseLightRevolverRapidFire(player);
    }

    /** releases the lock and clears native primary input so firing cannot continue after the burst. */
    private static void lspecial$releaseRapidFire(EntityPlayer player, LockedGun locked) {
        rapidFireLocks.remove(player);
        lspecial$setRapidFire(locked.stack, false);
    }

    /** checks the configured perk and its Intelligence threshold before forcing fire input. */
    private static boolean lspecial$canUseLightRevolverRapidFire(EntityPlayer player) {
        return player != null && !player.isDead && LSPECIALConfig.modifyGuns
                && LSPECIALConfig.intelligence && LSPECIALConfig.pepperboxPerk
                && LSPECIALConfig.lightRevolverRapidFirePerk && inst != null
                && inst.getIntelligenceScore() != null
                && inst.getIntelligenceScore().getScorePoints() >= 5;
    }

    /** identifies the NTM light revolver without relying on its translated name. */
    private static boolean lspecial$isLightRevolver(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getRegistryName() != null
                && "hbm:gun_light_revolver".equals(stack.getItem().getRegistryName().toString());
    }

    /** finds the exact stack identity in the hotbar so a replacement stack cannot inherit the lock. */
    private static int lspecial$findHotbarStack(EntityPlayer player, ItemStack target) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.inventory.getStackInSlot(slot) == target) return slot;
        }
        return -1;
    }

    /** sets the native primary-fire state for each NTM configuration on the locked stack. */
    private static void lspecial$setRapidFire(ItemStack stack, boolean firing) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        for (int config = 0; config < gun.getConfigCount(); config++) {
            ItemGunBaseNT.setPrimary(stack, config, firing);
        }
    }

    /** holds native primary input long enough for the receiver to emit three separate shots. */
    public static void startSequentialBurst(EntityPlayer player, ItemStack stack, int delay, int requiredLevel) {
        if (player == null || player.world.isRemote || player.isDead || stack == null || stack.isEmpty()
                || player.getHeldItemMainhand() != stack || !LSPECIALConfig.modifyGuns
                || !LSPECIALConfig.intelligence || inst == null || inst.getIntelligenceScore() == null
                || inst.getIntelligenceScore().getScorePoints() < requiredLevel
                || !(stack.getItem() instanceof ItemGunBaseNT)) {
            return;
        }

        PendingBurst previous = pendingBursts.remove(player);
        if (previous != null) lspecial$setRapidFire(previous.stack, false);
        pendingBursts.put(player, new PendingBurst(stack, Math.max(2, delay * 2 + 3)));
        lspecial$setRapidFire(stack, true);
    }

    private static void lspecial$tickPendingBurst(EntityPlayer player) {
        PendingBurst burst = pendingBursts.get(player);
        if (burst == null) return;
        if (player.isDead || player.world == null || player.world.isRemote
                || player.getHeldItemMainhand() != burst.stack || burst.stack.isEmpty()) {
            pendingBursts.remove(player);
            lspecial$setRapidFire(burst.stack, false);
            return;
        }
        if (--burst.ticksRemaining <= 0) {
            pendingBursts.remove(player);
            lspecial$setRapidFire(burst.stack, false);
        } else {
            lspecial$setRapidFire(burst.stack, true);
        }
    }

    /** starts the Lag self-kill countdown and plays its inspect animation. */
    public static boolean startLagSelfKill(EntityPlayer player, ItemStack stack) {
        if (player == null || player.world.isRemote || player.isDead || stack == null || stack.isEmpty()
                || player.getHeldItemMainhand() != stack
                || !LSPECIALConfig.modifyGuns || !LSPECIALConfig.lagSecondarySelfKill
                || !lspecial$isLag(stack) || !lspecial$hasLagSelfKillPerk()) return false;

        pendingSelfKills.put(player, new PendingSelfKill(stack, 20));
        if (stack.getItem() instanceof ItemGunBaseNT) {
            ItemGunBaseNT.playAnimation(player, stack, GunAnimation.INSPECT, 0);
        }
        return true;
    }

    /** checks the Lag's Intelligence requirement using the same lesser-perk setting as its other actions. */
    private static boolean lspecial$hasLagSelfKillPerk() {
        return LSPECIALConfig.intelligence && LSPECIALConfig.pepperboxPerk && inst != null
                && inst.getIntelligenceScore() != null && inst.getIntelligenceScore().getScorePoints() >= 5;
    }

    /** identifies the Lag by registry id rather than its translated item name. */
    private static boolean lspecial$isLag(ItemStack stack) {
        return stack.getItem().getRegistryName() != null
                && "hbm:gun_lag".equals(stack.getItem().getRegistryName().toString());
    }

    /** plays the Lag firing cue on tick 19 and applies the self-kill on tick 20. */
    private void lspecial$tickPendingSelfKill(EntityPlayer player) {
        PendingSelfKill pending = pendingSelfKills.get(player);
        if (pending == null) return;
        if (player.isDead || player.world == null || player.world.isRemote
                || player.getHeldItemMainhand() != pending.stack) {
            pendingSelfKills.remove(player);
            return;
        }
        if (--pending.ticksRemaining == 1 && !pending.firedCue) {
            pending.firedCue = true;
            lspecial$playLagFireCue(player, pending.stack);
        }
        if (pending.ticksRemaining <= 0) {
            pendingSelfKills.remove(player);
            player.onKillCommand();
        }
    }

    /** plays the gun's native fire sound and consumes one loaded round for the final cue. */
    private static void lspecial$playLagFireCue(EntityPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        GunConfig config = gun.getConfig(stack, 0);
        if (config == null) return;
        Receiver[] receivers = config.getReceivers(stack);
        if (receivers == null) return;

        SoundEvent sound = null;
        for (Receiver receiver : receivers) {
            if (receiver == null) continue;
            if (sound == null) sound = receiver.getFireSound(stack);
            IMagazine magazine = receiver.getMagazine(stack);
            if (magazine == null) continue;
            int amount = magazine.getAmount(stack, null);
            if (amount > 0) {
                magazine.setAmount(stack, amount - 1);
                break;
            }
        }
        if (sound != null) {
            player.world.playSound(null, player.getPosition(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!LSPECIALConfig.luck || !LSPECIALConfig.luckArmorDurabilityProtection) return;

        int luck = inst.getLuckScore().getScorePoints();
        if (luck <= 0) return;
        double chance = Math.min(LSPECIALConfig.luckArmorDurabilityChanceCap,
                Math.max(0, luck * LSPECIALConfig.luckArmorDurabilityChancePerPoint));
        if (player.getRNG().nextDouble() >= chance) return;

        Map<ItemStack, Boolean> restore = protectedArmor.computeIfAbsent(player, ignored -> new IdentityHashMap<>());
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (armor.isEmpty() || !armor.isItemStackDamageable()) continue;
            if (!restore.containsKey(armor)) {
                boolean wasUnbreakable = armor.hasTagCompound() && armor.getTagCompound().getBoolean("Unbreakable");
                restore.put(armor, wasUnbreakable);
                if (!armor.hasTagCompound()) armor.setTagCompound(new NBTTagCompound());
                armor.getTagCompound().setBoolean("Unbreakable", true);
                armor.getTagCompound().setBoolean(TEMP_UNBREAKABLE, true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        lspecial$tickPendingSelfKill(player);
        lspecial$tickPendingBurst(player);

        Map<ItemStack, Boolean> restore = protectedArmor.remove(player);
        if (restore != null) {
            for (Map.Entry<ItemStack, Boolean> entry : restore.entrySet()) {
                ItemStack stack = entry.getKey();
                if (stack == null || stack.isEmpty()) continue;
                if (stack.hasTagCompound() && stack.getTagCompound().getBoolean(TEMP_UNBREAKABLE)) {
                    stack.getTagCompound().setBoolean("Unbreakable", entry.getValue());
                    stack.getTagCompound().removeTag(TEMP_UNBREAKABLE);
                    if (stack.getTagCompound().getKeySet().isEmpty()) {
                        stack.setTagCompound(null);
                    }
                }
                // at the configured fail-safe level, a nearly broken armor piece is unequipped
                // instead of being left equipped with a single durability point.
                if (inst.getLuckScore().getScorePoints() >= LSPECIALConfig.luckArmorFailsafeLevel
                        && stack.isItemStackDamageable()
                        && stack.getMaxDamage() - stack.getItemDamage() <= 1) {
                    for (int slot = 0; slot < player.inventory.armorInventory.size(); slot++) {
                        if (player.inventory.armorInventory.get(slot) == stack) {
                            player.inventory.armorInventory.set(slot, ItemStack.EMPTY);
                            if (!player.inventory.addItemStackToInventory(stack)) player.dropItem(stack, false);
                            break;
                        }
                    }
                }
            }
        }

        if (LSPECIALConfig.charisma && LSPECIALConfig.charismaNeutralMobs
                && inst.getCharismaScore().getScorePoints() >= LSPECIALConfig.charismaNeutralLevel
                && player.ticksExisted % 10 == 0) {
            for (Entity entity : player.world.loadedEntityList) {
                if (!(entity instanceof EntityLiving) || entity == player
                        || entity.getDistanceSq(player) > 32 * 32) continue;

                EntityLiving mob = (EntityLiving) entity;

                if (isConfiguredNeutralMob(mob) && mob.getAttackTarget() == player) {
                    mob.setAttackTarget(null);
                }
            }
        }
    }

    @SubscribeEvent
    public void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityVillager) || event.getEntityPlayer().world.isRemote) return;
        EntityPlayer player = event.getEntityPlayer();
        if (!LSPECIALConfig.charisma || !LSPECIALConfig.charismaVillagerDiscount || inst == null
                || inst.getCharismaScore() == null
                || inst.getCharismaScore().getScorePoints() < LSPECIALConfig.charismaVillagerDiscountLevel) return;
        EntityVillager villager = (EntityVillager) event.getTarget();
        net.minecraft.village.MerchantRecipeList recipes = villager.getRecipes(player);
        if (recipes == null) return;
        for (net.minecraft.village.MerchantRecipe recipe : recipes) {
            int charisma = inst.getCharismaScore().getScorePoints();
            lspecial$discountTradeCost(recipe.getItemToBuy(), charisma);
            lspecial$discountTradeCost(recipe.getSecondItemToBuy(), charisma);
        }
    }

    /** discounts any stack used as a villager trade cost and records its original count for the tooltip. */
    private static void lspecial$discountTradeCost(ItemStack cost, int charisma) {
        if (cost == null || cost.isEmpty()) return;
        NBTTagCompound existingDiscount = cost.hasTagCompound()
                ? cost.getTagCompound().getCompoundTag("LSPECIALTradeDiscount") : new NBTTagCompound();
        int originalCount = existingDiscount.hasKey("originalCount")
                ? existingDiscount.getInteger("originalCount") : cost.getCount();
        if (originalCount <= 1) return;

        Map<String, Double> variables = new java.util.HashMap<>();
        variables.put("charisma", (double) charisma);
        double fraction = LSPECIALExpression.eval(LSPECIALConfig.charismaVillagerDiscountFormula, variables, 0.0D);
        fraction = Math.max(0.0D, Math.min(0.75D, fraction));
        int reduced = Math.max(1, (int) Math.ceil(originalCount * (1.0 - fraction)));
        if (reduced >= originalCount) {
            if (cost.hasTagCompound()) cost.getTagCompound().removeTag("LSPECIALTradeDiscount");
            cost.setCount(originalCount);
            return;
        }

        NBTTagCompound discount = new NBTTagCompound();
        discount.setInteger("originalCount", originalCount);
        discount.setInteger("discountedCount", reduced);
        discount.setInteger("charismaLevel", charisma);
        discount.setDouble("discountFraction", fraction);
        cost.setTagInfo("LSPECIALTradeDiscount", discount);
        cost.setCount(reduced);
    }

    private boolean isConfiguredNeutralMob(EntityLivingBase entity) {
        ResourceLocation key = net.minecraft.entity.EntityList.getKey(entity);
        String registryId = key == null ? null : key.toString();
        if (registryId == null || LSPECIALConfig.charismaNeutralEntityList == null) return false;
        for (String raw : LSPECIALConfig.charismaNeutralEntityList.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) continue;
            if (id.indexOf(':') < 0) id = "minecraft:" + id;
            if (id.equals(registryId)) return true;
        }
        return false;
    }
}
