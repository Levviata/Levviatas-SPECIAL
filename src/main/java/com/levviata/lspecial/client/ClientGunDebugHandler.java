package com.levviata.lspecial.client;

import com.levviata.lspecial.LSPECIALMod;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientGunDebugHandler {
    private ItemStack lastHeld = ItemStack.EMPTY;
    private int lastIndex = -2;
    private float lastDamage = Float.NaN;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }

        ItemStack held = mc.player.getHeldItemMainhand();
        int index = ClientGunState.getCurrentGunIndex();
        float damage = index < 0 ? Float.NaN : ClientGunState.getDamage(index);

        boolean heldChanged = !ItemStack.areItemStacksEqual(held, lastHeld);
        boolean stateChanged = index != lastIndex || Float.compare(damage, lastDamage) != 0;

        if (heldChanged || stateChanged) {
            LSPECIALMod.info("GunDebug",
                    "Client tick gun state: held={}, registry={}, meta={}, stored={}, storedRegistry={}, storedIndex={}, storedDamage={}, heldEqualStored={}",
                    held.isEmpty() ? "EMPTY" : held.getDisplayName(),
                    held.isEmpty() || held.getItem().getRegistryName() == null ? "null" : held.getItem().getRegistryName(),
                    held.isEmpty() ? -1 : held.getMetadata(),
                    ClientGunState.getGun().isEmpty() ? "EMPTY" : ClientGunState.getGun().getDisplayName(),
                    ClientGunState.getGun().isEmpty() || ClientGunState.getGun().getItem().getRegistryName() == null ? "null" : ClientGunState.getGun().getItem().getRegistryName(),
                    index,
                    damage,
                    !held.isEmpty() && ClientGunState.matches(held));

            lastHeld = held.copy();
            lastIndex = index;
            lastDamage = damage;
        }
    }
}
