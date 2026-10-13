package com.levviata.lspecial;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.UUID;

import static com.levviata.lspecial.SPECIALScoreboard.inst;

@SideOnly(Side.CLIENT)
public final class StrengthTooltipHandler {
    private static final UUID STRENGTH_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!LSPECIALConfig.strength || !LSPECIALConfig.strengthTooltip) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !ItemStack.areItemsEqual(event.getItemStack(), mc.player.getHeldItemMainhand())) return;

        Collection<AttributeModifier> modifiers = event.getItemStack()
                .getAttributeModifiers(EntityEquipmentSlot.MAINHAND)
                .get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
        if (modifiers == null || modifiers.isEmpty()) return;

        double original = 1.0;
        for (AttributeModifier modifier : modifiers) {
            if (!STRENGTH_MODIFIER.equals(modifier.getID())) original += modifier.getAmount();
        }
        double bonus = LSPECIALExpression.eval(LSPECIALConfig.attackDamageFormula,
                java.util.Collections.singletonMap("strength", (double) inst.getClientStrength()), 0.0);
        String line = LSPECIALConfig.strengthTooltipFormat
                .replace("{original}", FORMAT.format(original))
                .replace("{bonus}", FORMAT.format(bonus))
                .replace("{total}", FORMAT.format(original + bonus));
        event.getToolTip().add("\u00a7e" + line);
    }
}
