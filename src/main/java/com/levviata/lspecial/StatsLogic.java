package com.levviata.lspecial;

import com.hbm.inventory.RecipesCommon;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.HashMap;
import java.util.Map;

import static com.levviata.lspecial.LSPECIALMod.LOGGER;
import static com.levviata.lspecial.SPECIALScoreboard.inst;
import static com.levviata.lspecial.command.CommandSetFlySpeed.getPlayerFlySpeed;
import static com.levviata.lspecial.command.CommandSetSpeed.getPlayerSpeed;
import static com.levviata.lspecial.potion.PotionAmplifiedRegeneration.AMPLIFIED_REGENERATION_NAME;

public class StatsLogic {
    //vanilla uuids
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
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

    private int previousEndurance = -1;
    private float previousMaxHealth = Float.NaN;
    private int previousCharisma = -1;
    private int previousAgility = -1;
    private boolean previousEnduranceEnabled = true;
    private boolean previousCharismaEnabled = true;
    private boolean previousAgilityEnabled = true;

    private ItemStack modifiedStackRef = ItemStack.EMPTY;
    private Receiver modifiedReceiver;
    private boolean modified;
    private boolean installedMagazine;
    private float originalDamage;
    private int originalProjectileAmount;
    private float originalAmmoPiercing;
    private float originalArmorThresholdNegation;
    private float previousFlySpeed = Float.NaN;
    private float originalWear;
    private IMagazine originalMagazine;
    private final List<GunState> gunStates = new ArrayList<>();

    private ItemStack wearTrackedStack;
    private float previousObservedWear = Float.NaN;

    private int gunIndex = -1;

    private ItemStack loggedSednaGun = ItemStack.EMPTY;
    private static final List<UUID> uuids = new ArrayList<>();

    static List<Item> g = new ArrayList<>();
    static List<String> r = new ArrayList<>();

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

        inst.scoreboard = player.getEntityWorld().getScoreboard();

        // WARNING if the scores or scoreboards don't start, the mod is useless
        inst.startBoards();
        inst.startScores(player);

        ItemStack heldStack = player.getHeldItemMainhand();
        if (!heldStack.isEmpty() && heldStack.isItemStackDamageable()) {
            boolean perkActive = LSPECIALConfig.infiniteItemDurability && LSPECIALConfig.intelligence && inst.getIntelligenceScore().getScorePoints() >= 5;
            if (perkActive) {
                if (!heldStack.hasTagCompound()) heldStack.setTagCompound(new NBTTagCompound());
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
                tag.removeTag("LSPECIALAppliedUnbreakable"); tag.removeTag("LSPECIALWasUnbreakable"); tag.removeTag("LSPECIALWasUnbreakableRecorded");
            }
        }

        if (heldStack.getItem() instanceof ItemGunBaseNT) {
            applyGunWearPolicy(heldStack);
        } else {
            wearTrackedStack = null;
            previousObservedWear = Float.NaN;
        }

