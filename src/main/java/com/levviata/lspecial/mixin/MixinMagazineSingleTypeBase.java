package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** adds the configured magazine capacities only while the Intelligence higher perk is active. */
@Mixin(value = MagazineSingleTypeBase.class, remap = false)
public abstract class MixinMagazineSingleTypeBase {

    @Shadow public List<BulletConfig> acceptedBullets;

    /** keeps saved alternate-caliber NBT valid after reload by restoring the requested native config to its magazine. */
    @Inject(method = "getType(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;)Lcom/hbm/items/weapon/sedna/BulletConfig;", at = @At("HEAD"), remap = false)
    private void lspecial$registerSavedAlternateCaliber(ItemStack stack, net.minecraft.inventory.IInventory inventory,
            CallbackInfoReturnable<BulletConfig> callback) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null || acceptedBullets == null) return;
        String itemId = stack.getItem().getRegistryName().toString();
        BulletConfig requested = null;
        if ("hbm:gun_pepperbox".equals(itemId)) requested = XFactory44.m44_fmj;
        else if ("hbm:gun_star_f".equals(itemId) || "hbm:gun_star_f_akimbo".equals(itemId)) requested = XFactory50.bmg50_fmj;
        else if ("hbm:gun_uzi".equals(itemId) || "hbm:gun_uzi_akimbo".equals(itemId)) requested = XFactory556mm.r556_fmj;
        else if ("hbm:gun_g3".equals(itemId) || "hbm:gun_g3_zebra".equals(itemId)) requested = XFactory10ga.g10;
        else if ("hbm:gun_minigun".equals(itemId) || "hbm:gun_minigun_dual".equals(itemId)) requested = XFactoryEnergy.energy_las;
        if (requested != null && !acceptedBullets.contains(requested)) acceptedBullets.add(requested);
    }

    @Inject(method = "getCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$applyHigherPerkCapacity(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (stack == null || stack.isEmpty() || !LSPECIALConfig.intelligence || !LSPECIALConfig.revolverPerk) {
            return;
        }

        if (SPECIALScoreboard.inst == null
                || SPECIALScoreboard.inst.getIntelligenceScore() == null
                || SPECIALScoreboard.inst.getIntelligenceScore().getScorePoints() < 15
                || stack.getItem().getRegistryName() == null) {
            return;
        }

        String registryName = stack.getItem().getRegistryName().toString();

        if (LSPECIALConfig.fatmanHigherPerk && registryName.equals("hbm:gun_fatman")) {
            callback.setReturnValue(LSPECIALConfig.fatmanMagazineCapacity);
        } else if (LSPECIALConfig.laserPewPewHigherPerk && registryName.equals("hbm:gun_laser_pistol_pew_pew")) {
            callback.setReturnValue(LSPECIALConfig.laserPewPewMagazineCapacity);
        } else if (LSPECIALConfig.carbineHigherPerkCapacity && registryName.equals("hbm:gun_carbine")) {
            callback.setReturnValue(LSPECIALConfig.carbineMagazineCapacity);
        }
    }
}
