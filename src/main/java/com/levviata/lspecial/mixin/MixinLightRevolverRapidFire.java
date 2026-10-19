package com.levviata.lspecial.mixin;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALGameplayHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// todo doesnt work at all

/**
 * Feeds the light revolver's native firing state without requiring physical primary-fire input.
 */
@Mixin(value = ItemGunBaseNT.class, remap = false)
public abstract class MixinLightRevolverRapidFire {

    /**
     * Holds native primary input for the selected light revolver while its perk is enabled.
     */
    @Inject(method = "onUpdate", at = @At("RETURN"), remap = false)
    private void lspecial$forceLightRevolverPrimary(ItemStack stack, World world, Entity entity,
                                                    int slot, boolean selected, CallbackInfo callback) {
        if (!selected || !(entity instanceof EntityPlayer) || stack == null || stack.isEmpty()
                || stack.getItem().getRegistryName() == null
                || !"hbm:gun_light_revolver".equals(stack.getItem().getRegistryName().toString())
                || !LSPECIALConfig.modifyGuns || !LSPECIALConfig.intelligence || !LSPECIALConfig.pepperboxPerk
                || !LSPECIALConfig.lightRevolverRapidFirePerk
                || !SPECIALGameplayHandler.isLightRevolverRapidFireLocked((EntityPlayer) entity, stack)) {
            return;
        }

        ItemGunBaseNT gun = (ItemGunBaseNT) (Object) this;
        for (int config = 0; config < gun.getConfigCount(); config++) {
            ItemGunBaseNT.setPrimary(stack, config, true);
        }
    }
}
