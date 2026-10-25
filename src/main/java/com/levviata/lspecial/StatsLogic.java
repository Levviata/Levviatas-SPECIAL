package com.levviata.lspecial;

import com.github.bsideup.jabel.Desugar;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

import static com.levviata.lspecial.LSPECIALMod.syncStats;
import static com.levviata.lspecial.LSPECIALMod.syncGunState;
import static com.levviata.lspecial.SPECIALScoreboard.inst;
import static com.levviata.lspecial.command.CommandSetFlySpeed.getPlayerFlySpeed;
import static com.levviata.lspecial.command.CommandSetSpeed.getPlayerSpeed;
import static com.levviata.lspecial.potion.PotionAmplifiedRegeneration.AMPLIFIED_REGENERATION_NAME;

public class StatsLogic {
    // vanilla uuid, it belongs to the held weapon's own damage modifier so this mod must never write to it
    public static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    // this mod's own strength modifier
    private static final UUID STRENGTH_UUID = UUID.fromString("a3c9e7b1-2d54-4f86-8b1a-6e0d9c3f5a09");
    //private static final UUID ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    // randomized uuids, same UUIDs as my mod Attribute Modifier
    private static final UUID MAX_HEALTH_UUID = UUID.fromString("5b94c2f0-6a6e-4b7d-9f6f-8d2a4d7c1e01");
    //private static final UUID FOLLOW_RANGE_UUID =UUID.fromString("7d31a6c2-15bb-47d5-aef5-3c94a87f3202");
    //private static final UUID KNOCKBACK_RESISTANCE_UUID =UUID.fromString("93ef45e8-12c0-4f17-8c3e-61d2a94b5f03");
    private static final UUID MOVEMENT_SPEED_UUID = UUID.fromString("b7d3a6f9-58d4-4b2f-a0f7-9c13e4d8a904");
    //private static final UUID FLYING_SPEED_UUID = UUID.fromString("d2f9c781-7b48-4c81-93ae-0d7f2b6e1505");
    private static final UUID ARMOR_UUID = UUID.fromString("e5a14d92-4f33-4d0f-b1ce-7a8d0f2c3606");
    private static final UUID ARMOR_TOUGHNESS_UUID = UUID.fromString("f84c7b13-2d75-4d8b-9ef4-4b0a91d54707");
    //private static final UUID LUCK_UUID = UUID.fromString("18b4f6d0-8ec1-4cba-a57e-52d6f83a7808");
    private static final String nameIn = "Lev Attribute Modifier";

    private static final Map<UUID, PlayerState> playerStates = new HashMap<>();
    private static final List<String> r = new ArrayList<>();
    // native (unmodified) values of every receiver and bullet config a player has modified. NTM shares these objects
    // between everyone using the same gun, so the first player to touch one records the native values and the last
    // one to let go restores them. Server thread only.
    private static final Map<Receiver, NativeReceiver> nativeReceivers = new IdentityHashMap<>();
    private static final Map<BulletConfig, NativeBullet> nativeBullets = new IdentityHashMap<>();

    // server variable
    private boolean hasLoggedUnsupportedFeature = false;

    public static PlayerState getPlayerstate(EntityPlayer player) {
        return playerStates.computeIfAbsent(player.getUniqueID(), PlayerState::new);
    }

    public static void init() {
        // the buckshot gauge list isn't parsed here, MixinReceiver reads it itself (and ignores malformed entries)
        r.clear();

        for (String raw : LSPECIALConfig.revolverList.split(",")) {
            if (!raw.trim().isEmpty()) {
                r.add(raw.trim());
            }
        }
    }

    private static double expression(String formula, String key, double value) {
        return expression(formula, key, value, null, 0);
    }

