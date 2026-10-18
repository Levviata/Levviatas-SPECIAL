package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALExpression;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies Charisma discounts to a per-player copy of a villager's offers.
 */
@Mixin(EntityVillager.class)
public abstract class MixinEntityVillager {

    @Shadow
    private MerchantRecipeList buyingList;

    @Inject(method = "getRecipes(Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/village/MerchantRecipeList;",
            at = @At("RETURN"), cancellable = true)
    private void lspecial$makePlayerSpecificTrades(EntityPlayer player,
                                                   CallbackInfoReturnable<MerchantRecipeList> callback) {
        if (player == null || !LSPECIALConfig.charisma
                || !LSPECIALConfig.charismaVillagerDiscount) return;

        int charisma = SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.CHARISMA_BOARD);
        if (charisma < LSPECIALConfig.charismaVillagerDiscountLevel) return;

        MerchantRecipeList originalRecipes = callback.getReturnValue();
        if (originalRecipes == null || originalRecipes.isEmpty()) return;

        MerchantRecipeList playerRecipes = new MerchantRecipeList();
        for (MerchantRecipe original : originalRecipes) {
            // Round-trip through NBT to preserve recipe fields, uses, max uses and XP settings.
            MerchantRecipe copy = new MerchantRecipe(original.writeToTags());
            lspecial$discountCost(copy.getItemToBuy(), charisma);
            lspecial$discountCost(copy.getSecondItemToBuy(), charisma);
            playerRecipes.add(copy);
        }
        callback.setReturnValue(playerRecipes);
    }

    /**
     * Keeps the villager's persistent offer-use counters in sync with the discounted copies.
     */
    @Inject(method = "useRecipe(Lnet/minecraft/village/MerchantRecipe;)V", at = @At("HEAD"))
    private void lspecial$syncPlayerTradeUsage(MerchantRecipe usedRecipe, CallbackInfo callback) {
        if (usedRecipe == null || buyingList == null || buyingList.isEmpty()) return;

        // Vanilla calls useRecipe with the actual list entry when no per-player copy was made.
        for (MerchantRecipe original : buyingList) {
            if (original == usedRecipe) return;
        }

        for (MerchantRecipe original : buyingList) {
            if (original.getToolUses() == usedRecipe.getToolUses()
                    && lspecial$sameStackIgnoringCount(original.getItemToBuy(), usedRecipe.getItemToBuy())
                    && lspecial$sameStackIgnoringCount(original.getSecondItemToBuy(), usedRecipe.getSecondItemToBuy())
                    && lspecial$sameStackIgnoringCount(original.getItemToSell(), usedRecipe.getItemToSell())) {
                original.incrementToolUses();
                return;
            }
        }
    }

    @Unique
    private static void lspecial$discountCost(ItemStack cost, int charisma) {
        if (cost == null || cost.isEmpty() || cost.getCount() <= 1) return;

        Map<String, Double> variables = new HashMap<>();
        variables.put("charisma", (double) charisma);
        double fraction = LSPECIALExpression.eval(
                LSPECIALConfig.charismaVillagerDiscountFormula, variables, 0.0D);
        fraction = Math.max(0.0D, Math.min(0.75D, fraction));
        int reducedCount = Math.max(1, (int) Math.ceil(cost.getCount() * (1.0D - fraction)));
        cost.setCount(reducedCount);
    }

    @Unique
    private static boolean lspecial$sameStackIgnoringCount(ItemStack first, ItemStack second) {
        if (first == null || first.isEmpty()) return second == null || second.isEmpty();
        if (second == null || second.isEmpty()) return false;
        return first.getItem() == second.getItem()
                && first.getMetadata() == second.getMetadata()
                && ItemStack.areItemStackTagsEqual(first, second);
    }
}
