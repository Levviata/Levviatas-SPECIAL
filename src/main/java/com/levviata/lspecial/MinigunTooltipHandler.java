package com.levviata.lspecial;

import net.minecraft.item.ItemStack;

import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
        event.getToolTip().add("§eHigher Perk: Accepts Capacitor ammo");
    }
}
