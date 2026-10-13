package com.levviata.lspecial;

import com.hbm.entity.projectile.EntityBulletBase;
import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.entity.projectile.EntityRocket;
import com.hbm.entity.projectile.EntityThrowableNT;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** cancels direct and explosion damage from supported launcher projectiles to their own player. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class StingerDamageHandler {

    private StingerDamageHandler() {
    }

    /** checks the projectile owner and currently held launcher before cancelling self-inflicted damage. */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!LSPECIALConfig.stingerSelfDamageProtection || event == null
                || !(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        Entity immediate = event.getSource().getImmediateSource();
        if (!lspecial$isSupportedProjectile(immediate) || !lspecial$isStingerFamilyHeld(player)) return;
        if (lspecial$getProjectileOwner(immediate) != player) return;
        event.setCanceled(true);
    }

    /** limits protection to NTM projectile classes that expose a shooter or thrower reference. */
    private static boolean lspecial$isSupportedProjectile(Entity projectile) {
        return projectile instanceof EntityBulletBaseNT || projectile instanceof EntityBulletBase
                || projectile instanceof EntityRocket || projectile instanceof EntityThrowableNT;
    }

    /** resolves the firing player from the supported NTM projectile ownership fields. */
    private static Entity lspecial$getProjectileOwner(Entity projectile) {
        if (projectile instanceof EntityBulletBaseNT) return ((EntityBulletBaseNT) projectile).getThrower();
        if (projectile instanceof EntityBulletBase) return ((EntityBulletBase) projectile).shooter;
        if (projectile instanceof EntityRocket) return ((EntityRocket) projectile).shootingEntity;
        if (projectile instanceof EntityThrowableNT) return ((EntityThrowableNT) projectile).getThrower();
        return null;
    }

    /** recognizes the supported launcher family in either hand using registry IDs rather than display names. */
    private static boolean lspecial$isStingerFamilyHeld(EntityPlayer player) {
        return lspecial$isStingerFamilyItem(player.getHeldItemMainhand())
                || lspecial$isStingerFamilyItem(player.getHeldItemOffhand());
    }

    /** returns whether an item is one of the configured Stinger-family launchers. */
    private static boolean lspecial$isStingerFamilyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return false;
        String itemId = stack.getItem().getRegistryName().toString();
        return "hbm:gun_stinger".equals(itemId)
                || "hbm:gun_quadro".equals(itemId)
                || "hbm:gun_panzerschreck".equals(itemId)
                || "hbm:gun_missile_launcher".equals(itemId);
    }
}
