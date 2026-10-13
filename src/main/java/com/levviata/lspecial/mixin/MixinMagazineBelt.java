package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** preserves the Minigun's saved native laser mode by registering the laser beam config on its belt magazine. */
@Mixin(value = MagazineBelt.class, remap = false)
public abstract class MixinMagazineBelt {

    @Shadow public List<BulletConfig> acceptedBullets;

    /** adds the native laser-rifle beam config before NTM resolves the stored magazine type. */
    @Inject(method = "getType(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;)Lcom/hbm/items/weapon/sedna/BulletConfig;", at = @At("HEAD"), remap = false)
    private void lspecial$registerSavedMinigunLaserMode(ItemStack stack, IInventory inventory,
            CallbackInfoReturnable<BulletConfig> callback) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null || acceptedBullets == null) return;
        String itemId = stack.getItem().getRegistryName().toString();
        if (("hbm:gun_minigun".equals(itemId) || "hbm:gun_minigun_dual".equals(itemId))
                && !acceptedBullets.contains(XFactoryEnergy.energy_las)) {
            acceptedBullets.add(XFactoryEnergy.energy_las);
        }
    }
}