        // strength
        IAttributeInstance strength = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        Collection<AttributeModifier> damageCollection = heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());

        // the player's attack attribute already has its vanilla base value of 1.0.
        // this UUID replaces the held-item modifier, so only the item's modifier amount belongs here.
        double baseHeldDamage = 0.0;
        if (damageCollection != null && !damageCollection.isEmpty()) {
            baseHeldDamage = damageCollection.iterator().next().getAmount();
        }
        strength.removeModifier(ATTACK_DAMAGE_UUID);
        double strengthBonus = LSPECIALConfig.strength ? expression(LSPECIALConfig.attackDamageFormula, "strength", inst.getStrengthScore().getScorePoints()) : 0.0;
        strength.applyModifier(new AttributeModifier(ATTACK_DAMAGE_UUID, nameIn, baseHeldDamage + strengthBonus, 0));

        // endurance
        IAttributeInstance maxHealth = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        int enduranceScore = inst.getEnduranceScore().getScorePoints();
        if (enduranceScore != previousEndurance || previousEnduranceEnabled != LSPECIALConfig.endurance || Float.isNaN(previousMaxHealth) || Math.abs(player.getMaxHealth() - previousMaxHealth) > 0.001F) {
            previousEndurance = enduranceScore;
            maxHealth.removeModifier(MAX_HEALTH_UUID);
            if (LSPECIALConfig.endurance) {
                maxHealth.applyModifier(new AttributeModifier(MAX_HEALTH_UUID, nameIn,
                        expression(LSPECIALConfig.enduranceFormula, "endurance", enduranceScore * 2), 0));
            }
            previousMaxHealth = player.getMaxHealth();
            previousEnduranceEnabled = LSPECIALConfig.endurance;
        }

        // charisma
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance armorToughness = player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
        if (inst.getCharismaScore().getScorePoints() != previousCharisma || previousCharismaEnabled != LSPECIALConfig.charisma) {
            previousCharisma = inst.getCharismaScore().getScorePoints();
            previousCharismaEnabled = LSPECIALConfig.charisma;

            armor.removeModifier(ARMOR_UUID);
            armorToughness.removeModifier(ARMOR_TOUGHNESS_UUID);

            if (LSPECIALConfig.charisma && LSPECIALConfig.charismaBonus) {
                armor.applyModifier(new AttributeModifier(ARMOR_UUID, nameIn, expression(LSPECIALConfig.armorFormula, "charisma", inst.getCharismaScore().getScorePoints()), 0));
            }
            if (LSPECIALConfig.charisma && LSPECIALConfig.charismaBonus) {
                armorToughness.applyModifier(new AttributeModifier(ARMOR_TOUGHNESS_UUID, nameIn, expression(LSPECIALConfig.armorToughnessFormula, "charisma", inst.getCharismaScore().getScorePoints()), 0));
            }
        }

        // agility
        IAttributeInstance movementSpeed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (inst.getAgilityScore().getScorePoints() != previousAgility || previousAgilityEnabled != LSPECIALConfig.agility || getPlayerSpeed(player) != -1 && inst.getAgilityScore().getScorePoints() != getPlayerSpeed(player)) {
            previousAgilityEnabled = LSPECIALConfig.agility;
            if (getPlayerSpeed(player) > -1) {
                previousAgility = getPlayerSpeed(player);
            } else {
                previousAgility = inst.getAgilityScore().getScorePoints();
            }

            movementSpeed.removeModifier(MOVEMENT_SPEED_UUID);

            // operationIn: multiplicative
            // 7.5% increase per point up to 75%
            if (LSPECIALConfig.agility) {
                double agilityValue = getPlayerSpeed(player) > -1 ? getPlayerSpeed(player) : inst.getAgilityScore().getScorePoints();
                movementSpeed.applyModifier(new AttributeModifier(MOVEMENT_SPEED_UUID, nameIn, expression(LSPECIALConfig.agilityFormula, "agility", agilityValue), 1));
            }

        }

        // flying speed is based on vanilla's 0.05 capability speed and is recalculated, never compounded.
        float baseFlySpeed = 0.05F;
        float flySpeed = baseFlySpeed;
        if (LSPECIALConfig.agility && LSPECIALConfig.agilityFlyingSpeed) {
            int configuredFlySpeed = getPlayerFlySpeed(player);
            double agilityValue = configuredFlySpeed >= 0 ? configuredFlySpeed : inst.getAgilityScore().getScorePoints();
            double multiplierBonus = expression(LSPECIALConfig.agilityFlyingSpeedFormula, "agility", agilityValue);
            double minimum = Math.min(LSPECIALConfig.agilityFlyingSpeedMinimum, LSPECIALConfig.agilityFlyingSpeedMaximum);
            double maximum = Math.max(LSPECIALConfig.agilityFlyingSpeedMinimum, LSPECIALConfig.agilityFlyingSpeedMaximum);
            flySpeed = (float)Math.max(minimum, Math.min(maximum, baseFlySpeed * (1.0 + multiplierBonus)));
        }
        if (Float.isNaN(previousFlySpeed) || Math.abs(player.capabilities.getFlySpeed() - flySpeed) > 0.0001F) {
            player.capabilities.setFlySpeed(flySpeed);
            previousFlySpeed = flySpeed;
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                player.sendPlayerAbilities();
            }
        }

        // perception, intelligence, and luck

        if (LSPECIALConfig.intelligence && "INFINITE_AT_INTELLIGENCE_5".equalsIgnoreCase(LSPECIALConfig.gunWearMode)
                && inst.getIntelligenceScore().getScorePoints() >= 5 && heldStack.getItem() instanceof ItemGunBaseNT) {
            ItemGunBaseNT gun = (ItemGunBaseNT) heldStack.getItem();
            for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
                ItemGunBaseNT.setWear(heldStack, configIndex, 0);
            }
        }

        if (heldStack.getItem() instanceof ItemGunBaseSedna) {
            // to my knowledge, no guns inherit this class
            if (loggedSednaGun.isEmpty() || !ItemStack.areItemStacksEqual(loggedSednaGun, heldStack)) {
                LSPECIALMod.info("Your held gun inherits ItemGunBaseSedna.class which don't expose damage variables, can't modify it. Your gun is: {}", heldStack);

                loggedSednaGun = heldStack.copy();
            }
        }

        // restore modifications as soon as the selected stack changes, becomes empty, or the feature is disabled.
        if (modified && (heldStack.isEmpty() || !LSPECIALConfig.modifyGuns
                || !(heldStack.getItem() instanceof ItemGunBaseNT)
                || modifiedStackRef != heldStack)) {
            cleanGun(modifiedStackRef);
        }

        if (!heldStack.isEmpty() && heldStack.getItem() != Items.AIR) {
            if (LSPECIALConfig.modifyGuns && heldStack.getItem() instanceof ItemGunBaseNT) {
                if (!modified || modifiedStackRef != heldStack) {
                    ItemGunBaseNT gun = (ItemGunBaseNT) heldStack.getItem();
                    int configs = gun.getConfigCount();
                    // restore the previous item once before applying changes to a newly held stack.
                    if (modified) {
                        cleanGun(modifiedStackRef);
                    }

                    for (gunIndex = 0; gunIndex < configs; gunIndex++) {
                        // modify guns using their unique gun config
                        // akimbo guns have two configs so this loop would trigger twice, modifying both
                        modifyGuns(heldStack, player, gun.getConfig(heldStack, gunIndex));
                    }

                    modifiedStackRef = heldStack;
                    modified = true;
                }
            } else if (modified) {
                // the held item changed or gun modification was disabled. Never cast a non-gun item.
                cleanGun(modifiedStackRef);
            }
        }

        givePotions(player);
    }

    public void modifyGuns(ItemStack heldStack, EntityPlayer player, GunConfig cfg) {
        if (heldStack.getItem().equals(ModItems.gun_chemthrower)) {
            return;
        }

        Receiver[] receivers = cfg.getReceivers(heldStack);
        if (receivers == null || receivers.length == 0 || receivers[0] == null) return;
        LOGGER.debug("Applying gun config {} at index {}", cfg, gunIndex);

        if (LSPECIALConfig.intelligence && "INFINITE_AT_INTELLIGENCE_5".equalsIgnoreCase(LSPECIALConfig.gunWearMode) && inst.getIntelligenceScore().getScorePoints() >= 5) {
            ItemGunBaseNT.setWear(heldStack, gunIndex, 0);
        }

        modifiedReceiver = receivers[0];

        if (modifiedReceiver.getMagazine(heldStack) == null) return;
        Object ammoType = modifiedReceiver.getMagazine(heldStack).getType(heldStack, player.inventory);
        if (!(ammoType instanceof BulletConfig)) return;
        BulletConfig bConfig = (BulletConfig) ammoType;

        setOriginalValues(bConfig, modifiedReceiver, heldStack);

        if (!installedMagazine) {
            if (LSPECIALConfig.refundMagazines && LSPECIALConfig.luck && inst.getLuckScore().getScorePoints() > 0) { // if i have luck, install a magazine that can refund bullets
                modifiedReceiver.mag(new RefundMagazine(originalMagazine));

                installedMagazine = true;
            }
        }

        processReceiver(modifiedReceiver, originalDamage, heldStack, player);
    }

    private void processReceiver(Receiver recIn, float ogDmg, ItemStack heldStack, EntityPlayer player) {
        recIn.dmg((float) (ogDmg + (LSPECIALConfig.perception ? expression(LSPECIALConfig.perceptionGunDamageFormula, "perception", inst.getPerceptionScore().getScorePoints(), "originalDamage", (double)ogDmg) : 0)));

        if (recIn.getMagazine(heldStack) == null) {
            return;
        }

        Object ammoType = recIn.getMagazine(heldStack).getType(heldStack, player.inventory);
        if (!(ammoType instanceof BulletConfig)) {
            return;
        }

        BulletConfig bC = (BulletConfig) ammoType;
        if (LSPECIALConfig.perception && LSPECIALConfig.perceptionArmorThresholdNegation && inst.getPerceptionScore().getScorePoints() > LSPECIALConfig.perceptionArmorThresholdNegationLevel) {
            double addedNegation = (inst.getPerceptionScore().getScorePoints() - LSPECIALConfig.perceptionArmorThresholdNegationLevel) * LSPECIALConfig.perceptionArmorThresholdNegationPerPoint;
            bC.armorThresholdNegation = (float)Math.min(1.0, originalArmorThresholdNegation + addedNegation);
        }
        if (LSPECIALConfig.intelligence && LSPECIALConfig.pepperboxPerk && inst.getIntelligenceScore().getScorePoints() >= 5 && !heldStack.isEmpty()) {
            String itemID = String.valueOf(heldStack.getItem().getRegistryName());
            if (LSPECIALConfig.pepperboxSixShotPerk && "hbm:gun_pepperbox".equals(itemID)) {
                recIn.rounds(6);
            }
        }

        if (LSPECIALConfig.intelligence && LSPECIALConfig.revolverPerk && inst.getIntelligenceScore().getScorePoints() >= 15 && !heldStack.isEmpty()) {
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

    public void cleanGun(ItemStack heldStack) {
        if (heldStack == null || (!heldStack.isEmpty() && heldStack.getItem().equals(ModItems.gun_chemthrower))) {
            if (gunStates.isEmpty()) resetGunTracking();
            return;
        }

        // each akimbo/config receiver gets its own snapshot. Restoring only the final receiver
        // was the source of the old left/right gun corruption and cumulative damage changes.
        for (int i = gunStates.size() - 1; i >= 0; i--) {
            GunState state = gunStates.get(i);
            if (state.bulletConfig != null) {
                state.bulletConfig.armorPiercingPercent = state.armorPiercing;
                state.bulletConfig.armorThresholdNegation = state.armorThresholdNegation;
                state.bulletConfig.doesPenetrate = state.doesPenetrate;
                state.bulletConfig.headshotMult = state.headshotMultiplier;
            }
            state.receiver.dmg(state.damage);
            state.receiver.auto(state.refireOnHold);
            state.receiver.autoAfterDry(state.refireAfterDry);
            state.receiver.rounds(state.rounds);
            state.receiver.mag(state.magazine);
            if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemGunBaseNT) {
                ItemGunBaseNT.setWear(heldStack, state.index, state.wear);
            }
        }
        resetGunTracking();
    }

    private void resetGunTracking() {
        gunStates.clear();
        gunIndex = 0;
        installedMagazine = false;
        modifiedStackRef = ItemStack.EMPTY;
        modifiedReceiver = null;
        modified = false;
    }

    public void setOriginalValues(BulletConfig bCIn, Receiver rIn, ItemStack heldStack) {
        originalWear = ItemGunBaseNT.getWear(heldStack, gunIndex);
        originalMagazine = rIn.getMagazine(heldStack);
        originalAmmoPiercing = bCIn.armorPiercingPercent;
        originalArmorThresholdNegation = bCIn.armorThresholdNegation;
        originalDamage = rIn.getBaseDamage(heldStack);
        originalProjectileAmount = rIn.getRoundsPerCycle(heldStack);
        gunStates.add(new GunState(gunIndex, rIn, bCIn, originalDamage,
                ((ReceiverStateAccess) rIn).lspecial$getNativeRefireOnHold(),
                ((ReceiverStateAccess) rIn).lspecial$getNativeRefireAfterDry(),
                bCIn.doesPenetrate, bCIn.headshotMult, originalProjectileAmount, originalMagazine,
                originalWear, originalAmmoPiercing, originalArmorThresholdNegation));
    }

    public void givePotions(EntityPlayer player) {
        if (LSPECIALConfig.amplifiedRegeneration && LSPECIALConfig.enduranceBonus && inst.getEnduranceBonusScore().getScorePoints() == 1) {
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
                            player.addPotionEffect(new PotionEffect(b, ae.getDuration(), cAmplifier + (int)LSPECIALConfig.regenAmplifierBonus));
                        }
                    }
                } else if (!player.isPotionActive(b)) {
                    player.addPotionEffect(new PotionEffect(b, LSPECIALConfig.potionTimeTicks, Math.max(0, LSPECIALConfig.endurancePotionAmplifier)));
                }
            }
        }

        if (LSPECIALConfig.charismaBonus && inst.getCharismaBonusScore().getScorePoints() == 1) {
            if (Potion.getPotionFromResourceLocation("resistance") != null && !player.isPotionActive(Potion.getPotionFromResourceLocation("resistance"))) {
                player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("resistance"), LSPECIALConfig.potionTimeTicks, LSPECIALConfig.charismaPotionAmplifier));
            }
        }
    }

    private void applyGunWearPolicy(ItemStack stack) {
        String mode = LSPECIALConfig.gunWearMode;

        if ("INFINITE_AT_INTELLIGENCE_5".equalsIgnoreCase(mode) || !LSPECIALConfig.intelligence) {
            return;
        }

        int intelligence = inst.getIntelligenceScore().getScorePoints();
        if (intelligence <= 0) {
            return;
        }

        float current = ItemGunBaseNT.getWear(stack, 0);

        if (wearTrackedStack != stack || Float.isNaN(previousObservedWear)) {
            wearTrackedStack = stack;
            previousObservedWear = current;
            return;
        }

        if (current > previousObservedWear) {
            double reduction;

            if ("PERCENT_REDUCTION".equalsIgnoreCase(mode)) {
                reduction = intelligence * LSPECIALConfig.gunWearReductionPerIntelligence;
            } else {
                Map<String, Double> vars = new HashMap<>();
                vars.put("intelligence", (double) intelligence);
                vars.put("originalWear", (double) previousObservedWear);

                reduction = LSPECIALExpression.eval(LSPECIALConfig.gunWearFormula, vars, 0.0);
            }

            reduction = Math.max(0.0, Math.min(1.0, reduction));

            float adjusted = (float) (previousObservedWear + (current - previousObservedWear) * (1.0 - reduction));

            ItemGunBaseNT.setWear(stack, 0, adjusted);
            previousObservedWear = adjusted;
        } else {
            previousObservedWear = current;
        }
    }

    private static double expression(String formula, String key, double value) {
        return expression(formula, key, value, null, 0);
    }

    private static double expression(String formula, String key, double value, String key2, double value2) {
        Map<String, Double> vars = new HashMap<>(); vars.put(key, value); if (key2 != null) vars.put(key2, value2);
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

    private static final class GunState {
        private final int index;
        private final Receiver receiver;
        private final BulletConfig bulletConfig;
        private final float damage;
        private final boolean refireOnHold;
        private final boolean refireAfterDry;
        private final boolean doesPenetrate;
        private final float headshotMultiplier;
        private final int rounds;
        private final IMagazine magazine;
        private final float wear;
        private final float armorPiercing;
        private final float armorThresholdNegation;

        private GunState(int index, Receiver receiver, BulletConfig bulletConfig, float damage,
                         boolean refireOnHold, boolean refireAfterDry, boolean doesPenetrate,
                         float headshotMultiplier, int rounds, IMagazine magazine, float wear,
                         float armorPiercing, float armorThresholdNegation) {
            this.index = index;
            this.receiver = receiver;
            this.bulletConfig = bulletConfig;
            this.damage = damage;
            this.refireOnHold = refireOnHold;
            this.refireAfterDry = refireAfterDry;
            this.doesPenetrate = doesPenetrate;
            this.headshotMultiplier = headshotMultiplier;
            this.rounds = rounds;
            this.magazine = magazine;
            this.wear = wear;
            this.armorPiercing = armorPiercing;
            this.armorThresholdNegation = armorThresholdNegation;
        }
    }
}
