package com.levviata.lspecial;

import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/** appends the Minigun's capacitor-ammunition perk note to the end of its tooltip. */
public final class MinigunTooltipHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null
                || !"hbm:gun_minigun".equals(stack.getItem().getRegistryName().toString())
                && !"hbm:gun_minigun_dual".equals(stack.getItem().getRegistryName().toString())) {
            return;
        }
        List<String> tooltip = event.getToolTip();
        String anchor = "hbm:gun_minigun_dual".equals(stack.getItem().getRegistryName().toString()) ? "DEBUG" : "Standard Arsenal";
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).contains(anchor)) {
                tooltip.add(i + 1, "§eHigher Perk: Accepts Capacitor ammo");
                return;
            }
        }
        tooltip.add("§eHigher Perk: Accepts Capacitor ammo");
    }
}
