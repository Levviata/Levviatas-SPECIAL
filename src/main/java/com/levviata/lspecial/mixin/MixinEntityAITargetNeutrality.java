package com.levviata.lspecial.mixin;

import com.levviata.lspecial.CharismaNeutrality;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps neutral mobs from even picking the player as a target.<br>
 * <br>
 * Why: cancelling setAttackTarget alone leaves the targeting AI running, it picks the player, gets refused and picks
 * the player again next tick, which shows as mobs that lock on, drop it and repeat. Where: the static suitability check
 * every vanilla target AI goes through, so the AI simply finds no target. Risks: mods with their own target AI that
 * skips this check still get stopped by the setAttackTarget hook. Avoidable: no, Forge has no event that makes a
 * target unsuitable (LivingSetAttackTargetEvent can't cancel).
 */
@Mixin(EntityAITarget.class)
public abstract class MixinEntityAITargetNeutrality {

    @Inject(method = "isSuitableTarget(Lnet/minecraft/entity/EntityLiving;Lnet/minecraft/entity/EntityLivingBase;ZZ)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private static void lspecial$skipNeutralPlayer(EntityLiving attacker, EntityLivingBase target, boolean includeInvincibles, boolean checkSight, CallbackInfoReturnable<Boolean> callback) {
        if (!(target instanceof EntityPlayer player) || attacker == null || attacker.world.isRemote) {
            return;
        }

        if (CharismaNeutrality.isNeutralTowards(attacker, player)) {
            callback.setReturnValue(false);
        }
    }
}
