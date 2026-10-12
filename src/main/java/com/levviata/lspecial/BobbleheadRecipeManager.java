package com.levviata.lspecial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.PotionType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IngredientNBT;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class BobbleheadRecipeManager {

    private static final String HBM = "hbm";

    private static boolean oreDictRegistered = false;

    public static void registerOreDict() {
        registerConfiguredEntries(
                LSPECIALConfig.oreBobblehead,
                LSPECIALConfig.oreBobbleheadEntries
        );
        registerConfiguredEntries(
                LSPECIALConfig.oreAppleLead,
                LSPECIALConfig.oreAppleLeadEntries
        );
        registerConfiguredEntries(
                LSPECIALConfig.oreAmmo,
                LSPECIALConfig.oreAmmoEntries
        );
        registerConfiguredEntries(
                LSPECIALConfig.oreLuckAccessory,
                LSPECIALConfig.oreLuckAccessoryEntries
        );
        registerConfiguredEntries(
                LSPECIALConfig.oreLuckyGem,
                LSPECIALConfig.oreLuckyGemEntries
        );
    }

    private static void registerConfiguredEntries(String oreName, String entries) {
        for (String rawEntry : entries.split(",")) {
            String token = rawEntry.trim();

            if (token.isEmpty()) {
                continue;
            }

            String[] parts = token.split(":");
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalArgumentException(
                        "Ore entry must be ModID:ItemID[:Metadata or a metadata range of :start-end]: " + token
                );
            }

            Item item = getItem(parts[0], parts[1]);

            // if metadata is omitted, default to 0.
            String metadata = parts.length == 3 ? parts[2] : "0";

            if (metadata.contains("-")) {
                String[] range = metadata.split("-");
                int min = Integer.parseInt(range[0]);
                int max = Integer.parseInt(range[1]);

                if (max < min || max - min > 4096) {
                    throw new IllegalArgumentException("Invalid metadata range: " + token);
                }

                for (int meta = min; meta <= max; meta++) {
                    OreDictionary.registerOre(oreName, new ItemStack(item, 1, meta));
                }
            } else {
                OreDictionary.registerOre(oreName, new ItemStack(item, 1, Integer.parseInt(metadata)));
            }
        }
    }

    private static Item getItem(String namespace, String name) {
        ResourceLocation id = new ResourceLocation(namespace, name);
        Item item = ForgeRegistries.ITEMS.getValue(id);

        if (item == null || item == Items.AIR) {
            throw new IllegalStateException("Missing item: " + id);
        }

        return item;
    }

    private Ingredient item(String namespace, String name, int metadata) {
        ItemStack stack = new ItemStack(getItem(namespace, name), 1, metadata);
        return Ingredient.fromStacks(stack);
    }

    private Ingredient ore(String name) {
        List<ItemStack> stacks = OreDictionary.getOres(name);

        if (stacks.isEmpty()) {
            throw new IllegalStateException("Ore Dictionary entry is empty: " + name);
        }

        return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
    }

    private Ingredient potion(PotionType... types) {
        ItemStack[] stacks = new ItemStack[types.length];

        for (int i = 0; i < types.length; i++) {
            ItemStack potionStack = new ItemStack(Items.POTIONITEM);
            stacks[i] = net.minecraft.potion.PotionUtils.addPotionToItemStack(potionStack, types[i]);
        }

        return IngredientNBT.fromStacks(stacks);
    }

    private void addRecipe(RegistryEvent.Register<IRecipe> event, String name, int outputMetadata, Ingredient... ingredients) {
        NonNullList<Ingredient> input = NonNullList.create();
        input.addAll(Arrays.asList(ingredients));

        ItemStack output = new ItemStack(getItem(HBM, "bobblehead"), 1, outputMetadata);

        ResourceLocation recipeId = new ResourceLocation(Tags.MOD_ID, name);
        ShapelessOreRecipe recipe = new ShapelessOreRecipe(recipeId, input, output);
        recipe.setRegistryName(recipeId);

        event.getRegistry().register(recipe);
    }

    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        // Item registration has progressed beyond preInit by this point.
        if (!oreDictRegistered) {
            registerOreDict();
            oreDictRegistered = true;
        }

        for (int i = 0; i < 7; i++) {
            if (!LSPECIALConfig.recipeEnabled[i]) {
                continue;
            }

            try {
                addConfiguredRecipe(event, i + 1, LSPECIALConfig.recipeIngredients[i]);
            } catch (RuntimeException exception) {
                LSPECIALMod.LOGGER.error("Could not register configurable bobblehead recipe {}. " + "Keeping it disabled for this launch: {}", i + 1, exception.getMessage());
            }
        }
    }

    private void addConfiguredRecipe(RegistryEvent.Register<IRecipe> event, int index, String specification) {
        List<Ingredient> ingredients = new ArrayList<>();

        for (String rawEntry : specification.split(",")) {
            String token = rawEntry.trim();

            if (token.isEmpty()) {
                continue;
            }

            if (token.startsWith("potion:")) {
                String[] names = token.substring("potion:".length()).split("\\|");
                PotionType[] types = new PotionType[names.length];

                for (int i = 0; i < names.length; i++) {
                    String name = names[i].trim();
                    ResourceLocation id = name.contains(":") ? new ResourceLocation(name) : new ResourceLocation("minecraft", name);

                    types[i] = ForgeRegistries.POTION_TYPES.getValue(id);

                    if (types[i] == null) {
                        throw new IllegalArgumentException("Unknown potion type " + id);
                    }
                }

                ingredients.add(potion(types));
            } else if (!token.contains(":")) {
                ingredients.add(ore(getOreDictionaryName(token)));
            } else {
                String[] parts = token.split(":");

                if (parts.length < 2 || parts.length > 3) {
                    throw new IllegalArgumentException("Expected namespace:item[:metadata], got " + token);
                }

                int metadata = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;

                ingredients.add(item(parts[0], parts[1], metadata));
            }
        }

        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("No ingredients specified");
        }

        addRecipe(event, "bobblehead_" + index, index, ingredients.toArray(new Ingredient[0]));
    }

    private String getOreDictionaryName(String token) {
        switch (token) {
            case "bobblehead":
                return LSPECIALConfig.oreBobblehead;
            case "appleLead":
                return LSPECIALConfig.oreAppleLead;
            case "ammo":
                return LSPECIALConfig.oreAmmo;
            case "luckAccessory":
                return LSPECIALConfig.oreLuckAccessory;
            case "luckyGem":
                return LSPECIALConfig.oreLuckyGem;
            default:
                return token;
        }
    }
}