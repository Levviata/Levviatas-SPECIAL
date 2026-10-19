package com.levviata.lspecial;

import com.hbm.entity.projectile.EntityBulletBase;
import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.entity.projectile.EntityRocket;
import com.hbm.entity.projectile.EntityThrowableNT;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Cancels self-damage from configured NTM rocket-family projectiles without relying on the currently held item.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class StingerDamageHandler {

    private static final ThreadLocal<EntityPlayer> ROCKET_EXPLOSION_OWNER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROCKET_EXPLOSION_WEAPON = new ThreadLocal<>();

    private StingerDamageHandler() {
    }

    /**
     * Marks the synchronous explosion call so its damage can be attributed to the rocket that caused it.
     */
    public static void beginRocketExplosion(Entity owner, String weaponId) {
        if (owner instanceof EntityPlayer && isStingerFamilyWeapon(weaponId)) {
            ROCKET_EXPLOSION_OWNER.set((EntityPlayer) owner);
            ROCKET_EXPLOSION_WEAPON.set(weaponId);
        }
    }

    /**
     * Clears the temporary explosion context after NTM has applied explosion damage and knockback.
     */
    public static void endRocketExplosion() {
        ROCKET_EXPLOSION_OWNER.remove();
        ROCKET_EXPLOSION_WEAPON.remove();
    }

    /**
     * Checks whether the current synchronous explosion belongs to this player and a configured launcher.
     */
    public static boolean isTrackedRocketExplosion(EntityPlayer player) {
        return player != null && ROCKET_EXPLOSION_OWNER.get() == player
                && isStingerFamilyWeapon(ROCKET_EXPLOSION_WEAPON.get());
    }

    /**
     * checks whether a projectile has launch metadata for one of the supported launchers.
     */
    public static boolean isLauncherRecorded(Entity projectile) {
        if (projectile == null || !projectile.getEntityData().hasKey("LSPECIALShooterWeapon")) return false;
        return isStingerFamilyWeapon(projectile.getEntityData().getString("LSPECIALShooterWeapon"));
    }

    /**
     * recognizes the configured rocket-family weapon IDs.
     */
    public static boolean isStingerFamilyWeapon(String weapon) {
        return "hbm:gun_stinger".equals(weapon) || "hbm:gun_quadro".equals(weapon)
                || "hbm:gun_panzerschreck".equals(weapon) || "hbm:gun_missile_launcher".equals(weapon);
    }

    /**
     * checks whether a stack belongs to the configured rocket family.
     */
    public static boolean isStingerFamilyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return false;
        return isStingerFamilyWeapon(stack.getItem().getRegistryName().toString());
    }

    /**
     * records which supported launcher a player's rocket came from, including an offhand launcher.
     */
    public static String getHeldStingerFamilyWeapon(EntityPlayer player) {
        if (player == null) return null;
        ItemStack mainhand = player.getHeldItemMainhand();
        if (isStingerFamilyItem(mainhand)) return mainhand.getItem().getRegistryName().toString();
        ItemStack offhand = player.getHeldItemOffhand();
        if (isStingerFamilyItem(offhand)) return offhand.getItem().getRegistryName().toString();
        return null;
    }

    /**
     * cancels direct impacts and explosion damage from supported NTM projectiles to their own shooter.
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!LSPECIALConfig.stingerSelfDamageProtection || event == null
                || !(event.getEntityLiving() instanceof EntityPlayer player)) return;

        DamageSource source = event.getSource();
        Entity immediate = source.getImmediateSource();
        Entity trueSource = source.getTrueSource();
        boolean tracked = isTrackedRocketExplosion(player);
        boolean heldFallback = source.isExplosion() && trueSource == player && isStingerFamilyHeld(player);
        boolean directProjectile = isSupportedProjectile(immediate) && getProjectileOwner(immediate) == player
                && (isLauncherRecorded(immediate) || isStingerFamilyHeld(player));
        boolean cancel = directProjectile || tracked || heldFallback;
        if (source.isExplosion() || directProjectile || tracked) {
            LSPECIALMod.info("RocketDebug", "LivingAttack player={} damageType={} explosion={} immediate={} trueSource={} immediateOwner={} recordedLauncher={} heldLauncher={} trackedContext={} directProjectileMatch={} cancel={}",
                    player.getName(), source.damageType, source.isExplosion(), describeEntity(immediate),
                    describeEntity(trueSource), describeEntity(immediate == null ? null : getProjectileOwner(immediate)),
                    isLauncherRecorded(immediate), isStingerFamilyHeld(player), tracked, directProjectile, cancel);
        }
        if (cancel) {
            event.setCanceled(true);
            LSPECIALMod.info("RocketDebug", "LivingAttack CANCELLED player={} damageType={} trackedContext={} heldLauncherFallback={} directProjectile={}",
                    player.getName(), source.damageType, tracked, heldFallback, directProjectile);
        }
    }

    /**
     * catches damage implementations that continue into the hurt stage despite an attack-stage handler.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!LSPECIALConfig.stingerSelfDamageProtection || event == null
                || !(event.getEntityLiving() instanceof EntityPlayer player)) return;
        DamageSource source = event.getSource();
        boolean tracked = isTrackedRocketExplosion(player);
        boolean heldFallback = source.isExplosion() && source.getTrueSource() == player && isStingerFamilyHeld(player);
        if (tracked || heldFallback) {
            LSPECIALMod.info("RocketDebug", "LivingHurt CANCELLED player={} damageType={} amount={} trackedContext={} heldLauncherFallback={}",
                    player.getName(), source.damageType, event.getAmount(), tracked, heldFallback);
            event.setCanceled(true);
        }
    }

    private static String describeEntity(Entity entity) {
        return entity == null ? "null" : entity.getClass().getName() + "#" + entity.getEntityId();
    }

    /**
     * limits direct-hit checks to NTM projectile classes that expose a shooter or thrower reference.
     */
    private static boolean isSupportedProjectile(Entity projectile) {
        return projectile instanceof EntityBulletBase
                || projectile instanceof EntityRocket || projectile instanceof EntityThrowableNT;
    }

    /**
     * resolves the firing entity from the supported NTM projectile ownership fields.
     */
    private static Entity getProjectileOwner(Entity projectile) {
        if (projectile instanceof EntityBulletBaseNT) return ((EntityBulletBaseNT) projectile).getThrower();
        if (projectile instanceof EntityBulletBase) return ((EntityBulletBase) projectile).shooter;
        if (projectile instanceof EntityRocket) return ((EntityRocket) projectile).shootingEntity;
        if (projectile instanceof EntityThrowableNT) return ((EntityThrowableNT) projectile).getThrower();
        return null;
    }

    /**
     * recognizes a configured launcher in either hand as a fallback for projectiles without launch metadata.
     */
    private static boolean isStingerFamilyHeld(EntityPlayer player) {
        return isStingerFamilyItem(player.getHeldItemMainhand())
                || isStingerFamilyItem(player.getHeldItemOffhand());
    }
}
