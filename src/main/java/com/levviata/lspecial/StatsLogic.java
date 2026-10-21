package com.levviata.lspecial;

import com.github.bsideup.jabel.Desugar;
import com.hbm.inventory.RecipesCommon;
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
import net.minecraft.item.Item;
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
    //vanilla uuids
    public static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
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
    private static final List<Item> g = new ArrayList<>();
    private static final List<String> r = new ArrayList<>();
    private static final List<UUID> uuids = new ArrayList<>();

    // server variable
    private boolean hasLoggedUnsupportedFeature = false;

    public static PlayerState getPlayerstate(EntityPlayer player) {
        PlayerState state = playerStates.computeIfAbsent(player.getUniqueID(), id -> new PlayerState());
        for (UUID u : playerStates.keySet()) {
            if (u == player.getUniqueID()) {
                return playerStates.get(u);
            }
        }
        return state;
    }

    public static void init() {
        g.clear();
        r.clear();

        for (String raw : LSPECIALConfig.buckshotGaugeList.split(",")) {
            String token = raw.trim();

            if (token.isEmpty()) {
                continue;
            }

            if (token.matches("\\d+")) {
                g.add(new RecipesCommon.ComparableStack(ModItems.ammo_standard, 1, Integer.parseInt(token)).getStack().getItem());
            } else {
                String[] parts = token.split(":");

                if (parts.length >= 3 && parts[0].equals("hbm") && parts[1].equals("ammo_secret")) {
                    g.add(new RecipesCommon.ComparableStack(ModItems.ammo_secret, 1, Integer.parseInt(parts[2])).getStack().getItem());
                }
            }
        }
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

    public static List<UUID> getUUIDs() {
        if (uuids.isEmpty()) {
            uuids.add(ATTACK_DAMAGE_UUID);
            //uuids.add(ATTACK_SPEED_UUID);
            uuids.add(MAX_HEALTH_UUID);
            //uuids.add(FOLLOW_RANGE_UUID);
            //uuids.add(KNOCKBACK_RESISTANCE_UUID);
            uuids.add(MOVEMENT_SPEED_UUID);
            //uuids.add(FLYING_SPEED_UUID);
            uuids.add(ARMOR_UUID);
            uuids.add(ARMOR_TOUGHNESS_UUID);
            // uuids.add(LUCK_UUID);
        }
        return uuids;
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

        PlayerState state = playerStates.computeIfAbsent(player.getUniqueID(), id -> new PlayerState());

        ItemStack heldStack = player.getHeldItemMainhand();
        if (!heldStack.isEmpty() && heldStack.isItemStackDamageable()) {
            boolean perkActive = LSPECIALConfig.infiniteItemDurability && LSPECIALConfig.intelligence
                    && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= 5
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
            } else if (heldStack.hasTagCompound() && heldStack.getTagCompound().getBoolean("LSPECIALAppliedUnbreakable")) {
                NBTTagCompound tag = heldStack.getTagCompound();
                tag.setBoolean("Unbreakable", tag.getBoolean("LSPECIALWasUnbreakable"));
                tag.removeTag("LSPECIALAppliedUnbreakable");
                tag.removeTag("LSPECIALWasUnbreakable");
                tag.removeTag("LSPECIALWasUnbreakableRecorded");
            }
        }

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

                if (!state.modified
                        || state.modifiedStackRef != heldStack
                        || state.previousGunPerception != perceptionScore) {

                    int configs = gun.getConfigCount();
                    // restore the previous item once before applying changes to a newly held stack.
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

    private void modifyGuns(ItemStack heldStack, EntityPlayer player, GunConfig cfg, PlayerState state) {
        if (heldStack.getItem().equals(ModItems.gun_chemthrower) || heldStack.getItem().equals(ModItems.gun_flamer) || heldStack.getItem().equals(ModItems.gun_flamer_topaz) || heldStack.getItem().equals(ModItems.gun_drill)) {
            return;
        }

        Receiver[] receivers = cfg.getReceivers(heldStack);
        if (receivers == null || receivers.length == 0 || receivers[0] == null) return;
        LSPECIALMod.info("StatsLogic", "Applying gun config {} at index {}", cfg, state.gunIndex);

        if (LSPECIALConfig.intelligence && "INFINITE_AT_INTELLIGENCE_5".equalsIgnoreCase(LSPECIALConfig.gunWearMode) && inst.getPlayerScorePoints(player, inst.INTELLIGENCE_BOARD) >= 5) {
            ItemGunBaseNT.setWear(heldStack, state.gunIndex, 0);
        }

        state.modifiedReceiver = receivers[0];

        if (state.modifiedReceiver.getMagazine(heldStack) == null) return;
        Object ammoType = state.modifiedReceiver.getMagazine(heldStack).getType(heldStack, player.inventory);
        if (!(ammoType instanceof BulletConfig bConfig)) return;

        setOriginalValues(bConfig, state.modifiedReceiver, heldStack, state);

        if (!state.installedMagazine) {
            if (LSPECIALConfig.refundMagazines && LSPECIALConfig.luck && inst.getPlayerScorePoints(player, inst.LUCK_BOARD) > 0) { // if i have luck, install a magazine that can refund bullets
                state.modifiedReceiver.mag(new RefundMagazine(state.originalMagazine));

                state.installedMagazine = true;
            }
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

        // each akimbo/config receiver gets its own snapshot. Restoring only the final receiver
        // was the source of the old left/right gun corruption and cumulative damage changes.
        for (int i = state.gunStates.size() - 1; i >= 0; i--) {
            GunState gunState = state.gunStates.get(i);
            if (gunState.bulletConfig != null) {
                gunState.bulletConfig.armorPiercingPercent = gunState.armorPiercing;
                gunState.bulletConfig.armorThresholdNegation = gunState.armorThresholdNegation;
                gunState.bulletConfig.doesPenetrate = gunState.doesPenetrate;
                gunState.bulletConfig.headshotMult = gunState.headshotMultiplier;
            }
            gunState.receiver.dmg(gunState.damage);
            gunState.receiver.auto(gunState.refireOnHold);
            gunState.receiver.autoAfterDry(gunState.refireAfterDry);
            gunState.receiver.rounds(gunState.rounds);
            gunState.receiver.mag(gunState.magazine);
            if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemGunBaseNT) {
                ItemGunBaseNT.setWear(heldStack, gunState.index, gunState.wear);
            }
        }
        resetGunTracking(state);
    }

    private void resetGunTracking(PlayerState state) {
        state.gunStates.clear();
        state.gunIndex = 0;
        state.installedMagazine = false;
        state.modifiedStackRef = ItemStack.EMPTY;
        state.modifiedReceiver = null;
        state.modified = false;
        state.previousGunPerception = -1;
    }

    private void setOriginalValues(BulletConfig bCIn, Receiver rIn, ItemStack heldStack, PlayerState state) {
        state.originalWear = ItemGunBaseNT.getWear(heldStack, state.gunIndex);
        state.originalMagazine = rIn.getMagazine(heldStack);
        state.originalAmmoPiercing = bCIn.armorPiercingPercent;
        state.originalArmorThresholdNegation = bCIn.armorThresholdNegation;
        state.originalDamage = rIn.getBaseDamage(heldStack);
        state.originalProjectileAmount = rIn.getRoundsPerCycle(heldStack);
        state.gunStates.add(new GunState(state.gunIndex, rIn, bCIn, state.originalDamage,
                ((ReceiverStateAccess) rIn).lspecial$getNativeRefireOnHold(),
                ((ReceiverStateAccess) rIn).lspecial$getNativeRefireAfterDry(),
                bCIn.doesPenetrate, bCIn.headshotMult, state.originalProjectileAmount, state.originalMagazine,
                state.originalWear, state.originalAmmoPiercing, state.originalArmorThresholdNegation));
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

    private void synchronizePlayer(EntityPlayer player) {
        PlayerState state = playerStates.computeIfAbsent(
                player.getUniqueID(),
                id -> new PlayerState()
        );

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

        Collection<AttributeModifier> damageCollection = heldStack.isEmpty() ? null : heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());

        double baseHeldDamage = 0.0;

        if (damageCollection != null && !damageCollection.isEmpty()) {
            for (AttributeModifier modifier : damageCollection) {
                if (ATTACK_DAMAGE_UUID.equals(modifier.getID())) {
                    baseHeldDamage += modifier.getAmount();
                }
            }
        }

        double strengthBonus = LSPECIALConfig.strength ? expression(LSPECIALConfig.attackDamageFormula, "strength", inst.getPlayerScorePoints(player, inst.STRENGTH_BOARD)) : 0.0;

        double newDamage = baseHeldDamage + strengthBonus;
        AttributeModifier strengthModifier = strength.getModifier(ATTACK_DAMAGE_UUID);
        boolean hasStrengthModifier = strengthModifier != null;

        if (!LSPECIALConfig.strength) {
            if (hasStrengthModifier) {
                strength.removeModifier(ATTACK_DAMAGE_UUID);
            }
            state.previouStrengthDamage = Double.NaN;
        } else if (!hasStrengthModifier
                || Double.isNaN(state.previouStrengthDamage)
                || Math.abs(newDamage - state.previouStrengthDamage) > 0.000001D) {
            strength.removeModifier(ATTACK_DAMAGE_UUID);
            strength.applyModifier(new AttributeModifier(ATTACK_DAMAGE_UUID, nameIn, newDamage, 0));

            state.previouStrengthDamage = newDamage;
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

        boolean hasMovementSpeedModifier = movementSpeed.getModifier(MOVEMENT_SPEED_UUID) != null;

        if (agilityScore != state.previousAgility
                || state.previousAgilityEnabled != LSPECIALConfig.agility
                || (configuredPlayerSpeed != -1 && agilityScore != configuredPlayerSpeed)
                || (LSPECIALConfig.agility && !hasMovementSpeedModifier)
                || (!LSPECIALConfig.agility && hasMovementSpeedModifier)) {

            state.previousAgilityEnabled = LSPECIALConfig.agility;

            if (configuredPlayerSpeed > -1) {
                state.previousAgility = configuredPlayerSpeed;
            } else {
                state.previousAgility = agilityScore;
            }

            movementSpeed.removeModifier(MOVEMENT_SPEED_UUID);

            if (LSPECIALConfig.agility) {
                double agilityValue = configuredPlayerSpeed > -1 ? configuredPlayerSpeed : agilityScore;

                movementSpeed.applyModifier(new AttributeModifier(MOVEMENT_SPEED_UUID, nameIn, expression(LSPECIALConfig.agilityFormula, "agility", agilityValue), 1));
            }
        }
        MinecraftServer server = player.getServer();

        // fly speed
        float baseFlySpeed = 0.05F;
        float flySpeed = baseFlySpeed;

        if (LSPECIALConfig.agility && LSPECIALConfig.agilityFlyingSpeed) {
            int configuredFlySpeed = getPlayerFlySpeed(player);

            double agilityValue = configuredFlySpeed >= 0 ? configuredFlySpeed : agilityScore;

            double multiplierBonus = expression(LSPECIALConfig.agilityFlyingSpeedFormula, "agility", agilityValue);

            double minimum = Math.min(LSPECIALConfig.agilityFlyingSpeedMinimum, LSPECIALConfig.agilityFlyingSpeedMaximum);
            double maximum = Math.max(LSPECIALConfig.agilityFlyingSpeedMinimum, LSPECIALConfig.agilityFlyingSpeedMaximum);

            flySpeed = (float) Math.max(minimum, Math.min(maximum, baseFlySpeed * (1.0 + multiplierBonus)));
        }

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
        final List<GunState> gunStates = new ArrayList<>();
        int previousEndurance = -1;
        float previousMaxHealth = Float.NaN;
        int previousCharisma = -1;
        int previousAgility = -1;
        double previouStrengthDamage = Double.NaN;
        boolean previousEnduranceEnabled = true;
        boolean previousCharismaEnabled = true;
        boolean previousAgilityEnabled = true;
        ItemStack modifiedStackRef = ItemStack.EMPTY;
        Receiver modifiedReceiver;
        boolean modified;
        boolean installedMagazine;
        float originalDamage;
        int originalProjectileAmount;
        float originalAmmoPiercing;
        float originalArmorThresholdNegation;
        float previousFlySpeed = Float.NaN;
        float originalWear;
        IMagazine originalMagazine;
        ItemStack wearTrackedStack;
        float previousObservedWear = Float.NaN;
        int poweredArmorRechargeCooldown = 0;

        int gunIndex = -1;
        int previousGunPerception = -1;

        ItemStack loggedSednaGun = ItemStack.EMPTY;

        GunStateView getGunStateView(int index) {
            GunState gunState = gunStates.get(index);
            return new GunStateView(gunState.index, gunState.damage);
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
    private record GunState(int index,
                            Receiver receiver,
                            BulletConfig bulletConfig,
                            float damage,
                            boolean refireOnHold,
                            boolean refireAfterDry,
                            boolean doesPenetrate,
                            float headshotMultiplier,
                            int rounds,
                            IMagazine magazine,
                            float wear,
                            float armorPiercing,
                            float armorThresholdNegation) {
    }
}
