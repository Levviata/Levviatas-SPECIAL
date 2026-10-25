package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.SPECIALPlayerContext;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds fluid capacity to NTM's drill magazine without changing the magazine's shared base capacity.
 */
@Mixin(value = MagazineFluid.class, remap = false)
public abstract class MixinMagazineFluid {

    @Inject(method = "getCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$addDrillFluidCapacity(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (!LSPECIALConfig.drillHigherPerkCapacity || !LSPECIALConfig.intelligence
                || stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null
                || !"hbm:gun_drill".equals(stack.getItem().getRegistryName().toString())) {
            return;
        }

        EntityPlayer player = SPECIALPlayerContext.getPlayer();
        if (!LSPECIALMod.hasIntelligenceLevel(player, LSPECIALMod.HIGHER_PERK)) {
            return;
        }

        callback.setReturnValue(callback.getReturnValueI() + LSPECIALConfig.drillAdditionalCapacityMb);
    }
}
