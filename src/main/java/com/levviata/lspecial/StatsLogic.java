package com.levviata.lspecial;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.fluid.FluidType;
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
import java.util.logging.Logger;

import static com.levviata.lspecial.LSPECIALMod.LOGGER;
import static com.levviata.lspecial.SPECIALScoreboard.inst;
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

    private int previousHealth = -1;
    private int previousCharisma = -1;
    private int previousAgility = -1;

    private ItemStack modifiedGun = ItemStack.EMPTY;
    private Receiver modifiedReceiver;
    private boolean modified;
    private boolean installedMagazine;
    private float originalDamage;
    private int originalDelay;
    private int originalProjectileAmount;
    private float originalAmmoPiercing;
    private float originalWear;
    private IMagazine originalMagazine;

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
        // todo make the new damage be properly shown on held items with the literal format (Original + Bonus Attack Damage), make config option to mix the numbers
        IAttributeInstance strength = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);

        if (!heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName()).isEmpty()) {
            Collection<AttributeModifier> damageCollection = heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());

            double damage = 1 + damageCollection.iterator().next().getAmount();

            strength.removeModifier(ATTACK_DAMAGE_UUID);

            double newDamage = damage + (LSPECIALConfig.strength ? expression(LSPECIALConfig.attackDamageFormula, "strength", inst.getStrengthScore().getScorePoints()) : 0);

            strength.applyModifier(new AttributeModifier(ATTACK_DAMAGE_UUID, nameIn, newDamage, 0));
        }

        // endurance
        // todo at level 5 make health scale player model size (dont touch the hitbox), x0.25 increase per one extra full heart after that level
        IAttributeInstance maxHealth = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (inst.getEnduranceScore().getScorePoints() != previousHealth || player.getMaxHealth() != previousHealth) {
            previousHealth = inst.getEnduranceScore().getScorePoints();

            maxHealth.removeModifier(MAX_HEALTH_UUID);

            if (LSPECIALConfig.endurance) {
                maxHealth.applyModifier(new AttributeModifier(MAX_HEALTH_UUID, nameIn, expression(LSPECIALConfig.enduranceFormula, "endurance", inst.getEnduranceScore().getScorePoints() * 2), 0));
            }
        }

        // charisma
        // todo make mobs turn neutral at level 5 and make villagers drop their prices in half,
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance armorToughness = player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
        if (inst.getCharismaScore().getScorePoints() != previousCharisma) {
            previousCharisma = inst.getCharismaScore().getScorePoints();

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
        if (inst.getAgilityScore().getScorePoints() != previousAgility || getPlayerSpeed(player) != -1 && inst.getAgilityScore().getScorePoints() != getPlayerSpeed(player)) {
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
            // todo fix fly speed
/*          this sucks, its either too fast or too slow
            float def = 0.05f;
            player.capabilities.setFlySpeed(def);
            // im not sure if this works, if it doesnt, maybe sync problems?
            if (getPlayerFlySpeed(player) > -1) {
                player.capabilities.setFlySpeed(player.capabilities.getFlySpeed() * (getPlayerFlySpeed(player) / 0.075f));
            } else {
                player.capabilities.setFlySpeed(player.capabilities.getFlySpeed() * (inst.getAgilityScore().getScorePoints() / 0.075f));
            }*/
        }
        //originalFlySpeed = player.capabilities.getFlySpeed();

        // perception, intelligence, and luck
        // todo make perception increase found loot by copying once non stackable items (in the found loot inventory) x2 at level 5 and x3 at level 10 with luck also at level 10. Above 15 now increase +1% armor threshold negation (BulletConfig)
        // todo Make luck reduce armor durability lose on chance (1 point = 0.025% chance, make it configurable), at level 5 armor with 1 durability should be unequipped once as a fail safe, at level 10, while pairing with perception, make a special loot table spawn inside found loot as well (only once though), make a list of common blocks (like or common loot table where these items shouldnt appear (configurable)

        if (LSPECIALConfig.intelligence && "INFINITE_AT_INTELLIGENCE_5".equalsIgnoreCase(LSPECIALConfig.gunWearMode) && inst.getIntelligenceScore().getScorePoints() >= 5 && heldStack.getItem() instanceof ItemGunBaseNT) {
            ItemGunBaseNT.setWear(heldStack , 0, 0);
        }

        if (heldStack.getItem() instanceof ItemGunBaseSedna) {
            // to my knowledge, no guns inherit this class
            if (loggedSednaGun.isEmpty() || !ItemStack.areItemStacksEqual(loggedSednaGun, heldStack)) {
                LOGGER.info("Your held gun inherits ItemGunBaseSedna.class which don't expose damage variables, can't modify it. Your gun is: {}", heldStack);

                loggedSednaGun = heldStack.copy();
            }
        }

        // todo i keep getting a bunch of errors about "cannot be cast to com.hbm.items.weapon.sedna.ItemGunBaseNT", since all ItemGunBaseNT are extending Item work from there. OR since this is new, it might definitely be because of the for loops in cleanGun(), that's the most likely abnormality! it keeps happening yeah remove it
        if (heldStack.getItem() != ItemStack.EMPTY.getItem() || heldStack.getItem() != Items.AIR) {
            if (LSPECIALConfig.modifyGuns && heldStack.getItem() instanceof ItemGunBaseNT) {
                if (!modified || !ItemStack.areItemStacksEqual(modifiedGun, heldStack)) {
                    // todo fix akimbo guns, the first gun increments in damage constantly and the second gun does not get an increase, second gun seems it gets it's damage increased but then its cleaned. Maybe this applies to other modifications, could be a deeper logical fallacy
                    ItemGunBaseNT gun = (ItemGunBaseNT) heldStack.getItem();
                    int configs = gun.getConfigCount();
                    // clean for this cycle
                    if (modified) {
                        // this for loop was recently added to try and fix the akimbo gun problem
                        for (gunIndex = 0; gunIndex < configs; gunIndex++) {
                            cleanGun(heldStack, player);
                            LOGGER.info("modifying {}", gunIndex);
                        }
                    }

                    for (gunIndex = 0; gunIndex < configs; gunIndex++) {
                        // modify guns using their unique gun config
                        // akimbo guns have two configs so this loop would trigger twice, modifying both
                        modifyGuns(heldStack, player, gun.getConfig(heldStack, gunIndex));
                        LOGGER.info("modifying {}", gunIndex);
                    }

                    modifiedGun = heldStack.copy();
                    modified = true;
                }
            } else if (modified) {
                ItemGunBaseNT gun = (ItemGunBaseNT) heldStack.getItem();
                int configs = gun.getConfigCount();
                //clean for next cycle
                // this for loop was recently added to try and fix the akimbo gun problem
                for (gunIndex = 0; gunIndex < configs; gunIndex++) {
                    cleanGun(heldStack, player);
                    LOGGER.info("modifying {}", gunIndex);
                }
            }
        }

        givePotions(player);
    }

    public void modifyGuns(ItemStack heldStack, EntityPlayer player, GunConfig cfg) {
        if (heldStack.getItem().equals(ModItems.gun_chemthrower)) {
            return;
        }

        Receiver[] receivers = cfg.getReceivers(heldStack);
        LOGGER.info("cfg is: {}, gunindex: {}", cfg, gunIndex);

        if (LSPECIALConfig.intelligence && "INFINITE_AT_INTELLIGENCE_5".equalsIgnoreCase(LSPECIALConfig.gunWearMode) && inst.getIntelligenceScore().getScorePoints() >= 5) {
            ItemGunBaseNT.setWear(heldStack, 0, 0);
        }

        modifiedReceiver = receivers[0];
        LOGGER.info("receivers: {}", receivers);

        // todo also check if getType() actually returns BulletConfig, otherwise return
        BulletConfig bConfig = (BulletConfig) modifiedReceiver.getMagazine(heldStack).getType(heldStack, player.inventory);

        setOriginalValues(bConfig, modifiedReceiver, heldStack);

        if (!installedMagazine) {
            if (LSPECIALConfig.refundMagazines && LSPECIALConfig.luck && inst.getLuckScore().getScorePoints() > 0) { // if i have luck, install a magazine that can refund bullets
                modifiedReceiver.mag(new RefundMagazine(originalMagazine));

                installedMagazine = true;
            }
        }

        processReceiver(modifiedReceiver, originalDamage, originalDelay, heldStack, player);
    }

    private void processReceiver(Receiver recIn, float ogDmg, int ogDelay, ItemStack heldStack, EntityPlayer player) {
        recIn.dmg((float) (ogDmg + (LSPECIALConfig.perception ? expression(LSPECIALConfig.perceptionGunDamageFormula, "perception", inst.getPerceptionScore().getScorePoints(), "originalDamage", (double)ogDmg) : 0)));

        BulletConfig bC = (BulletConfig) recIn.getMagazine(heldStack).getType(heldStack, player.inventory);
        ItemStack ammoStack;
        if (bC.ammo != null) {
            ItemStack c = bC.ammo.getStack();

            ammoStack = player.inventory.mainInventory.stream()
                    .filter(stack -> stack.getItem() == c.getItem())
                    .findFirst()
                    .orElse(ItemStack.EMPTY);
        } else {
            ammoStack = ItemStack.EMPTY;
        }

        int intelligence = LSPECIALConfig.intelligence ? inst.getIntelligenceScore().getScorePoints() : 0;
        int modifiedDelay = (int)Math.round(ogDelay - expression(LSPECIALConfig.bonusDelayFormula, "intelligence", intelligence, "originalDelay", (double)ogDelay));

        if (modifiedDelay > 0) {
            int formula = (int)Math.round(expression(LSPECIALConfig.buckshotDelayFormula, "intelligence", intelligence, "originalDelay", (double)ogDelay));
            if (LSPECIALConfig.buckshotDelayNerf && (/*recIn.getRefireOnHold(heldStackIn) ||*/ isBuckshotAmmo(ammoStack))) { // loaded with buckshots (usually shotguns)
                modifiedDelay = formula;
            }
        } else {
            modifiedDelay = 0;
        }
        recIn.delay(modifiedDelay);

        // NOTE: some of this perk are intended to be applied on top of existing weapon mods so take care into perk <-> weapon mod compatibility
        if (LSPECIALConfig.intelligence && LSPECIALConfig.pepperboxPerk && inst.getIntelligenceScore().getScorePoints() >= 5) { // lesser perk
            if (!heldStack.isEmpty()) {
                String rName = String.valueOf(heldStack.getItem().getRegistryName());
                if (LSPECIALConfig.pepperboxPerk && rName.equals("hbm:gun_pepperbox")) {
                    // todo make this be bound to second fire
                    recIn.rounds(6);
                }
                if (rName.equals("hbm:gun_light_revolver")) { // todo remove from the higher perk revolver list in the config too
                    // todo make it rapid fire on second fire, this means that the full mag should empty one shot at the time until its completely empty (like a cowboy rapid firing), cant be stopped by the player (don't let them switch guns too)
                }
                if (rName.equals("hbm:gun_tau")) {
                    // todo fix charged second fire dealing less that rapid fire, it should deal about x3 compared to rapid fire
                }
                if (rName.equals("hbm:gun_lag")) {
                    // todo make right click shift click run empty mag animation to shoot yourself, should trigger death as if it was a /kill command
                    // todo make it shoot three round in a row on second fire, should be %33 base damage per shot. the gun should also have a delay of 16 when shotting like that
                }
                if (rName.equals("hbm:gun_maresleg")) { // if case also for gun_liberator, gun_autoshotgun, gun_congolake, gun_am180
                    // todo make second fire scope like how The Heretic does it, the spread should be tighter too
                }
                if (rName.equals("hbm:gun_star_f")) { // also for the akimbo variety
                    // todo on crouch right click, make it use .50 bmg rounds (can be switched back), should not be affected by auto(true), increase delay to 1 sec
                }
                if (rName.equals("hbm:gun_flamer")) {
                    // todo make second fire shoot like Team Fortress 2 Phlogistinator
                }
            }
        }
        if (LSPECIALConfig.intelligence && LSPECIALConfig.revolverPerk && inst.getIntelligenceScore().getScorePoints() >= 15) { // higher perk
            recIn.auto(true);
            if (!heldStack.isEmpty()) {
                String rName = String.valueOf(heldStack.getItem().getRegistryName());
                if (LSPECIALConfig.revolverPerk && r.contains(rName) ) { // match to all revolvers, thought of madness combat revolvers doing hell damage, so it's in
                    // todo config for these bonuses
                    bC.doesPenetrate = true;
                    bC.setHeadshot(3);
                }
                if (LSPECIALConfig.pepperboxPerk && rName.equals("hbm:gun_pepperbox")) {
                    // todo on crouch right click make it use .44 magnum, can be switched back
                }
                if (rName.equals("hbm:gun_uzi")) { // also applies to akimbo variety
                    // todo on crouch right click make it use 5.56mm, can be switched back, delay set as 2
                }
                if (rName.equals("hbm:gun_g3")) { // also applies to akimbo variety
                    // todo on crouch right click make it use 10 gauge, can be switched back, delay set as 10
                }
                if (rName.equals("hbm:gun_laser_pistol_pew_pew")) {
                    // todo increase total capacity to 30
                }
                if (rName.equals("hbm:gun_minigun")) { // also for dual miniguns
                    // todo on second fire make it shoot lasers but with delay set as 4
                }
                if (rName.equals("hbm:gun_fatman")) {
                    // todo increase total capacity to 3 but set delay to 4 seconds (in ticks)
                }
                if (rName.equals("hbm:gun_stinger")) { // also for gun_quadro, gun_panzerschreck, and gun_missile_launcher
                    // todo make it deal no self damage
                }
                if (rName.equals("hbm:gun_carbine")) {
                    // todo reduce recoil by half, increase capacity to 30, second fire should shot one round at a time
                }
                if (rName.equals("hbm:gun_drill")) {
                    // todo increase total capacity by +4000mB, should be added onto possible Canister weapon mod upgrade
                }
                if (rName.equals("hbm:gun_flamer_topaz")) {
                    // todo make second fire shoot like Team Fortress 2 Dragon's Fury
                }
                if (rName.equals("hbm:gun_double_barrel")) {
                    // todo make a system that lets you grapple to entities on second fire, this should be like Doom Eternal's double barrel grapple upgrade.
                    //  On an entity there should be the gun crosshair plus a V mark, on fire the chain (a set of "o-o-o-o" texts based on the length from the player to the entity grabbed) will propulse the player towards the entity,
                    //  the player should be able to then fire their shotgun and kill the enemy
                }
                if (rName.equals("hbm:gun_henry_lincoln")) {
                    // todo make it fire three shots in a row on second fire with %33 base damage per shot
                }
                // balance
                if (rName.equals("hbm:gun_laser_pistol")) {
                    // todo set delay to 10
                }
            }
        }
    }

    public void cleanGun(ItemStack heldStack, EntityPlayer player) {
        if (heldStack.getItem().equals(ModItems.gun_chemthrower)) {
            return;
        }

        if (modifiedReceiver == null) {
            return;
        }

        BulletConfig bConfig = (BulletConfig) modifiedReceiver.getMagazine(heldStack).getType(heldStack, player.inventory);
        bConfig.armorPiercingPercent = originalAmmoPiercing;
        modifiedReceiver.dmg(originalDamage);
        modifiedReceiver.delay(originalDelay);
        modifiedReceiver.rounds(originalProjectileAmount);
        modifiedReceiver.mag(originalMagazine);
        ItemGunBaseNT.setWear(heldStack, gunIndex, originalWear);

        gunIndex = 0;
        installedMagazine = false;
        modifiedGun = ItemStack.EMPTY;
        modifiedReceiver = null;
        modified = false;
    }

    public void setOriginalValues(BulletConfig bCIn, Receiver rIn, ItemStack heldStack) {
        originalWear = ItemGunBaseNT.getWear(heldStack, gunIndex);
        originalMagazine = rIn.getMagazine(heldStack);
        originalAmmoPiercing = bCIn.armorPiercingPercent;
        originalDamage = rIn.getBaseDamage(heldStack);
        originalDelay = rIn.getDelayAfterFire(heldStack);
        originalProjectileAmount = rIn.getRoundsPerCycle(heldStack);
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

    private boolean isBuckshotAmmo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        for (String raw : LSPECIALConfig.buckshotGaugeList.split(",")) {
            String token = raw.trim();
            try {
                if (token.matches("\\d+") && stack.getItem() == ModItems.ammo_standard && stack.getMetadata() == Integer.parseInt(token)) {
                    return true;
                }

                String[] p = token.split(":");

                if (p.length == 3 && p[0].equals("hbm") && p[1].equals("ammo_secret") && stack.getItem() == ModItems.ammo_secret && stack.getMetadata() == Integer.parseInt(p[2])) {
                    return true;
                }
            } catch (NumberFormatException ignored) { }
        }
        return false;
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
}
