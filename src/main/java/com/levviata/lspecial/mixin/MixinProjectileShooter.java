package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityThrowableNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.levviata.lspecial.StingerDamageHandler;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** persists the identity of the entity and weapon that launched an NTM throwable. */
@Mixin(value = EntityThrowableNT.class, remap = false)
public abstract class MixinProjectileShooter {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;)V", at = @At("RETURN"), remap = false)
    private void lspecial$recordConstructorShooter(World world, EntityLivingBase shooter, CallbackInfo callback) {
        lspecial$recordShooter(shooter);
    }

    @Inject(method = "setThrower(Lnet/minecraft/entity/EntityLivingBase;)V", at = @At("RETURN"), remap = false)
    private void lspecial$recordAssignedShooter(EntityLivingBase shooter, CallbackInfo callback) {
        lspecial$recordShooter(shooter);
    }

    @Unique
    private void lspecial$recordShooter(EntityLivingBase shooter) {
        if (shooter == null) {
            return;
        }

        NBTTagCompound data = ((EntityThrowableNT) (Object) this).getEntityData();
        data.setUniqueId("LSPECIALShooterUUID", shooter.getUniqueID());
        if (shooter instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) shooter;
            ItemStack held = player.getHeldItemMainhand();
            String rocketFamilyWeapon = StingerDamageHandler.getHeldStingerFamilyWeapon(player);
            if (rocketFamilyWeapon != null) {
                ItemStack offhand = player.getHeldItemOffhand();
                if (StingerDamageHandler.isStingerFamilyItem(held)) {
                    data.setString("LSPECIALShooterWeapon", held.getItem().getRegistryName().toString());
                } else if (StingerDamageHandler.isStingerFamilyItem(offhand)) {
                    data.setString("LSPECIALShooterWeapon", offhand.getItem().getRegistryName().toString());
                }
            } else if (!held.isEmpty() && held.getItem().getRegistryName() != null) {
                data.setString("LSPECIALShooterWeapon", held.getItem().getRegistryName().toString());
            }
            if (!held.isEmpty() && held.getItem() instanceof ItemGunBaseNT) {
                ItemGunBaseNT gun = (ItemGunBaseNT) held.getItem();
                boolean secondary = false;
                for (int i = 0; i < gun.getConfigCount(); i++) {
                    secondary |= ItemGunBaseNT.getSecondary(held, i);
                }
                data.setBoolean("LSPECIALShooterSecondary", secondary);
            }
        }
    }
}