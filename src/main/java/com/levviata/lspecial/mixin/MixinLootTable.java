package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LootPerkHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** applies Perception duplication and Luck's bonus roll to generated loot when a player is available in context. */
@Mixin(LootTable.class)
public abstract class MixinLootTable {

    /** duplicates non-stackable results at the configured Perception thresholds and rolls one rare-table bonus item. */
    @Inject(method = "generateLootForPools", at = @At("RETURN"), cancellable = true)
    private void lspecial$applyLootPerks(Random random, LootContext context,
                                         CallbackInfoReturnable<List<ItemStack>> callback) {
        if (random == null || context == null) return;
        EntityPlayer player = (EntityPlayer) context.getKillerPlayer();
        if (player == null) player = LootPerkHandler.getActiveLootingPlayer();
        if (player == null) return;

        List<ItemStack> generated = callback.getReturnValue();
        if (generated == null || generated.isEmpty()) return;
        List<ItemStack> results = new ArrayList<ItemStack>(generated);
        int perception = lspecial$getScore(player, "Perception");
        int luck = lspecial$getScore(player, "Luck");

        if (LSPECIALConfig.perception && LSPECIALConfig.perceptionLootDuplication) {
            int copies = perception >= LSPECIALConfig.perceptionLootTripleLevel ? 3
                    : perception >= LSPECIALConfig.perceptionLootDuplicateLevel ? 2 : 1;
            if (copies > 1) {
                List<ItemStack> duplicates = new ArrayList<ItemStack>();
                for (ItemStack stack : generated) {
                    if (stack != null && !stack.isEmpty() && stack.getMaxStackSize() == 1) {
                        for (int copy = 1; copy < copies; copy++) duplicates.add(stack.copy());
                    }
                }
                results.addAll(duplicates);
            }
        }

        if (LSPECIALConfig.luck && LSPECIALConfig.luckRareLootBonus && luck >= LSPECIALConfig.luckRareLootLevel && LootPerkHandler.isConfiguredRareTable((LootTable) (Object) this)) {
            double chance = Math.min(LSPECIALConfig.luckRareLootChanceCap,
                    luck * LSPECIALConfig.luckRareLootChancePerPoint);
            if (random.nextDouble() < MathHelper.clamp(chance, 0.0, 1.0)) {
                List<ItemStack> candidates = generated;
                if (!candidates.isEmpty()) {
                    ItemStack bonus = candidates.get(random.nextInt(candidates.size()));
                    if (bonus != null && !bonus.isEmpty()) results.add(bonus.copy());
                }
            }
        }

        if (results.size() != generated.size()) callback.setReturnValue(results);
    }

    /** reads a player's score without creating a missing objective or score entry. */
    @Unique
    private static int lspecial$getScore(EntityPlayer player, String objectiveName) {
        Scoreboard scoreboard = player.world.getScoreboard();
        ScoreObjective objective = scoreboard.getObjective(objectiveName);
        if (objective == null || !scoreboard.entityHasObjective(player.getName(), objective)) return 0;
        return scoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
    }
}
