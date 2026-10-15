package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents configured mobs from acquiring a protected player as an AI target. */
@Mixin(value = EntityAITarget.class, remap = false)
public abstract class MixinEntityAITargetNeutrality {

    @Inject(method = "isSuitableTarget(Lnet/minecraft/entity/EntityLiving;Lnet/minecraft/entity/EntityLivingBase;ZZ)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void lspecial$blockCharismaProtectedTarget(EntityLiving owner, EntityLivingBase target,
                                                               boolean includeInvincibles, boolean checkSight,
                                                               CallbackInfoReturnable<Boolean> callback) {
        if (target instanceof EntityPlayer && lspecial$isProtectedByCharisma(owner, (EntityPlayer) target)) {
            callback.setReturnValue(false);
        }
    }

    @Unique
    private static boolean lspecial$isProtectedByCharisma(EntityLiving mob, EntityPlayer player) {
        if (!LSPECIALConfig.charisma || !LSPECIALConfig.charismaNeutralMobs || player == null) return false;
        SPECIALScoreboard stats = SPECIALScoreboard.inst;
        if (stats == null || stats.getCharismaScore() == null
                || stats.getCharismaScore().getScorePoints() < Math.max(5, LSPECIALConfig.charismaNeutralLevel)) return false;
        ResourceLocation key = EntityList.getKey(mob);
        if (key == null || LSPECIALConfig.charismaNeutralEntityList == null) return false;
        for (String raw : LSPECIALConfig.charismaNeutralEntityList.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) continue;
            if (id.indexOf(':') < 0) id = "minecraft:" + id;
            if (id.equals(key.toString())) return true;
        }
        return false;
    }
}
