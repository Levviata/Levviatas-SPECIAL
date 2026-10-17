package com.levviata.lspecial.mixin;

import com.levviata.lspecial.LSPECIALConfig;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the player a mob should retaliate against. */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseRetaliation {

    private static final String LSPECIAL_RETALIATION_TARGET = "LSPECIALRetaliationTarget";

    @Inject(method = "setRevengeTarget(Lnet/minecraft/entity/EntityLivingBase;)V", at = @At("HEAD"))
    private void lspecial$recordRetaliationTarget(EntityLivingBase target, CallbackInfo ci) {
        if (!LSPECIALConfig.charismaNeutralPersistentRetaliation
                || !LSPECIALConfig.charisma || !LSPECIALConfig.charismaNeutralMobs
                || !(this instanceof EntityLiving) || !(target instanceof EntityPlayer)) return;

        EntityLiving mob = (EntityLiving) (Object) this;
        NBTTagCompound data = mob.getEntityData();
        data.setUniqueId(LSPECIAL_RETALIATION_TARGET, target.getUniqueID());
    }
}
