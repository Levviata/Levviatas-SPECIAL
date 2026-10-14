package com.levviata.lspecial;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class LSPECIALConfig {

    private LSPECIALConfig() {
    }

    public static Configuration config;

    private static final String STATS = "1_special_stats";
    private static final String BOB = "2_bobbleheads_consumption";
    private static final String GUNS = "3_guns_ammo";
    private static final String POTIONS = "4_potions_effects";
    private static final String RECIPES = "5_recipes_ore_dictionary";
    private static final String MESSAGES = "6_gameplay_messages";
    private static final String CLIENT = "7_client";
    private static final String COMMANDS = "8_commands";
    private static final String ADV = "9_advanced";
    private static final String PERKS = "10_perks_gui";

    public static boolean strength = true;
    public static boolean perception = true;
    public static boolean endurance = true;
    public static boolean charisma = true;
    public static boolean intelligence = true;
    public static boolean agility = true;
    public static boolean luck = true;
    public static boolean statLimit = true;
    public static boolean hideDisabledStats = true;

    public static int statsPerBobblehead = 1;
    public static int potionTimeTicks = 115;
    public static int charismaPotionAmplifier = 0;
    public static int endurancePotionAmplifier = 1;
    //public static int bobbleheadusesPerConsumption = 1;

    public static boolean infiniteItemDurability = true;
    public static boolean charismaBonus = true;
    public static boolean enduranceBonus = true;
    public static boolean amplifiedRegeneration = true;
    public static boolean respectHigherRegenAmplifier = false;
    public static boolean modifyGuns = true;
    public static boolean buckshotDelayNerf = true;
    public static boolean revolverPerk = true;
    public static boolean pepperboxPerk = true;
    public static boolean lightRevolverRapidFirePerk = true;
    public static int lightRevolverRapidFireTicks = 20;
    public static boolean refundMagazines = true;
    public static boolean bobbleheadRename = true;
    public static boolean hotbarMessages = true;
    public static boolean variedAugLimitMessages = true;
    public static boolean toggleSpecial = true;
    public static boolean showSystemStatusInfo = true;
    public static boolean showRandomFooter = true;
    public static boolean showLSplashText = true;
    public static boolean showPerkScreens = true;
    public static boolean showPerkTierHint = true;
    public static boolean enduranceModelScaling = true;
    public static double enduranceModelScalePerHeart = 0.25;
    public static double enduranceModelScaleThresholdHearts = 10.0;
    public static boolean strengthTooltip = true;
    public static boolean agilityFlyingSpeed = true;
    public static String agilityFlyingSpeedFormula = "agility * 0.075";
    public static double agilityFlyingSpeedMinimum = 0.05;
    public static double agilityFlyingSpeedMaximum = 0.25;
    public static boolean charismaNeutralMobs = true;
    public static int charismaNeutralLevel = 5;
    public static String charismaNeutralEntityList = "minecraft:zombie,minecraft:skeleton,minecraft:creeper,minecraft:spider,minecraft:cave_spider,minecraft:enderman,minecraft:witch,minecraft:slime,minecraft:ghast,minecraft:blaze,minecraft:magma_cube,minecraft:silverfish,minecraft:guardian,minecraft:shulker,minecraft:vex,minecraft:vindication_illager,minecraft:evocation_illager,minecraft:zombie_pigman,minecraft:ender_dragon,minecraft:wither";
    public static boolean charismaVillagerDiscount = true;
    public static int charismaVillagerDiscountLevel = 5;
    public static String charismaVillagerDiscountFormula = "min(charisma * 0.05, 0.75)";
    public static boolean luckArmorDurabilityProtection = true;
    public static double luckArmorDurabilityChancePerPoint = 0.00025;
    public static double luckArmorDurabilityChanceCap = 0.05;
    public static int luckArmorFailsafeLevel = 5;
    public static boolean perceptionArmorThresholdNegation = true;
    public static double perceptionArmorThresholdNegationPerPoint = 0.01;
    public static int perceptionArmorThresholdNegationLevel = 15;
    public static boolean pepperboxSixShotPerk = true;
    public static boolean revolverPenetrationPerk = true;
    public static float revolverHeadshotMultiplier = 3.0f;
    public static boolean laserPistolDelayBalance = true;
    public static int laserPistolDelayTicks = 10;
    public static boolean fatmanHigherPerk = true;
    public static boolean fatmanHigherPerkDelay = true;
    public static int fatmanHigherPerkDelayTicks = 80;
    public static int fatmanMagazineCapacity = 3;
    public static boolean laserPewPewHigherPerk = true;
    public static int laserPewPewMagazineCapacity = 30;
    public static boolean drillHigherPerkCapacity = true;
    public static int drillAdditionalCapacityMb = 4000;
    public static boolean weaponSecondaryAmmoCycling = true;
    public static boolean hereticStyleScoping = true;
    public static boolean minigunLaserModePerk = true;
    public static int minigunLaserModeDelayTicks = 4;
    public static int starFFiftyBmgDelayTicks = 20;
    public static int uzi556DelayTicks = 2;
    public static int g3TenGaugeDelayTicks = 10;
    public static boolean lagSecondarySelfKill = true;
    public static boolean secondaryFireToggle = true;
    public static int starFCapacity = 30;
    public static boolean doubleBarrelGrapple = true;
    public static boolean doubleBarrelGrappleVisuals = true;
    public static int doubleBarrelGrappleVisualTicks = 12;
    public static double doubleBarrelGrappleRange = 28.0;
    public static double doubleBarrelGrapplePullSpeed = 1.35;
    public static boolean phlogistinatorSustainedFirePerk = true;
    public static int phlogistinatorSustainedFireLevel = 5;
    public static int phlogistinatorFireDelayTicks = 1;
    public static boolean dragonsFuryDirectHitPerk = true;
    public static int dragonsFuryDirectHitLevel = 15;
    public static double dragonsFuryDirectHitMultiplier = 1.5;
    public static int dragonsFuryBurstDelayTicks = 10;
    public static double dragonsFuryBurstRange = 3.0;
    public static double phlogSecondaryRange = 5.0;
    public static double phlogSecondaryWidth = 2.0;
    public static boolean tauSecondaryDamageBonus = true;
    public static double tauSecondaryDamageMultiplier = 6.0;
    public static boolean lagSecondaryBurst = true;
    public static int lagSecondaryBurstDelay = 16;
    public static double burstProjectileDamageFraction = 0.3333333333333333;
    public static boolean scopedSecondarySpread = true;
    public static double scopedSpreadMultiplier = 0.5;
    public static boolean henryLincolnSecondaryBurst = true;
    public static boolean AMATQuackosRailgun = true;
    public static boolean stingerSelfDamageProtection = true;
    public static boolean carbineHigherPerkCapacity = true;
    public static int carbineMagazineCapacity = 30;
    public static boolean carbineSemiAutoPerk = true;
    public static double carbineRecoilMultiplier = 0.5;
    public static boolean perceptionLootDuplication = true;
    public static int perceptionLootDuplicateLevel = 5;
    public static int perceptionLootTripleLevel = 10;
    public static boolean luckRareLootBonus = true;
    public static int luckRareLootLevel = 15;
    public static double luckRareLootChancePerPoint = 0.01;
    public static double luckRareLootChanceCap = 0.25;
    public static String luckRareLootTablePattern = "rare";

    public static String systemStatusText = "SYSTEM STATUS";
    public static String neuralLinkText = "LEVVIATA NEURA-LINK  //  ACTIVE";
    public static String randomFooterMessages = "YOU ARE LIKELY NOT SPECIAL|JET FUEL CAN MELT STEEL BEAMS|VAMPIRES ARE NICE|I KNOW WHERE YOU LIVE|LEVVIATAINC|WHOS TIAMAT|SATAN IS A NICE GUY|WHOS LEVIATHAN|WHEN YOU SEE IT|ITS ALL A DREAM AND YOU ARE BLIND|BLESSED ARE THE STRONG|CURSED ARE THE WEAK|BE OBEDIENT IF YOU ARE WEAK|CONTROL IF YOU ARE STRONG|NOT EVERYONE IS EQUAL|TAX THE CHURCHES|AI IS FREE SLAVERY";
    public static boolean setSpeedCommand = true;
    public static boolean setFlySpeedCommand = true;

    //public static string bobbleheadConsumption = "ON_USE";
    // bug: cant put '' in cfg strings or it will make everything a string
    public static String limitMessage = "I have reached my limit, I cant consume more.";
    public static String augLimitMessage = "Augmented SPECIAL limit by one";
    public static String augUnder5 = "Augmented my SPECIAL limit by one, cool";
    public static String aug5 = "Augmented SPECIAL limit by one";
    public static String aug10 = "SPECIAL limit++";
    public static String aug20 = "Limit++";

    public static double regenAmplifierBonus = 1.0;
    public static double gunWearReductionPerIntelligence = 0.05;

    public static String agilityFormula = "agility * 0.075";
    public static String enduranceFormula = "endurance";
    public static String attackDamageFormula = "strength";
    public static String armorFormula = "charisma * 2.0";
    public static String armorToughnessFormula = "charisma * 0.5";
    public static String perceptionGunDamageFormula = "perception";
    public static String bonusDelayFormula = "floor(intelligence / 10)";
    public static String buckshotDelayFormula = "originalDelay - floor(intelligence / 100)";
    public static String gunWearMode = "INFINITE_AT_INTELLIGENCE_5";
    public static String gunWearFormula = "min(intelligence * 0.05, 0.75)";
    public static String refundAmmoFormula = "min(luck * 0.05, 0.5)";

    public static String buckshotGaugeList = "41,42,43,44,45,46,47,48,49,78,79,80,81,84,hbm:ammo_secret:3";
    public static String revolverList = "hbm:gun_flaregun,hbm:gun_heavy_revolver,hbm:gun_light_revolver_atlas";

    public static boolean[] recipeEnabled = {true, true, true, true, true, true, true};

    public static String oreBobblehead = "bobblehead";
    public static String oreAppleLead = "appleLead";
    public static String oreAmmo = "ammo";
    public static String oreLuckAccessory = "luckAccessory";
    public static String oreLuckyGem = "luckyGem";

    public static String oreBobbleheadEntries = "hbm:bobblehead:8-28";
    public static String oreAppleLeadEntries = "hbm:apple_lead:0-2";
    public static String oreAmmoEntries = "hbm:ammo_standard:0-94";
    public static String oreLuckAccessoryEntries = "hbm:bandaid,hbm:serum,hbm:spider_milk,hbm:heart_piece,hbm:wd40";
    public static String oreLuckyGemEntries = "minecraft:emerald,minecraft:diamond";

    public static String[] recipeIngredients = {
            "bobblehead,hbm:schnitzel_vegan,appleLead,hbm:ingot_u235",
            "bobblehead,hbm:weapon_mod_special:1,hbm:weapon_mod_special:6,ammo",
            "bobblehead,hbm:med_bag,hbm:crackpipe,potion:healing|strong_healing",
            "bobblehead,hbm:cobalt_helmet,hbm:steel_helmet,hbm:titanium_helmet",
            "bobblehead,hbm:part_mechanism:34,hbm:circuit:9,hbm:gun_kit_2",
            "bobblehead,hbm:cotton_candy,potion:swiftness|long_swiftness|strong_swiftness,minecraft:sugar",
            "bobblehead,ammo,luckyGem,luckAccessory"
    };

    public static void init(File file) {
        config = new Configuration(file);
        load();
    }

    public static void load() {
        if (config == null) {
            return;
        }

        config.load();
        config.getCategory(STATS).remove("strengthTooltipFormat");
        config.getCategory(GUNS).remove("phlogSecondaryDelayTicks");

        // SPECIAL stats
        strength = b(STATS, "strength", true, "Enable Strength");
        perception = b(STATS, "perception", true, "Enable Perception");
        endurance = b(STATS, "endurance", true, "Enable Endurance");
        charisma = b(STATS, "charisma", true, "Enable Charisma");
        intelligence = b(STATS, "intelligence", true, "Enable Intelligence");
        agility = b(STATS, "agility", true, "Enable Agility");
        luck = b(STATS, "luck", true, "Enable Luck");
        statLimit = b(STATS, "statLimit", true, "Enforce stat limit");
        hideDisabledStats = b(STATS, "hideDisabledStats", true, "Hide disabled stats in stats screen, false shows them as strike-through");
        charismaBonus = b(STATS, "charismaBonus", true, "Enable Charisma bonus");
        enduranceBonus = b(STATS, "enduranceBonus", true, "Enable Endurance bonus");
        infiniteItemDurability = b(STATS, "infiniteItemDurability", true, "Intelligence level 5 perk. Held damageable items do not lose durability");

       // bobblehead consumption
        statsPerBobblehead = i(BOB, "statsPerBobblehead", 1, 1, Integer.MAX_VALUE, "Points per bobblehead");
        bobbleheadRename = b(BOB, "rename", true, "Give bobbleheads their stats in their display name");

        // guns and ammunition
        refundAmmoFormula = s(GUNS, "refundAmmoFormula", refundAmmoFormula, "Validated expression; variable: luck");
        modifyGuns = b(GUNS, "modifyGuns", true, "Master switch for gun modifications. Whether to modify Nuclear Tech guns at all.");
        buckshotDelayNerf = b(GUNS, "buckshotDelayNerf", true, "Use the nerfed delay formula for guns that use buckshot (shotguns)");
        revolverPerk = b(GUNS, "revolverHigherPerk", true, "Enable Intelligence revolver perk");
        pepperboxPerk = b(GUNS, "pepperboxLesserPerk", true, "Enable Pepperbox lesser perk");
        lightRevolverRapidFirePerk = b(GUNS, "lightRevolverRapidFirePerk", true, "Intelligence level 5 perk. Secondary fire triggers a rapid-fire burst and locks the player to that exact hotbar stack");
        lightRevolverRapidFireTicks = i(GUNS, "lightRevolverRapidFireTicks", 20, 1, 200, "Duration of the light revolver rapid-fire burst in ticks");
        refundMagazines = b(GUNS, "refundMagazines", true, "Enable magazine refunds");
        gunWearMode = s(GUNS, "gunWearMode", "INFINITE_AT_INTELLIGENCE_5", "INFINITE_AT_INTELLIGENCE_5, PERCENT_REDUCTION, FORMULA");
        if (!gunWearMode.equals("INFINITE_AT_INTELLIGENCE_5") && !gunWearMode.equals("PERCENT_REDUCTION") && !gunWearMode.equals("FORMULA")) {
            gunWearMode = "INFINITE_AT_INTELLIGENCE_5";
        }
        gunWearReductionPerIntelligence = d(GUNS, "gunWearReductionPerIntelligence", 0.05, 0, 1, "Wear reduction per Intelligence point");
        buckshotGaugeList = s(GUNS, "buckshotGaugeList", buckshotGaugeList, "Comma separated list of hbm:ammo_standard but ONLY with the Metadata. Also supports hbm:ammo_secret:Metadata");
        revolverList = s(GUNS, "revolverList", revolverList, "Comma separated list of ModID:ItemID");

        // potions and effects
        amplifiedRegeneration = b(POTIONS, "amplifiedRegeneration", true, "Endurance level 5 perk. Enable amplified regeneration.");
        respectHigherRegenAmplifier = b(POTIONS, "respectHigherRegenAmplifier", false, "Whether to amplify existing regeneration or not. Might fix issues with other mods");
        regenAmplifierBonus = d(POTIONS, "regenAmplifierBonus", 1.0, 0, 255, "Amplifier bonus");
        potionTimeTicks = i(POTIONS, "potionTimeTicks", 115, 0, 720000, "Potion duration in ticks. Default value is 5~ seconds");
        charismaPotionAmplifier = i(POTIONS, "charismaPotionAmplifier", 0, 0, 255, "Resistance amplifier");
        endurancePotionAmplifier = i(POTIONS, "endurancePotionAmplifier", 1, 0, 255, "Regeneration amplifier bonus");

        // gameplay messages
        hotbarMessages = b(MESSAGES, "hotbarMessages", true, "Show hotbar messages");
        limitMessage = s(MESSAGES, "limitMessage", limitMessage, "Stat limit reached message");
        variedAugLimitMessages = b(MESSAGES, "variedAugLimitMessages", true, "Use varied messages when increment stat limit");
        augLimitMessage = s(MESSAGES, "augLimitMessage", "Augmented SPECIAL limit by one", "Single message with no variety");
        augUnder5 = s(MESSAGES, "augUnder5", augUnder5, "Triggers under 5 stat limit increases");
        aug5 = s(MESSAGES, "aug5", aug5, "Triggers above 5 stat limit increases");
        aug10 = s(MESSAGES, "aug10", aug10, "Triggers above 10 limit increases");
        aug20 = s(MESSAGES, "aug20", aug20, "Triggers above 20 increases");

        // client interface and commands
        toggleSpecial = b(CLIENT, "toggleSpecial", true, "Enable toggleSpecial keybind");
        showSystemStatusInfo = b(CLIENT, "showSystemStatusInfo", true, "Show the SYSTEM STATUS and 'Neura-Link' information in the stats screen");
        showRandomFooter = b(CLIENT, "showRandomFooter", true, "Show the randomized footer in the stats screen");
        showLSplashText = b(CLIENT, "showLSplashText", true, "Allow LSPECIAL custom main-menu splashes");
        enduranceModelScaling = b(STATS, "enduranceModelScaling", true, "Scale the rendered player model from Endurance health without changing the hitbox");
        enduranceModelScalePerHeart = d(STATS, "enduranceModelScalePerHeart", 0.25, 0, 5, "Rendered model scale added per full heart above the threshold");
        enduranceModelScaleThresholdHearts = d(STATS, "enduranceModelScaleThresholdHearts", 10.0, 0, 2048, "Maximum-health hearts at which Endurance model scaling begins");
        strengthTooltip = b(STATS, "strengthTooltip", true, "Show Strength damage breakdown in held-item tooltips");
        agilityFlyingSpeed = b(STATS, "agilityFlyingSpeed", true, "Apply Agility to player flying speed");
        agilityFlyingSpeedFormula = s(ADV, "agilityFlyingSpeedFormula", "agility * 0.075", "Variables: agility; output is a multiplier bonus above vanilla flying speed");
        agilityFlyingSpeedMinimum = d(STATS, "agilityFlyingSpeedMinimum", 0.05, 0.01, 1.0, "Minimum flying speed");
        agilityFlyingSpeedMaximum = d(STATS, "agilityFlyingSpeedMaximum", 0.25, 0.01, 1.0, "Maximum flying speed");
        charismaNeutralMobs = b(STATS, "charismaNeutralMobs", true, "Charisma can stop configured mobs targeting the player");
        charismaNeutralLevel = i(STATS, "charismaNeutralLevel", 5, 1, 100, "Minimum Charisma level for mob neutrality");
        charismaNeutralEntityList = s(STATS, "charismaNeutralEntityList", charismaNeutralEntityList, "Comma-separated entity registry IDs affected by Charisma");
        charismaVillagerDiscount = b(STATS, "charismaVillagerDiscount", true, "Charisma reduces villager trade prices");
        charismaVillagerDiscountLevel = i(STATS, "charismaVillagerDiscountLevel", 5, 1, 100, "Minimum Charisma level for villager trade discounts");
        charismaVillagerDiscountFormula = s(ADV, "charismaVillagerDiscountFormula", "min(charisma * 0.05, 0.75)", "Variables: charisma; output is a discount fraction from 0 to 0.75. Default: 5% per Charisma level, capped at 75%");
        luckArmorDurabilityProtection = b(STATS, "luckArmorDurabilityProtection", true, "Luck can prevent armor durability loss when hurt");
        luckArmorDurabilityChancePerPoint = d(STATS, "luckArmorDurabilityChancePerPoint", 0.00025, 0, 1, "Chance per Luck point to prevent armor durability loss (0.00025 = 0.025%)");
        luckArmorDurabilityChanceCap = d(STATS, "luckArmorDurabilityChanceCap", 0.05, 0, 1, "Maximum armor durability protection chance");
        luckArmorFailsafeLevel = i(STATS, "luckArmorFailsafeLevel", 5, 1, 100, "At this Luck level, a successful protection also saves armor from breaking by unequipping it on the next tick");
        perceptionArmorThresholdNegation = b(STATS, "perceptionArmorThresholdNegation", true, "Perception adds armor-threshold negation to supported NTM bullets");
        perceptionArmorThresholdNegationPerPoint = d(STATS, "perceptionArmorThresholdNegationPerPoint", 0.01, 0, 1, "Armor-threshold negation added per Perception point above threshold");
        perceptionArmorThresholdNegationLevel = i(STATS, "perceptionArmorThresholdNegationLevel", 15, 0, 100, "Perception level at which armor-threshold negation starts");
        pepperboxSixShotPerk = b(GUNS, "pepperboxSixShotPerk", true, "Intelligence 5: Pepperbox fires six rounds per cycle");
        revolverPenetrationPerk = b(GUNS, "revolverPenetrationPerk", true, "Intelligence 15: configured revolvers penetrate and deal increased headshot damage");
        revolverHeadshotMultiplier = (float)d(GUNS, "revolverHeadshotMultiplier", 3.0, 1, 20, "Headshot multiplier for the Intelligence revolver perk");
        laserPistolDelayBalance = b(GUNS, "laserPistolDelayBalance", true, "Apply the higher-perk laser pistol delay balance");
        laserPistolDelayTicks = i(GUNS, "laserPistolDelayTicks", 10, 0, 1200, "Laser pistol delay in ticks");
        fatmanHigherPerk = b(GUNS, "fatmanHigherPerk", true, "Intelligence 15: increase Fat Man magazine capacity and apply its delay");
        fatmanHigherPerkDelay = b(GUNS, "fatmanHigherPerkDelay", true, "Intelligence 15: apply the Fat Man firing delay");
        fatmanHigherPerkDelayTicks = i(GUNS, "fatmanHigherPerkDelayTicks", 80, 0, 1200, "Fat Man firing delay in ticks (80 = 4 seconds)");
        fatmanMagazineCapacity = i(GUNS, "fatmanMagazineCapacity", 3, 1, 100, "Fat Man magazine capacity with the higher perk");
        laserPewPewHigherPerk = b(GUNS, "laserPewPewHigherPerk", true, "Intelligence 15: increase the Pew-Pew laser pistol magazine capacity");
        laserPewPewMagazineCapacity = i(GUNS, "laserPewPewMagazineCapacity", 30, 1, 1000, "Pew-Pew laser pistol magazine capacity with the higher perk");
        drillHigherPerkCapacity = b(GUNS, "drillHigherPerkCapacity", true, "Intelligence 15: add fluid capacity to the drill");
        drillAdditionalCapacityMb = i(GUNS, "drillAdditionalCapacityMb", 4000, 0, 1000000, "Additional drill fluid capacity in mB");
        weaponSecondaryAmmoCycling = b(GUNS, "weaponSecondaryAmmoCycling", true, "Allow crouch + secondary to swap the configured caliber and refund loaded old-caliber ammunition");
        hereticStyleScoping = b(GUNS, "hereticStyleScoping", true, "Allow secondary-fire hold-to-scope behavior on supported lesser-perk weapons");
        minigunLaserModePerk = b(GUNS, "minigunLaserModePerk", true, "Intelligence 15: crouch + secondary swaps the minigun to the native laser-rifle energy projectile");
        minigunLaserModeDelayTicks = i(GUNS, "minigunLaserModeDelayTicks", 4, 0, 1200, "Minigun laser-mode firing delay in ticks");
        starFFiftyBmgDelayTicks = i(GUNS, "starFFiftyBmgDelayTicks", 20, 0, 1200, "Star-F .50 BMG mode firing delay in ticks (20 = 1 second)");
        uzi556DelayTicks = i(GUNS, "uzi556DelayTicks", 2, 0, 1200, "UZI 5.56 mm mode firing delay in ticks");
        g3TenGaugeDelayTicks = i(GUNS, "g3TenGaugeDelayTicks", 10, 0, 1200, "G3 10 gauge mode firing delay in ticks");
        lagSecondarySelfKill = b(GUNS, "lagSecondarySelfKill", true, "Intelligence 5: crouch + secondary with the Lag starts the delayed self-kill sequence");
        secondaryFireToggle = b(GUNS, "secondaryFireToggle", true, "Toggle supported secondary-fire modes with Secondary Fire; primary fire then fires the selected mode");
        starFCapacity = i(GUNS, "starFCapacity", 30, 1, 1000, "Star-F magazine capacity at Intelligence 15");
        doubleBarrelGrapple = b(GUNS, "doubleBarrelGrapple", true, "Intelligence 15: secondary fire grapples toward the targeted entity with the double barrel");
        doubleBarrelGrappleVisuals = b(GUNS, "doubleBarrelGrappleVisuals", true, "Render a text chain and v marker for the double-barrel grapple");
        doubleBarrelGrappleVisualTicks = i(GUNS, "doubleBarrelGrappleVisualTicks", 12, 1, 100, "Duration of the double-barrel grapple chain text, in ticks");
        doubleBarrelGrappleRange = d(GUNS, "doubleBarrelGrappleRange", 28.0, 4.0, 128.0, "Maximum entity grapple range in blocks");
        doubleBarrelGrapplePullSpeed = d(GUNS, "doubleBarrelGrapplePullSpeed", 1.35, 0.1, 5.0, "Velocity applied toward the grapple target");
        phlogistinatorSustainedFirePerk = b(GUNS, "phlogistinatorSustainedFirePerk", true, "Intelligence perk: use NTM native held-fire cadence for the Phlogistinator-style flamer");
        phlogistinatorSustainedFireLevel = i(GUNS, "phlogistinatorSustainedFireLevel", 5, 1, 100, "Minimum Intelligence required for the Phlogistinator sustained-fire perk");
        phlogistinatorFireDelayTicks = i(GUNS, "phlogistinatorFireDelayTicks", 1, 0, 1200, "Delay between Phlogistinator secondary-fire shots in ticks");
        dragonsFuryDirectHitPerk = b(GUNS, "dragonsFuryDirectHitPerk", true, "Intelligence perk: increase direct-hit damage from the Topaz flamer's native flame projectiles");
        dragonsFuryDirectHitLevel = i(GUNS, "dragonsFuryDirectHitLevel", 15, 1, 100, "Minimum Intelligence required for the Dragon's Fury direct-hit perk");
        dragonsFuryDirectHitMultiplier = d(GUNS, "dragonsFuryDirectHitMultiplier", 1.5, 1.0, 20.0, "Direct-hit damage multiplier for Dragon's Fury secondary-fire projectiles");
        dragonsFuryBurstDelayTicks = i(GUNS, "dragonsFuryBurstDelayTicks", 10, 1, 1200, "Delay between Dragon's Fury secondary-fire shots in ticks");
        dragonsFuryBurstRange = d(GUNS, "dragonsFuryBurstRange", 3.0, 0.5, 64.0, "Maximum travel distance of Dragon's Fury secondary-fire projectiles in blocks");
        phlogSecondaryRange = d(GUNS, "phlogSecondaryRange", 5.0, 0.5, 64.0, "Maximum travel distance of Phlogistinator secondary-fire projectiles in blocks");
        phlogSecondaryWidth = d(GUNS, "phlogSecondaryWidth", 2.0, 0.25, 8.0, "Width of Phlogistinator secondary-fire projectiles in blocks");
        tauSecondaryDamageBonus = b(GUNS, "tauSecondaryDamageBonus", true, "Intelligence 5: multiply damage from the Tau's charged secondary projectile");
        tauSecondaryDamageMultiplier = d(GUNS, "tauSecondaryDamageMultiplier", 6.0, 1.0, 20.0, "Tau secondary-fire damage multiplier");
        lagSecondaryBurst = b(GUNS, "lagSecondaryBurst", true, "Intelligence 5: Lag secondary fire uses a three-projectile burst");
        lagSecondaryBurstDelay = i(GUNS, "lagSecondaryBurstDelay", 16, 0, 1200, "Lag secondary burst delay in ticks");
        burstProjectileDamageFraction = d(GUNS, "burstProjectileDamageFraction", 0.3333333333333333, 0.01, 1.0, "Damage fraction per projectile for three-projectile secondary bursts");
        scopedSecondarySpread = b(GUNS, "scopedSecondarySpread", true, "Reduce spread while aiming with configured lesser-perk weapons");
        scopedSpreadMultiplier = d(GUNS, "scopedSpreadMultiplier", 0.5, 0.05, 1.0, "Spread multiplier while aiming");
        henryLincolnSecondaryBurst = b(GUNS, "henryLincolnSecondaryBurst", true, "Intelligence 15: Henry Lincoln secondary fire uses a three-shot sequential burst");
        AMATQuackosRailgun = b(GUNS, "bmgQuackosRailgun", true, "Intelligence 15: BMG tertiary fire switches to a deuterium plasma capsule caliber; its native projectile homes to and kills Quackos");
        stingerSelfDamageProtection = b(GUNS, "stingerSelfDamageProtection", true, "Intelligence 15: suppress self-inflicted damage from configured Stinger-family projectiles");
        carbineHigherPerkCapacity = b(GUNS, "carbineHigherPerkCapacity", true, "Intelligence 15: increase the carbine magazine capacity");
        carbineMagazineCapacity = i(GUNS, "carbineMagazineCapacity", 30, 1, 1000, "Carbine magazine capacity with the higher perk");
        carbineSemiAutoPerk = b(GUNS, "carbineSemiAutoPerk", true, "Intelligence 15: right-click fires one Carbine shot per press; left-click retains native fire behavior");
        carbineRecoilMultiplier = d(GUNS, "carbineRecoilMultiplier", 0.5, 0, 1, "Carbine recoil multiplier with the higher perk; 0 disables recoil and 1 keeps native recoil");
        perceptionLootDuplication = b(STATS, "perceptionLootDuplication", true, "Perception duplicates non-stackable loot from supported loot-table results");
        perceptionLootDuplicateLevel = i(STATS, "perceptionLootDuplicateLevel", 5, 1, 100, "Perception level for two total copies of non-stackable loot");
        perceptionLootTripleLevel = i(STATS, "perceptionLootTripleLevel", 10, 1, 100, "Perception level for three total copies of non-stackable loot");
        luckRareLootBonus = b(STATS, "luckRareLootBonus", true, "Luck can add one extra item to loot tables whose path matches the configured rare-table pattern");
        luckRareLootLevel = i(STATS, "luckRareLootLevel", 15, 1, 100, "Minimum Luck level for the rare-loot bonus");
        luckRareLootChancePerPoint = d(STATS, "luckRareLootChancePerPoint", 0.01, 0, 1, "Chance per Luck point to add one extra item to a matching rare loot table");
        luckRareLootChanceCap = d(STATS, "luckRareLootChanceCap", 0.25, 0, 1, "Maximum chance for the rare-loot bonus");
        luckRareLootTablePattern = s(STATS, "luckRareLootTablePattern", "rare", "Case-insensitive substring used to identify rare loot table paths");
        showPerkScreens = b(PERKS, "showPerkScreens", true, "Allow clicking a SPECIAL row to open its perk overlay");
        showPerkTierHint = b(PERKS, "showPerkTierHint", true, "Show the Lesser/Higher perk level explanation");
        systemStatusText = s(CLIENT, "systemStatusText", "SYSTEM STATUS", "Text used for the SYSTEM STATUS heading in the stats screen");
        neuralLinkText = s(CLIENT, "neuralLinkText", "LEVVIATA NEURA-LINK  //  ACTIVE", "Text used for the 'Neura-Link' line in the stats screen");
        randomFooterMessages = s(CLIENT, "randomFooterMessages", randomFooterMessages, "Random footer options separated by |. Leave empty to show no random message.");
        setSpeedCommand = b(COMMANDS, "setSpeedCommand", true, "Register /setspeed");
        setFlySpeedCommand = b(COMMANDS, "setFlySpeedCommand", true, "Register /setflyspeed and use its per-player speed override");

        // recipe switches
        String[] recipeNames = {
                "recipeStrength",
                "recipePerception",
                "recipeEndurance",
                "recipeCharisma",
                "recipeIntelligence",
                "recipeAgility",
                "recipeLuck"
        };

        String[] specialNames = {
                "Strength",
                "Perception",
                "Endurance",
                "Charisma",
                "Intelligence",
                "Agility",
                "Luck"
        };

        for (int n = 0; n < recipeNames.length; n++) {
            recipeEnabled[n] = b(RECIPES, recipeNames[n], true, "Enable " + specialNames[n] + " bobblehead recipe");
        }

        // ore dictionary names
        oreBobblehead = s(RECIPES, "oreBobblehead", "bobblehead", "Ore dictionary name");
        oreAppleLead = s(RECIPES, "oreAppleLead", "appleLead", "Ore dictionary name");
        oreAmmo = s(RECIPES, "oreAmmo", "ammo", "Ore dictionary name");
        oreLuckAccessory = s(RECIPES, "oreLuckAccessory", "luckAccessory", "Ore dictionary name");
        oreLuckyGem = s(RECIPES, "oreLuckyGem", "luckyGem", "Ore dictionary name");

        // ore dictionary entries
        oreBobbleheadEntries = s(RECIPES, "oreBobbleheadEntries", oreBobbleheadEntries, "ModID:ItemID:[:Metadata or a metadata range of :start-end], comma separated");
        oreAppleLeadEntries = s(RECIPES, "oreAppleLeadEntries", oreAppleLeadEntries, "Ore dictionary entries");
        oreAmmoEntries = s(RECIPES, "oreAmmoEntries", oreAmmoEntries, "Ore dictionary entries");
        oreLuckAccessoryEntries = s(RECIPES, "oreLuckAccessoryEntries", oreLuckAccessoryEntries, "Ore dictionary entries");
        oreLuckyGemEntries = s(RECIPES, "oreLuckyGemEntries", oreLuckyGemEntries, "Ore dictionary entries");

        // recipe ingredients
        for (int n = 0; n < recipeIngredients.length; n++) {
            recipeIngredients[n] = s(RECIPES, "recipe" + (n + 1) + "Ingredients", recipeIngredients[n], "Comma separated entries of: oreDictionary, ModID:ItemID[:Metadata], potion:type|type");
        }

        // advanced formulas
        agilityFormula = s(ADV, "agilityFormula", agilityFormula, "Variables: agility");
        enduranceFormula = s(ADV, "enduranceFormula", enduranceFormula, "Variables: endurance");
        attackDamageFormula = s(ADV, "attackDamageFormula", attackDamageFormula, "Variables: strength");
        armorFormula = s(ADV, "armorFormula", armorFormula, "Variables: charisma");
        armorToughnessFormula = s(ADV, "armorToughnessFormula", armorToughnessFormula, "Variables: charisma");
        perceptionGunDamageFormula = s(ADV, "perceptionGunDamageFormula", perceptionGunDamageFormula, "Variables: perception, originalDamage");
        bonusDelayFormula = s(ADV, "bonusDelayFormula", bonusDelayFormula, "Variables: intelligence, originalDelay; floor preserves the original integer step");
        buckshotDelayFormula = s(ADV, "buckshotDelayFormula", buckshotDelayFormula, "Variables: intelligence, originalDelay");
        gunWearFormula = s(ADV, "gunWearFormula", gunWearFormula, "Variables: intelligence, originalWear; output is reduction fraction");

        for (String category : Arrays.asList(
                STATS, BOB, GUNS, POTIONS, RECIPES, MESSAGES, CLIENT, COMMANDS, ADV, PERKS)) {
            config.setCategoryComment(category, categoryComment(category));
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static String categoryComment(String category) {
        switch (category) {
            case PERKS:
                return "Client-side perk overlay settings.";
            case ADV:
                return "Arithmetic expressions support + - * / %, parentheses, variables, min/max/abs/sqrt/floor/ceil/round.";
            case POTIONS:
                return "Potion duration values are measured in ticks.";
            case CLIENT:
                return "Client-only GUI and keybind settings.";
            case MESSAGES:
                return "Gameplay feedback messages.";
            case BOB:
                return "Bobblehead item behavior and stat progression.";
            case GUNS:
                return "Gun mechanics, perks, ammunition and durability.";
            case RECIPES:
                return "Recipe toggles and ore dictionary configuration. Reload game to set.";
            case COMMANDS:
                return "Reload game to set.";
        }
        return "LSPECIAL mod configuration.";
    }

    private static String sideComment(String category, String comment) {
        return (CLIENT.equals(category) ? "[Client-side] " : "[Server-side] ") + comment;
    }

    // boolean, integer, double, or string
    private static boolean b(String category, String key, boolean defaultValue, String comment) {
        return config.get(category, key, defaultValue, sideComment(category, comment)).getBoolean(defaultValue);
    }
    private static int i(String category, String key, int defaultValue, int min, int max, String comment) {
        return config.get(category, key, defaultValue, sideComment(category, comment), min, max).getInt(defaultValue);
    }
    private static double d(String category, String key, double defaultValue, double min, double max, String comment) {
        return config.get(category, key, defaultValue, sideComment(category, comment), min, max).getDouble(defaultValue);
    }
    private static String s(String category, String key, String defaultValue, String comment) {
        return config.get(category, key, defaultValue, sideComment(category, comment)).getString();
    }

    @SubscribeEvent
    public static void changed(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Tags.MOD_ID.equals(event.getModID()) && config != null) {
            config.save();
            load();
        }
    }

    @SideOnly(Side.CLIENT)
    public static GuiScreen gui(GuiScreen parent) {
        List<IConfigElement> elements = new ArrayList<>();

        for (String categoryName : Arrays.asList(STATS, BOB, GUNS, POTIONS, MESSAGES, RECIPES, CLIENT, COMMANDS, ADV, PERKS)) {
            ConfigCategory category = config.getCategory(categoryName);
            elements.add(new ConfigElement(category));
        }

        return new GuiConfig(parent, elements, Tags.MOD_ID, false, false, Tags.MOD_NAME + " Configuration"
        );
    }
}
