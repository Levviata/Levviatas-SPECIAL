package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.levviata.lspecial.PlayerAcceptedBullets;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Preserves the Minigun's selected capacitor mode and keeps the capacitor configs per player.<br>
 * <br>
 * Why: the belt's acceptedBullets list is shared by every Minigun, so adding the capacitors to it unlocked them for
 * everybody. How/where: the constructor swaps the field for a PlayerAcceptedBullets view. Risks: see
 * MixinMagazineSingleTypeBase. Avoidable: not without NTM giving getType a player argument.
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
     * Wraps the shared acceptedBullets list so the capacitor modes only show up for players who may use them.
     */
    @Inject(method = "<init>()V", at = @At("RETURN"), remap = false)
    private void lspecial$installPlayerAcceptedBullets(CallbackInfo callback) {
        if (acceptedBullets != null && !(acceptedBullets instanceof PlayerAcceptedBullets)) {
            acceptedBullets = new PlayerAcceptedBullets(acceptedBullets);
        }
    }
}
