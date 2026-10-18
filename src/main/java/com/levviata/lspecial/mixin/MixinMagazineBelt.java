package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

/**
 * Preserves the Minigun's selected capacitor mode by registering its supported energy configs.
 */
@Mixin(value = MagazineBelt.class, remap = false)
public abstract class MixinMagazineBelt {

    @Shadow
    public List<BulletConfig> acceptedBullets;

    /**
     * Resolves the saved capacitor mode before NTM falls back to its first accepted type.
     */
    @Inject(method = "getType(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;)Lcom/hbm/items/weapon/sedna/BulletConfig;", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$preserveSelectedCapacitor(ItemStack stack, IInventory inventory, CallbackInfoReturnable<BulletConfig> callback) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound() || stack.getItem().getRegistryName() == null || acceptedBullets == null) {
            return;
        }

        String itemId = stack.getItem().getRegistryName().toString();
        if (!"hbm:gun_minigun".equals(itemId) && !"hbm:gun_minigun_dual".equals(itemId)) {
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("LSPECIALSelectedBeltCaliber")) return;
        int selectedId = tag.getInteger("LSPECIALSelectedBeltCaliber");
        for (BulletConfig config : acceptedBullets) {
            if (config != null && config.id == selectedId) {
                callback.setReturnValue(config);
                return;
            }
        }
    }

    /**
     * Registers supported capacitor configurations before NTM resolves the stored magazine type.
     */
    @Inject(method = "getType(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;)Lcom/hbm/items/weapon/sedna/BulletConfig;", at = @At("HEAD"), remap = false)
    private void lspecial$registerMinigunCapacitors(ItemStack stack, IInventory inventory, CallbackInfoReturnable<BulletConfig> callback) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null || acceptedBullets == null) {
            return;
        }
        String itemId = stack.getItem().getRegistryName().toString();
        if (!"hbm:gun_minigun".equals(itemId) && !"hbm:gun_minigun_dual".equals(itemId)) {
            return;
        }
        for (BulletConfig config : Arrays.asList(XFactoryEnergy.energy_las,
                XFactoryEnergy.energy_las_overcharge, XFactoryEnergy.energy_las_ir,
                XFactoryEnergy.energy_tesla, XFactoryEnergy.energy_tesla_overcharge,
                XFactoryEnergy.energy_tesla_ir, XFactoryEnergy.energy_emerald, XFactoryEnergy.energy_emerald_overcharge,
                XFactoryEnergy.energy_emerald_ir)) {
            if (config != null && !acceptedBullets.contains(config)) {
                acceptedBullets.add(config);
            }
        }
    }
}
