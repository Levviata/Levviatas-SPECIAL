package com.levviata.lspecial;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

public class BobbleheadUse {

    // the limit starts at 10 and every augment adds 1 (see SPECIALScoreboard), so the player's own score says how many they used
    private static final int DEFAULT_LIMIT = 10;

    @SubscribeEvent
    public void use(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();

        if (player.world.isRemote) {
            return;
        }

        Item bobblehead = Item.getByNameOrId("hbm:bobblehead");
        if (bobblehead == null) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getItem() != bobblehead || !player.isSneaking()) {
            return;
        }

        int metadata = stack.getMetadata();
        boolean granted = false;

        switch (metadata) {
            case 1:
                granted = grant(inst.getPlayerScore(player, inst.STRENGTH_BOARD), LSPECIALConfig.strength, stack, player);
                break;
            case 2:
                granted = grant(inst.getPlayerScore(player, inst.PERCEPTION_BOARD), LSPECIALConfig.perception, stack, player);
                break;
            case 3:
                granted = grantWithPerk(
                        inst.getPlayerScore(player, inst.ENDURANCE_BOARD),
                        inst.getPlayerScore(player, inst.ENDURANCE_BONUS_BOARD),
                        LSPECIALConfig.endurance,
                        LSPECIALConfig.enduranceBonus,
                        stack,
                        player
                );
                break;
            case 4:
                granted = grantWithPerk(
                        inst.getPlayerScore(player, inst.CHARISMA_BOARD),
                        inst.getPlayerScore(player, inst.CHARISMA_BONUS_BOARD),
                        LSPECIALConfig.charisma,
                        LSPECIALConfig.charismaBonus,
                        stack,
                        player
                );
                break;
            case 5:
                granted = grant(inst.getPlayerScore(player, inst.INTELLIGENCE_BOARD), LSPECIALConfig.intelligence, stack, player);
                break;
            case 6:
                granted = grant(inst.getPlayerScore(player, inst.AGILITY_BOARD), LSPECIALConfig.agility, stack, player);
                break;
            case 7:
                granted = grant(inst.getPlayerScore(player, inst.LUCK_BOARD), LSPECIALConfig.luck, stack, player);
                break;
            default:
                if (metadata > 7 && LSPECIALConfig.statLimit && inst.getPlayerScore(player, inst.LIMIT_BOARD).getScorePoints() < Integer.MAX_VALUE) {

                    inst.getPlayerScore(player, inst.LIMIT_BOARD).setScorePoints(inst.getPlayerScorePoints(player, inst.LIMIT_BOARD) + 1);

                    showAugLimit(player);
                    granted = true;
                    stack.shrink(1);
                }
                break;
        }

        if (granted && player instanceof EntityPlayerMP) {
            LSPECIALMod.syncStats((EntityPlayerMP) player);
        }
    }

    private boolean grant(Score score, boolean enabled, ItemStack stack, EntityPlayer player) {
        if (!enabled) {
            return false;
        }

        if (LSPECIALConfig.statLimit
                && score.getScorePoints() >= inst.getPlayerScorePoints(player, inst.LIMIT_BOARD)) {
            message(player, LSPECIALConfig.limitMessage);
            return false;
        }

        int gain = LSPECIALConfig.statsPerBobblehead;

        if (LSPECIALConfig.statLimit) {
            int remaining = Math.max(0, inst.getPlayerScorePoints(player, inst.LIMIT_BOARD) - score.getScorePoints());
            gain = Math.min(gain, remaining);
        }

        if (gain <= 0) {
            message(player, LSPECIALConfig.limitMessage);
            return false;
        }

        score.setScorePoints(score.getScorePoints() + gain);
        stack.shrink(1);
        return true;
    }

    private boolean grantWithPerk(Score score, Score bonus, boolean enabled, boolean bonusEnabled, ItemStack stack, EntityPlayer player) {
        if (!grant(score, enabled, stack, player)) {
            return false;
        }

        if (bonusEnabled && score.getScorePoints() >= 4 && bonus.getScorePoints() != 1) {
            bonus.setScorePoints(1);
        }
        return true;
    }

    private void message(EntityPlayer player, String text) {
        if (LSPECIALConfig.hotbarMessages && text != null && !text.isEmpty()) {
            player.sendStatusMessage(new TextComponentString(text), true);
        }
    }

    private void showAugLimit(EntityPlayer player) {
        if (!LSPECIALConfig.statLimit) {
            return;
        }

        // the score was already raised for this augment, and it belongs to this player only
        int augLimit = Math.max(0, inst.getPlayerScorePoints(player, inst.LIMIT_BOARD) - DEFAULT_LIMIT);

        if (!LSPECIALConfig.variedAugLimitMessages) {
            message(player, LSPECIALConfig.augLimitMessage);
            return;
        }

        if (augLimit >= 20) {
            message(player, LSPECIALConfig.aug20);
        } else if (augLimit >= 10) {
            message(player, LSPECIALConfig.aug10);
        } else if (augLimit >= 5) {
            message(player, LSPECIALConfig.aug5);
        } else {
            message(player, LSPECIALConfig.augUnder5);
        }
    }
}
