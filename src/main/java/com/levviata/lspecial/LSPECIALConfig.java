package com.levviata.lspecial;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
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
    //public static int bobbleheadUsesPerConsumption = 1;

    public static boolean infiniteItemDurability = true;
    public static boolean charismaBonus = true;
    public static boolean enduranceBonus = true;
    public static boolean amplifiedRegeneration = true;
    public static boolean respectHigherRegenAmplifier = false;
    public static boolean modifyGuns = true;
    public static boolean buckshotDelayNerf = true;
    public static boolean revolverPerk = true;
    public static boolean pepperboxPerk = true;
    public static boolean refundMagazines = true;
    public static boolean bobbleheadRename = true;
    public static boolean hotbarMessages = true;
    public static boolean variedAugLimitMessages = true;
    public static boolean toggleSpecial = true;
    public static boolean showSystemStatusInfo = true;
    public static boolean showRandomFooter = true;

    public static String systemStatusText = "SYSTEM STATUS";
    public static String neuralLinkText = "LEVVIATA NEURA-LINK  //  ACTIVE";
    public static String randomFooterMessages = "YOU ARE LIKELY NOT SPECIAL|JET FUEL CAN MELT STEEL BEAMS|VAMPIRES ARE NICE|I KNOW WHERE YOU LIVE|LEVVIATAINC|WHOS TIAMAT|SATAN IS A NICE GUY|WHOS LEVIATHAN|WHEN YOU SEE IT|ITS ALL A DREAM AND YOU ARE BLIND|BLESSED ARE THE STRONG|CURSED ARE THE WEAK|BE OBEDIENT IF YOU ARE WEAK|CONTROL IF YOU ARE STRONG|NOT EVERYONE IS EQUAL|TAX THE CHURCHES|AI IS FREE SLAVERY";
    public static boolean setSpeedCommand = true;
    public static boolean setFlySpeedCommand = true;

    //public static String bobbleheadConsumption = "ON_USE";
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
    public static String revolverList = "hbm:gun_flaregun,hbm:gun_heavy_revolver,hbm:gun_light_revolver_atlas,hbm:gun_light_revolver";

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

       // Bobblehead consumption
        statsPerBobblehead = i(BOB, "statsPerBobblehead", 1, 1, Integer.MAX_VALUE, "Points per bobblehead");
        bobbleheadRename = b(BOB, "rename", true, "Give bobbleheads their stats in their display name");

        // Guns and ammunition
        refundAmmoFormula = s(GUNS, "refundAmmoFormula", refundAmmoFormula, "Validated expression; variable: luck");
        modifyGuns = b(GUNS, "modifyGuns", true, "Master switch for gun modifications. Whether to modify Nuclear Tech guns at all.");
        buckshotDelayNerf = b(GUNS, "buckshotDelayNerf", true, "Use the nerfed delay formula for guns that use buckshot (shotguns)");
        revolverPerk = b(GUNS, "revolverHigherPerk", true, "Enable Intelligence revolver perk");
        pepperboxPerk = b(GUNS, "pepperboxLesserPerk", true, "Enable Pepperbox lesser perk");
        refundMagazines = b(GUNS, "refundMagazines", true, "Enable magazine refunds");
        gunWearMode = s(GUNS, "gunWearMode", "INFINITE_AT_INTELLIGENCE_5", "INFINITE_AT_INTELLIGENCE_5, PERCENT_REDUCTION, FORMULA");
        if (!gunWearMode.equals("INFINITE_AT_INTELLIGENCE_5") && !gunWearMode.equals("PERCENT_REDUCTION") && !gunWearMode.equals("FORMULA")) {
            gunWearMode = "INFINITE_AT_INTELLIGENCE_5";
        }
        gunWearReductionPerIntelligence = d(GUNS, "gunWearReductionPerIntelligence", 0.05, 0, 1, "Wear reduction per Intelligence point");
        buckshotGaugeList = s(GUNS, "buckshotGaugeList", buckshotGaugeList, "Comma separated list of hbm:ammo_standard but ONLY with the Metadata. Also supports hbm:ammo_secret:Metadata");
        revolverList = s(GUNS, "revolverList", revolverList, "Comma separated list of ModID:ItemID");

        // Potions and effects
        amplifiedRegeneration = b(POTIONS, "amplifiedRegeneration", true, "Endurance level 5 perk. Enable amplified regeneration.");
        respectHigherRegenAmplifier = b(POTIONS, "respectHigherRegenAmplifier", false, "Whether to amplify existing regeneration or not. Might fix issues with other mods");
        regenAmplifierBonus = d(POTIONS, "regenAmplifierBonus", 1.0, 0, 255, "Amplifier bonus");
        potionTimeTicks = i(POTIONS, "potionTimeTicks", 115, 0, 720000, "Potion duration in ticks. Default value is 5~ seconds");
        charismaPotionAmplifier = i(POTIONS, "charismaPotionAmplifier", 0, 0, 255, "Resistance amplifier");
        endurancePotionAmplifier = i(POTIONS, "endurancePotionAmplifier", 1, 0, 255, "Regeneration amplifier bonus");

        // Gameplay messages
        hotbarMessages = b(MESSAGES, "hotbarMessages", true, "Show hotbar messages");
        limitMessage = s(MESSAGES, "limitMessage", limitMessage, "Stat limit reached message");
        variedAugLimitMessages = b(MESSAGES, "variedAugLimitMessages", true, "Use varied messages when increment stat limit");
        augLimitMessage = s(MESSAGES, "augLimitMessage", "Augmented SPECIAL limit by one", "Single message with no variety");
        augUnder5 = s(MESSAGES, "augUnder5", augUnder5, "Triggers under 5 stat limit increases");
        aug5 = s(MESSAGES, "aug5", aug5, "Triggers above 5 stat limit increases");
        aug10 = s(MESSAGES, "aug10", aug10, "Triggers above 10 limit increases");
        aug20 = s(MESSAGES, "aug20", aug20, "Triggers above 20 increases");

        // Client interface and commands
        toggleSpecial = b(CLIENT, "toggleSpecial", true, "Enable toggleSpecial keybind");
        showSystemStatusInfo = b(CLIENT, "showSystemStatusInfo", true, "Show the SYSTEM STATUS and 'Neura-Link' information in the stats screen");
        showRandomFooter = b(CLIENT, "showRandomFooter", true, "Show the randomized footer in the stats screen");
        systemStatusText = s(CLIENT, "systemStatusText", "SYSTEM STATUS", "Text used for the SYSTEM STATUS heading in the stats screen");
        neuralLinkText = s(CLIENT, "neuralLinkText", "LEVVIATA NEURA-LINK  //  ACTIVE", "Text used for the 'Neura-Link' status line in the stats screen");
        randomFooterMessages = s(CLIENT, "randomFooterMessages", randomFooterMessages, "Random footer options separated by |. Leave empty to show no random message.");
        setSpeedCommand = b(COMMANDS, "setSpeedCommand", true, "Register /setspeed");
        setFlySpeedCommand = b(COMMANDS, "setFlySpeedCommand", true, "Register /setflyspeed. Currently doesn't work");

        // Recipe switches
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

        // Ore dictionary names
        oreBobblehead = s(RECIPES, "oreBobblehead", "bobblehead", "Ore dictionary name");
        oreAppleLead = s(RECIPES, "oreAppleLead", "appleLead", "Ore dictionary name");
        oreAmmo = s(RECIPES, "oreAmmo", "ammo", "Ore dictionary name");
        oreLuckAccessory = s(RECIPES, "oreLuckAccessory", "luckAccessory", "Ore dictionary name");
        oreLuckyGem = s(RECIPES, "oreLuckyGem", "luckyGem", "Ore dictionary name");

        // Ore dictionary entries
        oreBobbleheadEntries = s(RECIPES, "oreBobbleheadEntries", oreBobbleheadEntries, "ModID:ItemID:[:Metadata or a metadata range of :start-end], comma separated");
        oreAppleLeadEntries = s(RECIPES, "oreAppleLeadEntries", oreAppleLeadEntries, "Ore dictionary entries");
        oreAmmoEntries = s(RECIPES, "oreAmmoEntries", oreAmmoEntries, "Ore dictionary entries");
        oreLuckAccessoryEntries = s(RECIPES, "oreLuckAccessoryEntries", oreLuckAccessoryEntries, "Ore dictionary entries");
        oreLuckyGemEntries = s(RECIPES, "oreLuckyGemEntries", oreLuckyGemEntries, "Ore dictionary entries");

        // Recipe ingredients
        for (int n = 0; n < recipeIngredients.length; n++) {
            recipeIngredients[n] = s(RECIPES, "recipe" + (n + 1) + "Ingredients", recipeIngredients[n], "Comma-separated tokens: oreName, ModID:ItemID[:Metadata], potion:type|type");
        }

        // Advanced formulas
        agilityFormula = s(ADV, "agilityFormula", agilityFormula, "Expression variable: agility");
        enduranceFormula = s(ADV, "enduranceFormula", enduranceFormula, "Expression variable: endurance");
        attackDamageFormula = s(ADV, "attackDamageFormula", attackDamageFormula, "Expression variable: strength");
        armorFormula = s(ADV, "armorFormula", armorFormula, "Expression variable: charisma");
        armorToughnessFormula = s(ADV, "armorToughnessFormula", armorToughnessFormula, "Expression variable: charisma");
        perceptionGunDamageFormula = s(ADV, "perceptionGunDamageFormula", perceptionGunDamageFormula, "Variables: perception, originalDamage");
        bonusDelayFormula = s(ADV, "bonusDelayFormula", bonusDelayFormula, "Variables: intelligence, originalDelay; floor preserves the original integer step");
        buckshotDelayFormula = s(ADV, "buckshotDelayFormula", buckshotDelayFormula, "Variables: intelligence, originalDelay");
        gunWearFormula = s(ADV, "gunWearFormula", gunWearFormula, "Variables: intelligence, originalWear; output is reduction fraction");

        for (String category : Arrays.asList(
                STATS, BOB, GUNS, POTIONS, RECIPES, MESSAGES, CLIENT, COMMANDS, ADV)) {
            config.setCategoryComment(category, categoryComment(category));
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static String categoryComment(String category) {
        switch (category) {
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
                return "Recipe toggles and ore dictionary configuration. Needs reload to set.";
            case COMMANDS:
                return "Needs reload to set.";
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

        for (String categoryName : Arrays.asList(STATS, BOB, GUNS, POTIONS, MESSAGES, RECIPES, CLIENT, COMMANDS, ADV)) {
            ConfigCategory category = config.getCategory(categoryName);
            elements.add(new ConfigElement(category));
        }

        return new GuiConfig(parent, elements, Tags.MOD_ID, false, false, Tags.MOD_NAME + " Configuration"
        );
    }
}
