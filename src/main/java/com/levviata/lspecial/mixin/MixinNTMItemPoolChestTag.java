package com.levviata.lspecial.mixin;

import com.hbm.handler.WeightedRandomChestContentFrom1710;
import com.hbm.itempool.ItemPool;
import com.levviata.lspecial.LSPECIALNTMLootHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Random;

/**
 * Tags containers populated by NTM item pools, including structure-generated chests.
 */
@Mixin(value = WeightedRandomChestContentFrom1710.class, remap = false)
public abstract class MixinNTMItemPoolChestTag {
    @Inject(
            method = "generateChestContents(Ljava/util/Random;[Lcom/hbm/handler/WeightedRandomChestContentFrom1710;Lnet/minecraftforge/common/capabilities/ICapabilityProvider;I)V",
            at = @At("RETURN"),
            remap = false
    )
    private static void lspecial$tagPoolContainer(Random random, WeightedRandomChestContentFrom1710[] contents, ICapabilityProvider provider, int rolls, CallbackInfo callback) {
        if (!(provider instanceof TileEntity) || contents == null || ItemPool.pools == null) {
            return;
        }

        String poolName = null;
        for (Map.Entry<String, ItemPool> entry : ItemPool.pools.entrySet()) {
            ItemPool pool = entry.getValue();
            if (pool != null && pool.pool == contents) {
                poolName = entry.getKey();
                break;
            }
        }

        if (poolName != null) {
            LSPECIALNTMLootHandler.tagContainer((TileEntity) provider, poolName, rolls, rolls);
        }
    }
}
