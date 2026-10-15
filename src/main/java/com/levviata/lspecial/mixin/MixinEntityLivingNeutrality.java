package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents configured hostile mobs from acquiring players as targets when that player has sufficient Charisma. */
@Mixin(value = EntityLiving.class, remap = false)
public abstract class MixinEntityLivingNeutrality {

    @Inject(method = "setAttackTarget(Lnet/minecraft/entity/EntityLivingBase;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$preventTargetingCharismaticPlayer(EntityLivingBase target, CallbackInfo ci) {
        if (!(target instanceof EntityPlayer) || !LSPECIALConfig.charisma
                || !LSPECIALConfig.charismaNeutralMobs) {
            return;
        }

        EntityLiving mob = (EntityLiving) (Object) this;
        if (!lspecial$isConfiguredNeutralMob(mob)) return;

        EntityPlayer player = (EntityPlayer) target;
        Scoreboard scoreboard = player.getWorldScoreboard();
        ScoreObjective objective = scoreboard.getObjective("Charisma");
        if (objective == null || !scoreboard.entityHasObjective(player.getName(), objective)) return;

        int charisma = scoreboard.getOrCreateScore(player.getName(), objective).getScorePoints();
        if (charisma >= Math.max(5, LSPECIALConfig.charismaNeutralLevel)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean lspecial$isConfiguredNeutralMob(EntityLivingBase entity) {
        ResourceLocation key = EntityList.getKey(entity);
        if (key == null || LSPECIALConfig.charismaNeutralEntityList == null) return false;
        String registryId = key.toString();
        for (String raw : LSPECIALConfig.charismaNeutralEntityList.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) continue;
            if (id.indexOf(':') < 0) id = "minecraft:" + id;
            if (id.equals(registryId)) return true;
        }
        return false;
    }
}