    private static double expression(String formula, String key, double value, String key2, double value2) {
        Map<String, Double> vars = new HashMap<>();
        vars.put(key, value);
        if (key2 != null) vars.put(key2, value2);
        return LSPECIALExpression.eval(formula, vars, value);
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayer player = event.player;
        if (player == null) {
            return;
        }

        if (player.world == null || player.world.isRemote) { // server sided
            return;
        }

        PlayerState state = playerStates.computeIfAbsent(player.getUniqueID(), PlayerState::new);

        ItemStack heldStack = player.getHeldItemMainhand();
        updateInfiniteDurability(player, heldStack, state);

        if (heldStack.getItem() instanceof ItemGunBaseNT) {
            applyGunWearPolicy(player, heldStack, state);
        } else {
            state.wearTrackedStack = null;
            state.previousObservedWear = Float.NaN;
        }

        updatePlayerAttributes(player, heldStack, state);

        // perception, intelligence, and luck

        if (heldStack.getItem() instanceof ItemGunBaseSedna) {
            // to my knowledge, no guns inherit this class
            if (state.loggedSednaGun.isEmpty() || !ItemStack.areItemStacksEqual(state.loggedSednaGun, heldStack)) {
                String s = "Your held gun inherits ItemGunBaseSedna.class which doesn't expose damage variables, I can't modify it! Your gun is: " + heldStack;
                LSPECIALMod.playerLog(player, s);

                state.loggedSednaGun = heldStack.copy();
            }
        }

        // restore modifications as soon as the selected stack changes, becomes empty, or the feature is disabled.
        if (state.modified && (heldStack.isEmpty() || !LSPECIALConfig.modifyGuns
                || !(heldStack.getItem() instanceof ItemGunBaseNT)
                || state.modifiedStackRef != heldStack)) {
            cleanGun(state.modifiedStackRef, state);
        }

        if (!heldStack.isEmpty() && heldStack.getItem() != Items.AIR) {
            if (LSPECIALConfig.modifyGuns && heldStack.getItem() instanceof ItemGunBaseNT gun) {
                int perceptionScore = inst.getPlayerScorePoints(player, inst.PERCEPTION_BOARD);
                int luckScore = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);

                if (!state.modified
                        || state.modifiedStackRef != heldStack
                        || state.previousGunPerception != perceptionScore
                        || state.previousGunLuck != luckScore) {

                    int configs = gun.getConfigCount();

                    if (state.modified) {
                        cleanGun(state.modifiedStackRef, state);
                    }

                    for (state.gunIndex = 0; state.gunIndex < configs; state.gunIndex++) {
                        // modify guns using their unique gun config
                        // akimbo guns have two configs so this loop would trigger twice, modifying both
                        modifyGuns(heldStack, player, gun.getConfig(heldStack, state.gunIndex), state);
                    }

                    state.modifiedStackRef = heldStack;
                    state.modified = true;
                    state.previousGunPerception = perceptionScore;
                    state.previousGunLuck = luckScore;

                    if (player instanceof EntityPlayerMP) {
                        syncGunState((EntityPlayerMP) player, state);
                    }
                }
            } else if (state.modified) {
                // the held item changed or gun modification was disabled. Never cast a non-gun item.
                cleanGun(state.modifiedStackRef, state);
                state.previousGunPerception = -1;

                if (player instanceof EntityPlayerMP) {
                    syncGunState((EntityPlayerMP) player, state);
                }
            }
        } else if (state.modified) {
            cleanGun(state.modifiedStackRef, state);
            state.previousGunPerception = -1;

            if (player instanceof EntityPlayerMP) {
                syncGunState((EntityPlayerMP) player, state);
            }
        }

