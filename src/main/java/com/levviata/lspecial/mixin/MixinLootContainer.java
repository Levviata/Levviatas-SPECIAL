package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LootPerkHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityLockableLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** makes the player opening a vanilla loot container available to the nested loot-table generation hook. */
@Mixin(TileEntityLockableLoot.class)
public abstract class MixinLootContainer {

    /** records the looter before the container asks its loot table to generate item stacks. */
    @Inject(method = "fillWithLoot(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("HEAD"))
    private void lspecial$beginLootGeneration(EntityPlayer player, CallbackInfo callback) {
        LootPerkHandler.setActiveLootingPlayer(player);
    }

    /** clears the temporary looter reference after generation to prevent affecting unrelated loot rolls. */
    @Inject(method = "fillWithLoot(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("RETURN"))
    private void lspecial$endLootGeneration(EntityPlayer player, CallbackInfo callback) {
        LootPerkHandler.setActiveLootingPlayer(null);
    }
}
