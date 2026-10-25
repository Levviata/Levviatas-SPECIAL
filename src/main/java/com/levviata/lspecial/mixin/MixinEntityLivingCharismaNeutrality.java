package com.levviata.lspecial.mixin;

import com.levviata.lspecial.CharismaNeutrality;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALMod;
import com.levviata.lspecial.SPECIALScoreboard;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLiving.class)
public abstract class MixinEntityLivingCharismaNeutrality {

    @Unique
    private static int lspecial$getCharisma(EntityPlayer player) {
        return SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.CHARISMA_BOARD);
    }

    @Unique
    private static String lspecial$getStoredRetaliationTarget(EntityLiving mob) {
        NBTTagCompound data = mob.getEntityData();
        return data.hasUniqueId(CharismaNeutrality.RETALIATION_TARGET)
                ? data.getUniqueId(CharismaNeutrality.RETALIATION_TARGET).toString() : "<none>";
    }

    @Inject(method = "setAttackTarget(Lnet/minecraft/entity/EntityLivingBase;)V",
            at = @At("HEAD"), cancellable = true)
    private void lspecial$applyCharismaNeutrality(EntityLivingBase target, CallbackInfo ci) {
        if (!(target instanceof EntityPlayer player)) {
            return;
        }

        EntityLiving mob = (EntityLiving) (Object) this;
        if (mob.world.isRemote) {
            return;
        }

        ResourceLocation registryKey = EntityList.getKey(mob);
        String entityId = registryKey == null ? "<unregistered>" : registryKey.toString();
        String mode = LSPECIALConfig.charismaNeutralRetaliationMode == null
                ? "NORMAL" : LSPECIALConfig.charismaNeutralRetaliationMode.trim().toUpperCase();

        if (!LSPECIALConfig.charisma || !LSPECIALConfig.charismaNeutralMobs) {
            LSPECIALMod.info("CharismaNeutrality", "entity={} target={} result=ALLOW reason=feature_disabled charismaEnabled={} neutralityEnabled={}",
                    entityId, player.getName(), LSPECIALConfig.charisma, LSPECIALConfig.charismaNeutralMobs);
            return;
        }

        int charisma = lspecial$getCharisma(player);
        if (charisma < LSPECIALConfig.charismaNeutralLevel) {
            LSPECIALMod.info("CharismaNeutrality", "entity={} target={} result=ALLOW reason=below_threshold charisma={} required={}",
                    entityId, player.getName(), charisma, LSPECIALConfig.charismaNeutralLevel);
            return;
        }

        boolean configured = CharismaNeutrality.isConfiguredNeutralMob(mob, entityId);
        if (!configured) {
            LSPECIALMod.info("CharismaNeutrality", "entity={} target={} result=ALLOW reason=not_in_neutral_list mode={} list={}",
                    entityId, player.getName(), mode, LSPECIALConfig.charismaNeutralEntityList);
            return;
        }

        boolean retaliationAllowed = CharismaNeutrality.isRetaliatingAgainst(mob, player, mode);
        if (retaliationAllowed) {
            LSPECIALMod.info("CharismaNeutrality", "entity={} target={} result=ALLOW reason=retaliation mode={} revengeTarget={} storedTarget={}",
                    entityId, player.getName(), mode,
                    mob.getRevengeTarget() == null ? "<none>" : mob.getRevengeTarget().getName(),
                    lspecial$getStoredRetaliationTarget(mob));
            return;
        }

        LSPECIALMod.info("CharismaNeutrality", "entity={} target={} result=CANCEL reason=neutral charisma={} required={} mode={} revengeTarget={} storedTarget={}",
                entityId, player.getName(), charisma, LSPECIALConfig.charismaNeutralLevel, mode,
                mob.getRevengeTarget() == null ? "<none>" : mob.getRevengeTarget().getName(),
                lspecial$getStoredRetaliationTarget(mob));
        ci.cancel();
    }
}