        givePotions(player);
    }

    /**
     * Marks the held tool as unbreakable while the perk is active and puts it back as soon as it stops being held.<br>
     * A flagged stack counts as not damageable for vanilla, so this can't be gated on isItemStackDamageable().
     */
    private void updateInfiniteDurability(EntityPlayer player, ItemStack heldStack, PlayerState state) {
        // anything flagged earlier that isn't the held stack anymore goes back to normal
        if (state.unbreakableStack != null && state.unbreakableStack != heldStack) {
            revertUnbreakable(state.unbreakableStack);
            state.unbreakableStack = null;
        }

        boolean perkActive = !heldStack.isEmpty() && heldStack.getMaxDamage() > 0
                && LSPECIALConfig.infiniteItemDurability
                && LSPECIALMod.hasIntelligenceLevel(player, LSPECIALMod.LESSER_PERK)
                && !(heldStack.getItem() instanceof ItemArmor);

        if (perkActive) {
            if (!heldStack.hasTagCompound()) {
                heldStack.setTagCompound(new NBTTagCompound());
            }
            NBTTagCompound tag = heldStack.getTagCompound();
            if (!tag.getBoolean("LSPECIALWasUnbreakableRecorded")) {
                tag.setBoolean("LSPECIALWasUnbreakable", tag.getBoolean("Unbreakable"));
                tag.setBoolean("LSPECIALWasUnbreakableRecorded", true);
            }
            tag.setBoolean("LSPECIALAppliedUnbreakable", true);
            tag.setBoolean("Unbreakable", true);
            state.unbreakableStack = heldStack;
        } else {
            revertUnbreakable(heldStack);
        }

        // a flagged stack can still end up somewhere else (shift-click copies it, trades, etc), sweep once a second
        if (player.ticksExisted % 20 == 0) {
            for (ItemStack stack : player.inventory.mainInventory) {
                if (stack != heldStack) {
                    revertUnbreakable(stack);
                }
            }
            for (ItemStack stack : player.inventory.offHandInventory) {
                revertUnbreakable(stack);
            }
        }
    }

    /**
     * Undoes the perk's Unbreakable flag on a stack, restoring whatever it had before.
     */
    private static void revertUnbreakable(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();

        if (!tag.getBoolean("LSPECIALAppliedUnbreakable")) {
            return;
        }

        if (tag.getBoolean("LSPECIALWasUnbreakable")) {
            tag.setBoolean("Unbreakable", true);
        } else {
            tag.removeTag("Unbreakable");
        }

        tag.removeTag("LSPECIALAppliedUnbreakable");
        tag.removeTag("LSPECIALWasUnbreakable");
        tag.removeTag("LSPECIALWasUnbreakableRecorded");

        if (tag.hasNoTags()) {
            // an empty tag would stop this stack from merging with identical ones
            stack.setTagCompound(null);
        }
    }

    private void modifyGuns(ItemStack heldStack, EntityPlayer player, GunConfig cfg, PlayerState state) {
        if (heldStack.getItem().equals(ModItems.gun_chemthrower) || heldStack.getItem().equals(ModItems.gun_flamer) || heldStack.getItem().equals(ModItems.gun_flamer_topaz) || heldStack.getItem().equals(ModItems.gun_drill)) {
            return;
        }

        Receiver[] receivers = cfg.getReceivers(heldStack);
        if (receivers == null || receivers.length == 0 || receivers[0] == null) return;
        state.currentGunIndex = state.gunIndex;
        LSPECIALMod.info("GunDebug",
                "Server modifying gun: player={}, gun={}, registry={}, gunIndex={}, config={}",
                player.getName(),
                heldStack.getDisplayName(),
                heldStack.getItem().getRegistryName(),
                state.gunIndex,
                cfg);

        if (LSPECIALConfig.intelligence && "INFINITE_AT_INTELLIGENCE_5".equalsIgnoreCase(LSPECIALConfig.gunWearMode) && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= 5) {
            ItemGunBaseNT.setWear(heldStack, state.gunIndex, 0);
        }

        state.modifiedReceiver = receivers[0];

        if (state.modifiedReceiver.getMagazine(heldStack) == null) return;
        Object ammoType = state.modifiedReceiver.getMagazine(heldStack).getType(heldStack, player.inventory);
        if (!(ammoType instanceof BulletConfig bConfig)) return;

        setOriginalValues(bConfig, state.modifiedReceiver, heldStack, state);

        // if i have luck, install a magazine that can refund bullets. tracked per receiver so akimbo guns get both wrapped
        NativeReceiver nativeReceiver = nativeReceivers.get(state.modifiedReceiver);
        if (nativeReceiver != null && !nativeReceiver.refundInstalled
                && LSPECIALConfig.refundMagazines && LSPECIALConfig.luck && inst.getPlayerScorePoints(player, inst.LUCK_BOARD) > 0) {
            state.modifiedReceiver.mag(new RefundMagazine(state.originalMagazine));
            nativeReceiver.refundInstalled = true;
        }

        processReceiver(state.modifiedReceiver, state.originalDamage, heldStack, player, state);
    }

    private void processReceiver(Receiver recIn, float ogDmg, ItemStack heldStack, EntityPlayer player, PlayerState state) {
        int perceptionScore = inst.getPlayerScorePoints(player, inst.PERCEPTION_BOARD);

        double perceptionBonus = LSPECIALConfig.perception
                ? expression(
                LSPECIALConfig.perceptionGunDamageFormula,
                "perception",
                perceptionScore,
                "originalDamage",
                (double) ogDmg) : 0.0;

        double newDamage = ogDmg + perceptionBonus;

        LSPECIALMod.info("StatsLogic","Perception gun damage: original={}, perception={}, bonus={}, final={}",
                String.valueOf(ogDmg),
                perceptionScore,
                perceptionBonus,
                newDamage);

        recIn.dmg((float) newDamage);

        state.modifiedGunDamages.put(state.gunIndex, (float) newDamage);

        LSPECIALMod.info("GunDebug",
                "Server receiver updated: player={}, gunIndex={}, original={}, final={}, receiverDamageNow={}",
                player.getName(),
                state.gunIndex,
                ogDmg,
                newDamage,
                recIn.getBaseDamage(heldStack));

        if (recIn.getMagazine(heldStack) == null) {
            return;
        }

        Object ammoType = recIn.getMagazine(heldStack).getType(heldStack, player.inventory);
        if (!(ammoType instanceof BulletConfig bC)) {
            return;
        }

        if (LSPECIALConfig.perception && LSPECIALConfig.perceptionArmorThresholdNegation && inst.getPlayerScorePoints(player, inst.PERCEPTION_BOARD) > LSPECIALConfig.perceptionArmorThresholdNegationLevel) {
            double addedNegation = (inst.getPlayerScorePoints(player, inst.PERCEPTION_BOARD) - LSPECIALConfig.perceptionArmorThresholdNegationLevel) * LSPECIALConfig.perceptionArmorThresholdNegationPerPoint;
            bC.armorThresholdNegation = (float) Math.min(1.0, state.originalArmorThresholdNegation + addedNegation);
        }
        if (LSPECIALConfig.intelligence && LSPECIALConfig.pepperboxPerk && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= 5 && !heldStack.isEmpty()) {
            String itemID = String.valueOf(heldStack.getItem().getRegistryName());
            if (LSPECIALConfig.pepperboxSixShotPerk && "hbm:gun_pepperbox".equals(itemID)) {
                recIn.rounds(6);
            }
        }

        if (LSPECIALConfig.intelligence && LSPECIALConfig.revolverPerk && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= 15 && !heldStack.isEmpty()) {
            String itemID = String.valueOf(heldStack.getItem().getRegistryName());
            recIn.auto(true);
            if ("hbm:gun_tau".equals(itemID)) {
                recIn.auto(false);
            }
            if (LSPECIALConfig.revolverPenetrationPerk && r.contains(itemID)) {
                bC.doesPenetrate = true;
                bC.setHeadshot(LSPECIALConfig.revolverHeadshotMultiplier);
            }
        }
    }

    private void cleanGun(ItemStack heldStack, PlayerState state) {
        if (heldStack == null || (!heldStack.isEmpty() && heldStack.getItem().equals(ModItems.gun_chemthrower))) {
            if (state.gunStates.isEmpty()) resetGunTracking(state);
            return;
        }

        // each akimbo/config receiver is released on its own. The native values are only written back once
        // nobody else is holding the same receiver, wear is left alone on purpose (restoring it undid real wear)
        for (int i = state.gunStates.size() - 1; i >= 0; i--) {
            GunState gunState = state.gunStates.get(i);
            if (gunState.bulletConfig != null) {
                releaseBullet(gunState.bulletConfig, state.owner);
            }
            releaseReceiver(gunState.receiver, state.owner);
        }
        resetGunTracking(state);
    }

    /**
     * Drops a player's claim on a receiver and restores its native values if that was the last claim.
     */
    private static void releaseReceiver(Receiver receiver, UUID owner) {
        NativeReceiver nativeValues = nativeReceivers.get(receiver);

        if (nativeValues == null) {
            return;
        }

        nativeValues.holders.remove(owner);

        if (nativeValues.holders.isEmpty()) {
            restoreReceiver(receiver, nativeValues);
            nativeReceivers.remove(receiver);
        }
    }

    private static void releaseBullet(BulletConfig bullet, UUID owner) {
        NativeBullet nativeValues = nativeBullets.get(bullet);

        if (nativeValues == null) {
            return;
        }

        nativeValues.holders.remove(owner);

        if (nativeValues.holders.isEmpty()) {
            restoreBullet(bullet, nativeValues);
            nativeBullets.remove(bullet);
        }
    }

    private static void restoreReceiver(Receiver receiver, NativeReceiver nativeValues) {
        receiver.dmg(nativeValues.damage);
        receiver.auto(nativeValues.refireOnHold);
        receiver.autoAfterDry(nativeValues.refireAfterDry);
        receiver.rounds(nativeValues.rounds);
        receiver.mag(nativeValues.magazine);
    }

    private static void restoreBullet(BulletConfig bullet, NativeBullet nativeValues) {
        bullet.armorPiercingPercent = nativeValues.armorPiercing;
        bullet.armorThresholdNegation = nativeValues.armorThresholdNegation;
        bullet.doesPenetrate = nativeValues.doesPenetrate;
        bullet.headshotMult = nativeValues.headshotMultiplier;
    }

    /**
     * Puts every modified receiver and bullet config back to its native values and forgets all player state.
     * Called when the server stops so nothing leaks into the next world in the same JVM.
     */
    public static void restoreAllNativeGuns() {
        for (Map.Entry<Receiver, NativeReceiver> entry : nativeReceivers.entrySet()) {
            restoreReceiver(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<BulletConfig, NativeBullet> entry : nativeBullets.entrySet()) {
            restoreBullet(entry.getKey(), entry.getValue());
        }
        nativeReceivers.clear();
        nativeBullets.clear();

        for (PlayerState state : playerStates.values()) {
            revertUnbreakable(state.unbreakableStack);
        }
        playerStates.clear();
    }

    private void resetGunTracking(PlayerState state) {
        state.gunStates.clear();
        state.modifiedGunDamages.clear();
        state.gunIndex = 0;
        state.modifiedStackRef = ItemStack.EMPTY;
        state.modifiedReceiver = null;
        state.modified = false;
        state.previousGunPerception = -1;
        state.previousGunLuck = -1;
        state.currentGunIndex = -1;
    }

    private void setOriginalValues(BulletConfig bCIn, Receiver rIn, ItemStack heldStack, PlayerState state) {
        // only the first holder records the values, anyone after that reads the recorded ones so they never
        // mistake another player's perk values for the gun's native ones
        NativeReceiver nativeReceiver = nativeReceivers.get(rIn);
        if (nativeReceiver == null) {
            nativeReceiver = new NativeReceiver(rIn.getBaseDamage(heldStack),
                    ((ReceiverStateAccess) rIn).lspecial$getNativeRefireOnHold(),
                    ((ReceiverStateAccess) rIn).lspecial$getNativeRefireAfterDry(),
                    rIn.getRoundsPerCycle(heldStack), rIn.getMagazine(heldStack));
            nativeReceivers.put(rIn, nativeReceiver);
        }
        nativeReceiver.holders.add(state.owner);

        NativeBullet nativeBullet = nativeBullets.get(bCIn);
        if (nativeBullet == null) {
            nativeBullet = new NativeBullet(bCIn.armorPiercingPercent, bCIn.armorThresholdNegation, bCIn.doesPenetrate, bCIn.headshotMult);
            nativeBullets.put(bCIn, nativeBullet);
        }
        nativeBullet.holders.add(state.owner);

        state.originalMagazine = nativeReceiver.magazine;
        state.originalArmorThresholdNegation = nativeBullet.armorThresholdNegation;
        state.originalDamage = nativeReceiver.damage;
        state.gunStates.add(new GunState(state.gunIndex, rIn, bCIn, nativeReceiver.damage));
    }

    private void givePotions(EntityPlayer player) {
        if (LSPECIALConfig.amplifiedRegeneration && LSPECIALConfig.enduranceBonus && inst.getPlayerScorePoints(player, inst.ENDURANCE_BONUS_BOARD) == 1) {
            String p = Tags.MOD_ID + ":" + AMPLIFIED_REGENERATION_NAME;
            if (Potion.getPotionFromResourceLocation("regeneration") != null && Potion.getPotionFromResourceLocation(p) != null) {
                Potion a = Potion.getPotionFromResourceLocation("regeneration");
                Potion b = Potion.getPotionFromResourceLocation(p);
                if (player.isPotionActive(a)) {
                    PotionEffect existing = player.getActivePotionEffect(a);
                    if (!(LSPECIALConfig.respectHigherRegenAmplifier && existing != null && existing.getAmplifier() > 1)) {
                        PotionEffect ae = player.getActivePotionEffect(a);
                        if (ae != null) {
                            int cAmplifier = ae.getAmplifier();
                            player.removePotionEffect(a);
                            player.addPotionEffect(new PotionEffect(b, ae.getDuration(), cAmplifier + (int) LSPECIALConfig.regenAmplifierBonus));
                        }
                    }
                } else if (!player.isPotionActive(b)) {
                    player.addPotionEffect(new PotionEffect(b, LSPECIALConfig.potionTimeTicks, Math.max(0, LSPECIALConfig.endurancePotionAmplifier)));
                }
            }
        }

        if (LSPECIALConfig.charismaBonus && inst.getPlayerScorePoints(player, inst.CHARISMA_BONUS_BOARD) == 1) {
            if (Potion.getPotionFromResourceLocation("resistance") != null && !player.isPotionActive(Potion.getPotionFromResourceLocation("resistance"))) {
                player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("resistance"), LSPECIALConfig.potionTimeTicks, LSPECIALConfig.charismaPotionAmplifier));
            }
        }
    }

    private void applyGunWearPolicy(EntityPlayer player, ItemStack heldStack, PlayerState state) {
        if (!LSPECIALConfig.modifyGuns) {
            return;
        }

        String mode = LSPECIALConfig.gunWearMode;

        if (!LSPECIALConfig.intelligence) {
            return;
        }

        if ("INFINITE_AT_INTELLIGENCE_5".equalsIgnoreCase(LSPECIALConfig.gunWearMode)
                && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= 5 && heldStack.getItem() instanceof ItemGunBaseNT gun) {
            for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
                ItemGunBaseNT.setWear(heldStack, configIndex, 0);
            }
            return;
        }

        int intelligence = inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD);
        if (intelligence <= 0) {
            return;
        }

        float current = ItemGunBaseNT.getWear(heldStack, 0);

        if (state.wearTrackedStack != heldStack || Float.isNaN(state.previousObservedWear)) {
            state.wearTrackedStack = heldStack;
            state.previousObservedWear = current;
            return;
        }

        if (current > state.previousObservedWear) {
            double reduction;

            if ("PERCENT_REDUCTION".equalsIgnoreCase(mode)) {
                reduction = intelligence * LSPECIALConfig.gunWearReductionPerIntelligence;
            } else {
                Map<String, Double> vars = new HashMap<>();
                vars.put("intelligence", (double) intelligence);
                vars.put("originalWear", (double) state.previousObservedWear);

                reduction = LSPECIALExpression.eval(LSPECIALConfig.gunWearFormula, vars, 0.0);
            }

            reduction = Math.max(0.0, Math.min(1.0, reduction));

            float adjusted = (float) (state.previousObservedWear + (current - state.previousObservedWear) * (1.0 - reduction));

            ItemGunBaseNT.setWear(heldStack, 0, adjusted);
            state.previousObservedWear = adjusted;
        } else {
            state.previousObservedWear = current;
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;

        if (player.world == null || player.world.isRemote) {
            return;
        }

        // the config goes first, the stats and gun sync below are read through it
        if (player instanceof EntityPlayerMP) {
            LSPECIALMod.syncConfig((EntityPlayerMP) player);
        }

        synchronizePlayer(player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;

        if (player.world == null || player.world.isRemote) {
            return;
        }

        synchronizePlayer(player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        EntityPlayer player = event.player;

        if (player.world == null || player.world.isRemote) {
            return;
        }

        synchronizePlayer(player);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;

        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }

        // give back whatever this player was holding on to, otherwise the modified gun stays modified for everyone
        PlayerState state = playerStates.remove(player.getUniqueID());

        if (state != null) {
            if (state.modified) {
                cleanGun(state.modifiedStackRef, state);
            }
            revertUnbreakable(state.unbreakableStack);
        }

        revertUnbreakable(player.getHeldItemMainhand());
    }

    private void synchronizePlayer(EntityPlayer player) {
        PlayerState state = playerStates.computeIfAbsent(player.getUniqueID(), PlayerState::new);

        ItemStack heldStack = player.getHeldItemMainhand();

        // Send the player's SPECIAL data to their client.
        syncStats((EntityPlayerMP) player);

        // Apply the player's attributes immediately.
        updatePlayerAttributes(player, heldStack, state);

        if (LSPECIALConfig.modifyGuns && heldStack.getItem() instanceof ItemGunBaseNT gun) {
            if (state.modified) {
                cleanGun(state.modifiedStackRef, state);
            }

            int perceptionScore = inst.getPlayerScorePoints(player, inst.PERCEPTION_BOARD);
            int configs = gun.getConfigCount();

            for (state.gunIndex = 0; state.gunIndex < configs; state.gunIndex++) {
                modifyGuns(heldStack, player, gun.getConfig(heldStack, state.gunIndex), state);
            }

            state.modifiedStackRef = heldStack;
            state.modified = true;
            state.previousGunPerception = perceptionScore;
            state.previousGunLuck = inst.getPlayerScorePoints(player, inst.LUCK_BOARD);

            syncGunState((EntityPlayerMP) player, state);
        } else {
            state.previousGunPerception = -1;
            syncGunState((EntityPlayerMP) player, state);
        }
    }

    /**
     * Update a player's:<br>
     * - Strength (held item's damage),<br>
     * - Endurance (max health),<br>
     * - Charisma (armor and armor toughness),<br>
     * - and Agility (speed and fly speed)<br>
     * if it has changed compared to last player tick.
     */
    private void updatePlayerAttributes(EntityPlayer player, ItemStack heldStack, PlayerState state) {
        // strength
        IAttributeInstance strength = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);

        // old versions wrote their modifier under vanilla's weapon id, which replaced the weapon's own damage.
        // drop that once (it is recognisable by its name) and hand the held weapon its modifier back
        if (!state.legacyStrengthChecked) {
            state.legacyStrengthChecked = true;
            AttributeModifier legacy = strength.getModifier(ATTACK_DAMAGE_UUID);

            if (legacy != null && nameIn.equals(legacy.getName())) {
                strength.removeModifier(ATTACK_DAMAGE_UUID);

                if (!heldStack.isEmpty()) {
                    Collection<AttributeModifier> weaponModifiers = heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());

                    for (AttributeModifier weaponModifier : weaponModifiers) {
                        if (strength.getModifier(weaponModifier.getID()) == null) {
                            strength.applyModifier(weaponModifier);
                        }
                    }
                }
            }
        }

        // only the bonus is added, the held weapon keeps its own modifier so damage is vanilla + strength
        double strengthBonus = LSPECIALConfig.strength ? expression(LSPECIALConfig.attackDamageFormula, "strength", inst.getPlayerScorePoints(player, inst.STRENGTH_BOARD)) : 0.0;
        AttributeModifier strengthModifier = strength.getModifier(STRENGTH_UUID);

        if (!LSPECIALConfig.strength) {
            if (strengthModifier != null) {
                strength.removeModifier(STRENGTH_UUID);
            }
        } else if (strengthModifier == null || Math.abs(strengthModifier.getAmount() - strengthBonus) > 0.000001D) {
            strength.removeModifier(STRENGTH_UUID);
            strength.applyModifier(new AttributeModifier(STRENGTH_UUID, nameIn, strengthBonus, 0));
        }

        // endurance
        IAttributeInstance maxHealth = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);

        int enduranceScore = inst.getPlayerScorePoints(player, inst.ENDURANCE_BOARD);

        boolean hasEnduranceModifier = maxHealth.getModifier(MAX_HEALTH_UUID) != null;

        if (enduranceScore != state.previousEndurance
                || state.previousEnduranceEnabled != LSPECIALConfig.endurance
                || Float.isNaN(state.previousMaxHealth)
                || Math.abs(player.getMaxHealth() - state.previousMaxHealth) > 0.001F
                || (LSPECIALConfig.endurance && !hasEnduranceModifier)
                || (!LSPECIALConfig.endurance && hasEnduranceModifier)) {

            state.previousEndurance = enduranceScore;

            maxHealth.removeModifier(MAX_HEALTH_UUID);

            if (LSPECIALConfig.endurance) {
                maxHealth.applyModifier(new AttributeModifier(MAX_HEALTH_UUID, nameIn, expression(LSPECIALConfig.enduranceFormula, "endurance", enduranceScore * 2), 0));
            }

            state.previousMaxHealth = player.getMaxHealth();
            state.previousEnduranceEnabled = LSPECIALConfig.endurance;
        }

        // charisma
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance armorToughness = player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);

        boolean charisma = LSPECIALConfig.charisma;

        int charismaScore = inst.getPlayerScorePoints(player, inst.CHARISMA_BOARD);

        boolean hasArmorModifier = armor.getModifier(ARMOR_UUID) != null;
        boolean hasArmorToughnessModifier = armorToughness.getModifier(ARMOR_TOUGHNESS_UUID) != null;

        if (charismaScore != state.previousCharisma
                || state.previousCharismaEnabled != LSPECIALConfig.charisma
                || (charisma && (!hasArmorModifier || !hasArmorToughnessModifier))
                || (!charisma && (hasArmorModifier || hasArmorToughnessModifier))) {

            state.previousCharisma = charismaScore;
            state.previousCharismaEnabled = LSPECIALConfig.charisma;

            armor.removeModifier(ARMOR_UUID);
            armorToughness.removeModifier(ARMOR_TOUGHNESS_UUID);

            if (charisma) {
                armor.applyModifier(new AttributeModifier(ARMOR_UUID, nameIn, expression(LSPECIALConfig.armorFormula, "charisma", charismaScore), 0));
                armorToughness.applyModifier(new AttributeModifier(ARMOR_TOUGHNESS_UUID, nameIn, expression(LSPECIALConfig.armorToughnessFormula, "charisma", charismaScore), 0));
            }
        }

        // agility
        IAttributeInstance movementSpeed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

        int agilityScore = inst.getPlayerScorePoints(player, inst.AGILITY_BOARD);

        int configuredPlayerSpeed = getPlayerSpeed(player);

        // what the modifier should be built from, compared to what it was last built from (not to the raw score,
        // otherwise a /setspeed override would rebuild the modifier every single tick)
        int desiredAgility = configuredPlayerSpeed > -1 ? configuredPlayerSpeed : agilityScore;

        boolean hasMovementSpeedModifier = movementSpeed.getModifier(MOVEMENT_SPEED_UUID) != null;

        if (desiredAgility != state.previousAgility
                || state.previousAgilityEnabled != LSPECIALConfig.agility
                || (LSPECIALConfig.agility && !hasMovementSpeedModifier)
                || (!LSPECIALConfig.agility && hasMovementSpeedModifier)) {

            state.previousAgilityEnabled = LSPECIALConfig.agility;
            state.previousAgility = desiredAgility;

            movementSpeed.removeModifier(MOVEMENT_SPEED_UUID);

            if (LSPECIALConfig.agility) {
                double agilityValue = configuredPlayerSpeed > -1 ? configuredPlayerSpeed : agilityScore;

                movementSpeed.applyModifier(new AttributeModifier(MOVEMENT_SPEED_UUID, nameIn, expression(LSPECIALConfig.agilityFormula, "agility", agilityValue), 1));
            }
        }
        MinecraftServer server = player.getServer();

        // fly speed, only touched while this feature is on (and once more to hand it back when it gets turned off)
        // so other mods that change the fly speed are left alone
        boolean manageFlySpeed = LSPECIALConfig.agility && LSPECIALConfig.agilityFlyingSpeed;

        if (!manageFlySpeed && !state.flySpeedManaged) {
            return;
        }

        float baseFlySpeed = 0.05F;
        float flySpeed = baseFlySpeed;

        if (manageFlySpeed) {
            int configuredFlySpeed = getPlayerFlySpeed(player);

            double agilityValue = configuredFlySpeed >= 0 ? configuredFlySpeed : agilityScore;

            double multiplierBonus = expression(LSPECIALConfig.agilityFlyingSpeedFormula, "agility", agilityValue);

            double minimum = Math.min(LSPECIALConfig.agilityFlyingSpeedMinimum, LSPECIALConfig.agilityFlyingSpeedMaximum);
            double maximum = Math.max(LSPECIALConfig.agilityFlyingSpeedMinimum, LSPECIALConfig.agilityFlyingSpeedMaximum);

            flySpeed = (float) Math.max(minimum, Math.min(maximum, baseFlySpeed * (1.0 + multiplierBonus)));
        }

        state.flySpeedManaged = manageFlySpeed;

        if (Float.isNaN(state.previousFlySpeed)
                || Math.abs(player.capabilities.getFlySpeed() - flySpeed) > 0.0001F) {
            if (server != null) {
                if (server.isSinglePlayer()) {
                    player.capabilities.setFlySpeed(flySpeed);
                } else {
                    player.capabilities.flySpeed = flySpeed; // no setFlySpeed in dedicated servers, the method is missing for some reason
                }
            }

            state.previousFlySpeed = flySpeed;

            if (player instanceof EntityPlayerMP) {
                player.sendPlayerAbilities();
            }
        }
    }

    public static class PlayerState {
        final UUID owner;
        final List<GunState> gunStates = new ArrayList<>();
        int previousEndurance = -1;
        float previousMaxHealth = Float.NaN;
        int previousCharisma = -1;
        int previousAgility = -1;
        boolean legacyStrengthChecked;
        boolean flySpeedManaged;
        ItemStack unbreakableStack;
        int previousGunLuck = -1;
        boolean previousEnduranceEnabled = true;
        boolean previousCharismaEnabled = true;
        boolean previousAgilityEnabled = true;
        ItemStack modifiedStackRef = ItemStack.EMPTY;
        Receiver modifiedReceiver;
        boolean modified;
        float originalDamage;
        float originalArmorThresholdNegation;
        float previousFlySpeed = Float.NaN;
        IMagazine originalMagazine;
        ItemStack wearTrackedStack;
        float previousObservedWear = Float.NaN;
        int poweredArmorRechargeCooldown = 0;

        int gunIndex = -1;
        int currentGunIndex = -1;
        int previousGunPerception = -1;
        final Map<Integer, Float> modifiedGunDamages = new HashMap<>();

        ItemStack loggedSednaGun = ItemStack.EMPTY;

        PlayerState(UUID owner) {
            this.owner = owner;
        }

        GunStateView getGunStateView(int index) {
            GunState gunState = gunStates.get(index);
            Float modifiedDamage = modifiedGunDamages.get(gunState.index);
            return new GunStateView(gunState.index, modifiedDamage == null ? gunState.damage : modifiedDamage);
        }
    }

    public static class GunStateView {
        public final int index;
        public final float damage;

        public GunStateView(int index, float damage) {
            this.index = index;
            this.damage = damage;
        }
    }

    @Desugar
    private record GunState(int index, Receiver receiver, BulletConfig bulletConfig, float damage) {
    }

    /**
     * Native values of a receiver, plus the players currently holding it.
     */
    private static final class NativeReceiver {
        final float damage;
        final boolean refireOnHold;
        final boolean refireAfterDry;
        final int rounds;
        final IMagazine magazine;
        final Set<UUID> holders = new HashSet<>();
        boolean refundInstalled;

        NativeReceiver(float damage, boolean refireOnHold, boolean refireAfterDry, int rounds, IMagazine magazine) {
            this.damage = damage;
            this.refireOnHold = refireOnHold;
            this.refireAfterDry = refireAfterDry;
            this.rounds = rounds;
            this.magazine = magazine;
        }
    }

    /**
     * Native values of a bullet config, plus the players currently using it.
     */
    private static final class NativeBullet {
        final float armorPiercing;
        final float armorThresholdNegation;
        final boolean doesPenetrate;
        final float headshotMultiplier;
        final Set<UUID> holders = new HashSet<>();

        NativeBullet(float armorPiercing, float armorThresholdNegation, boolean doesPenetrate, float headshotMultiplier) {
            this.armorPiercing = armorPiercing;
            this.armorThresholdNegation = armorThresholdNegation;
            this.doesPenetrate = doesPenetrate;
            this.headshotMultiplier = headshotMultiplier;
        }
    }
}
