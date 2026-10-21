package com.levviata.lspecial;

import com.levviata.lspecial.client.ClientGunState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.levviata.lspecial.SPECIALScoreboard.inst;
import static com.levviata.lspecial.StatsLogic.ATTACK_DAMAGE_UUID;

@SideOnly(Side.CLIENT)
public final class TooltipAdder {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
    private static final Pattern DAMAGE_VALUE = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?");

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack tooltipStack = event.getItemStack();

        minigunTooltips(tooltipStack, event);

        strengthTooltip(tooltipStack, event);

        perceptionTooltip(tooltipStack, event);

        armorFailsafeTooltip(tooltipStack, event);
    }

    private void armorFailsafeTooltip(ItemStack stack, ItemTooltipEvent event) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();

        if (tag.getBoolean("LSPECIALArmorFailsafe")) {
            int savedDamage = tag.getInteger("LSPECIALArmorFailsafeDamage");

            if (stack.getItemDamage() != savedDamage) {
                tag.removeTag("LSPECIALArmorFailsafe");
                tag.removeTag("LSPECIALArmorFailsafeDamage");
            } else {
                event.getToolTip().add("§7(Luck Lesser perk)");
                event.getToolTip().add("§aSaved!");
            }
        }

        if (tag.getBoolean("LSPECIALArmorRecharged")) {
            long savedCharge = tag.getLong("LSPECIALArmorRechargedCharge");
            long maxCharge = tag.getLong("LSPECIALArmorRechargedMaxCharge");

            if (maxCharge > 0L && Math.abs(getPoweredArmorCharge(stack) - savedCharge) >= maxCharge * 0.20D) {
                tag.removeTag("LSPECIALArmorRecharged");
                tag.removeTag("LSPECIALArmorRechargedCharge");
                tag.removeTag("LSPECIALArmorRechargedMaxCharge");
            } else {
                event.getToolTip().add("§7(Luck Lesser perk)");
                event.getToolTip().add("§aRecharged!");
            }
        }

        if (stack.getItem() instanceof com.hbm.items.armor.ArmorFSBPowered) {
            int cooldown = tag.getInteger("LSPECIALArmorRechargeCooldown");
            if (cooldown > 0) {
                event.getToolTip().add("§cRecharge Cooldown: " + ((cooldown + 19) / 20));
            }
        }
    }

    private long getPoweredArmorCharge(ItemStack stack) {
        if (!(stack.getItem() instanceof com.hbm.items.armor.ArmorFSBPowered poweredArmor)) {
            return Long.MIN_VALUE;
        }
        return poweredArmor.getCharge(stack);
    }

    private void strengthTooltip(ItemStack stack, ItemTooltipEvent event) {
        if (!LSPECIALConfig.strength || !LSPECIALConfig.strengthTooltip || inst == null) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !ItemStack.areItemsEqual(stack, mc.player.getHeldItemMainhand())) {
            return;
        }

        Collection<AttributeModifier> modifiers = stack
                .getAttributeModifiers(EntityEquipmentSlot.MAINHAND)
                .get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());

        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }

        double original = 0.0;
        for (AttributeModifier modifier : modifiers) {
            if (ATTACK_DAMAGE_UUID.equals(modifier.getID())) {
                original += modifier.getAmount();
            }
        }

        double bonus = LSPECIALExpression.eval(
                LSPECIALConfig.attackDamageFormula,
                java.util.Collections.singletonMap(
                        "strength", (double) inst.getClientStrength()),
                0.0);

        String attackDamageName = I18n.format("attribute.name.generic.attackDamage");
        List<String> tooltip = event.getToolTip();

        for (int i = 0; i < tooltip.size(); i++) {
            String line = tooltip.get(i);

            if (!line.contains(attackDamageName)) {
                continue;
            }

            Matcher matcher = DAMAGE_VALUE.matcher(line);
            if (matcher.find()) {
                tooltip.set(i, matcher.replaceFirst(
                        Matcher.quoteReplacement(FORMAT.format(original + bonus))));
            }

            return;
        }
    }

    private void minigunTooltips(ItemStack stack, ItemTooltipEvent event) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return;
        }

        String registryName = stack.getItem().getRegistryName().toString();

        boolean dual = "hbm:gun_minigun_dual".equals(registryName);

        if (!dual && !"hbm:gun_minigun".equals(registryName)) {
            return;
        }

        List<String> tooltip = event.getToolTip();
        String anchor = dual ? "DEBUG" : "Standard Arsenal";

        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).contains(anchor)) {
                tooltip.add(i + 1, "§eHigher Perk: Accepts Capacitor ammo");
                return;
            }
        }

        tooltip.add("§eHigher Perk: Accepts Capacitor ammo");
    }

    private void perceptionTooltip(ItemStack stack, ItemTooltipEvent event) {
        if (!LSPECIALConfig.perception) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null
                || !ClientGunState.matches(stack)
                || !ItemStack.areItemsEqual(stack, mc.player.getHeldItemMainhand())) {
            return;
        }

        float damage = ClientGunState.getDamage(0);

        if (Float.isNaN(damage)) {
            return;
        }

        String attackDamageName = I18n.format("attribute.name.generic.attackDamage");
        List<String> tooltip = event.getToolTip();

        for (int i = 0; i < tooltip.size(); i++) {
            String line = tooltip.get(i);

            if (!line.contains(attackDamageName)) {
                continue;
            }

            Matcher matcher = DAMAGE_VALUE.matcher(line);

            if (matcher.find()) {
                tooltip.set(i, matcher.replaceFirst(
                        Matcher.quoteReplacement(FORMAT.format(damage))
                ));
            }

            return;
        }
    }
}
