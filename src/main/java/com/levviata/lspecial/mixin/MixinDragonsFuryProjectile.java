package com.levviata.lspecial.mixin;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALSounds;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// todo
// rationale: NTM owns EntityBulletBaseMK4 collision and fire spawning, but has no addon hook for per-shot range, hit streaks, or impact audio. This mixin limits only the four Topaz configs and wraps their existing impact damage; risks are conflicts with addons mixing into onUpdate/onImpact. Avoidable if NTM adds projectile lifecycle and impact callbacks.
@Mixin(value = EntityBulletBaseMK4.class, remap = false)
public abstract class MixinDragonsFuryProjectile {
    @Shadow
    public BulletConfig config;
    @Shadow
    public float damage;

    @Unique
    private float lspecial$damageBeforeImpact;
    @Unique
    private boolean lspecial$boostedImpactDamage;
    @Unique
    private EntityLivingBase lspecial$igniteAfterImpact;
    @Unique
    private boolean lspecial$dragonsFuryImpactResolved;
    @Unique
    private boolean lspecial$dragonsFuryOriginSet;
    @Unique
    private double lspecial$dragonsFuryOriginX;
    @Unique
    private double lspecial$dragonsFuryOriginY;
    @Unique
    private double lspecial$dragonsFuryOriginZ;

    @Unique
    private static ItemStack lspecial$getDragonsFuryStack(EntityLivingBase shooter) {
        if (!(shooter instanceof EntityPlayer player)) return ItemStack.EMPTY;
        ItemStack held = player.getHeldItemMainhand();
        if (lspecial$isDragonsFuryWeapon(held)) return held;
        held = player.getHeldItemOffhand();
        if (lspecial$isDragonsFuryWeapon(held)) return held;
        for (ItemStack candidate : player.inventory.mainInventory) {
            if (lspecial$isDragonsFuryWeapon(candidate)) return candidate;
        }
        return ItemStack.EMPTY;
    }

    @Unique
    private static boolean lspecial$isDragonsFuryWeapon(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getRegistryName() != null
                && "hbm:gun_flamer_topaz".equals(stack.getItem().getRegistryName().toString());
    }

    @Unique
    private static boolean lspecial$isDragonsFuryFlame(BulletConfig bulletConfig) {
        return bulletConfig != null && (bulletConfig == XFactoryFlamer.flame_topaz_diesel
                || bulletConfig == XFactoryFlamer.flame_topaz_gas
                || bulletConfig == XFactoryFlamer.flame_topaz_napalm
                || bulletConfig == XFactoryFlamer.flame_topaz_balefire);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void lspecial$limitDragonsFuryRange(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (projectile.world.isRemote || !LSPECIALConfig.dragonsFuryPerk) return;
        BulletConfig activeConfig = config != null ? config : projectile.getBulletConfig();
        if (!lspecial$isDragonsFuryFlame(activeConfig)) return;

        if (!lspecial$dragonsFuryOriginSet) {
            lspecial$dragonsFuryOriginX = projectile.posX;
            lspecial$dragonsFuryOriginY = projectile.posY;
            lspecial$dragonsFuryOriginZ = projectile.posZ;
            lspecial$dragonsFuryOriginSet = true;
        }

        double dx = projectile.posX - lspecial$dragonsFuryOriginX;
        double dy = projectile.posY - lspecial$dragonsFuryOriginY;
        double dz = projectile.posZ - lspecial$dragonsFuryOriginZ;
        double travelled = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (travelled >= 8.0D) {
            lspecial$resetMissedStreak(projectile);
            projectile.setDead();
            callback.cancel();
            return;
        }

        double speed = activeConfig.velocity > 0.0F ? activeConfig.velocity : (206.0D / 3.6D / 20.0D);
        double motionLength = Math.sqrt(projectile.motionX * projectile.motionX
                + projectile.motionY * projectile.motionY + projectile.motionZ * projectile.motionZ);
        if (motionLength > 0.0001D) {
            double adjustedSpeed = Math.min(speed, Math.max(0.0D, 8.0D - travelled));
            double factor = adjustedSpeed / motionLength;
            projectile.motionX *= factor;
            projectile.motionY *= factor;
            projectile.motionZ *= factor;
        }
    }

    @Inject(method = "onUpdate", at = @At("RETURN"), remap = false)
    private void lspecial$finishDragonsFuryRange(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (projectile.world.isRemote || !LSPECIALConfig.dragonsFuryPerk || !lspecial$isDragonsFuryFlame(config) || !lspecial$dragonsFuryOriginSet)
            return;
        double dx = projectile.posX - lspecial$dragonsFuryOriginX;
        double dy = projectile.posY - lspecial$dragonsFuryOriginY;
        double dz = projectile.posZ - lspecial$dragonsFuryOriginZ;
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) >= 7.999D) {
            if (!lspecial$dragonsFuryImpactResolved) lspecial$resetMissedStreak(projectile);
            projectile.setDead();
        }
    }

    @Inject(method = "onUpdate", at = @At("HEAD"), remap = false)
    private void lspecial$renderDragonsFuryTrail(CallbackInfo callback) {
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (!projectile.world.isRemote || !LSPECIALConfig.dragonsFuryPerk || !lspecial$isDragonsFuryFlame(config))
            return;
        for (int i = 0; i < 3; i++) {
            double spreadX = (projectile.world.rand.nextDouble() - 0.5D) * 0.12D;
            double spreadY = (projectile.world.rand.nextDouble() - 0.5D) * 0.12D;
            double spreadZ = (projectile.world.rand.nextDouble() - 0.5D) * 0.12D;
            projectile.world.spawnParticle(EnumParticleTypes.FLAME, projectile.posX, projectile.posY, projectile.posZ,
                    -projectile.motionX * 0.025D + spreadX, -projectile.motionY * 0.025D + spreadY,
                    -projectile.motionZ * 0.025D + spreadZ);
        }
    }

    @Inject(method = "onImpact", at = @At("HEAD"), remap = false)
    private void lspecial$applyDragonsFuryHitRules(RayTraceResult hit, CallbackInfo callback) {
        lspecial$boostedImpactDamage = false;
        lspecial$igniteAfterImpact = null;
        EntityBulletBaseMK4 projectile = (EntityBulletBaseMK4) (Object) this;
        if (projectile.world.isRemote || !LSPECIALConfig.dragonsFuryPerk || hit == null) return;

        if (lspecial$isDragonsFuryFlame(config)) {
            if (hit.typeOfHit != RayTraceResult.Type.ENTITY && hit.typeOfHit != RayTraceResult.Type.BLOCK) return;
            lspecial$dragonsFuryImpactResolved = true;
            EntityLivingBase shooter = projectile.getThrower();
            ItemStack weapon = lspecial$getDragonsFuryStack(shooter);
            net.minecraft.nbt.NBTTagCompound weaponTag = null;
            if (!weapon.isEmpty() && weapon.getItem().getRegistryName() != null
                    && "hbm:gun_flamer_topaz".equals(weapon.getItem().getRegistryName().toString())) {
                weaponTag = weapon.getTagCompound();
                if (weaponTag == null) {
                    weaponTag = new net.minecraft.nbt.NBTTagCompound();
                    weapon.setTagCompound(weaponTag);
                }
            }
            int streak = weaponTag == null ? 0 : Math.max(0, weaponTag.getInteger("LSPECIALDragonsFuryHitStreak"));
            boolean critical = streak >= 4;
            if (weaponTag != null) weaponTag.setInteger("LSPECIALDragonsFuryHitStreak", critical ? 0 : streak + 1);

            boolean miniCrit = projectile.getEntityData().getBoolean("LSPECIALAirblastMiniCrit");
            if (hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit instanceof EntityLivingBase target) {
                lspecial$igniteAfterImpact = target;
                float multiplier = target.isBurning() ? 3.0F : 1.0F;
                if (miniCrit) multiplier *= 1.35F;
                if (critical) multiplier *= 3.0F;
                if (multiplier != 1.0F) {
                    lspecial$damageBeforeImpact = damage;
                    damage *= multiplier;
                    lspecial$boostedImpactDamage = true;
                }
                if (weaponTag != null) weaponTag.setBoolean("LSPECIALDragonsFuryNextShotFast", true);
            }

            if (hit.hitVec != null) {
                net.minecraft.util.SoundEvent impactSound = critical ? LSPECIALSounds.DRAGONS_FURY_IMPACT_CRIT
                        : miniCrit ? LSPECIALSounds.DRAGONS_FURY_IMPACT_MINICRIT : LSPECIALSounds.DRAGONS_FURY_IMPACT;
                BlockPos soundPos = new BlockPos(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z);
                projectile.world.playSound(null, soundPos, impactSound, SoundCategory.PLAYERS, 0.9F, 1.0F);
            }
            if (miniCrit) projectile.getEntityData().removeTag("LSPECIALAirblastMiniCrit");
            return;
        }

        if (hit.typeOfHit != RayTraceResult.Type.ENTITY || !(hit.entityHit instanceof EntityLivingBase)) return;
        String weaponId = projectile.getEntityData().getString("LSPECIALShooterWeapon");
        if (projectile.getEntityData().getBoolean("LSPECIALAirblastMiniCrit")) {
            lspecial$damageBeforeImpact = damage;
            damage = damage * 1.35F;
            lspecial$boostedImpactDamage = true;
            projectile.getEntityData().removeTag("LSPECIALAirblastMiniCrit");
        }
        if ("hbm:gun_tau".equals(weaponId) && projectile.getEntityData().getBoolean("LSPECIALShooterSecondary")
                && LSPECIALConfig.tauSecondaryDamageBonus && config == XFactoryAccelerator.tau_uranium_charge) {
            if (!lspecial$boostedImpactDamage) lspecial$damageBeforeImpact = damage;
            damage = (float) (damage * LSPECIALConfig.tauSecondaryDamageMultiplier);
            lspecial$boostedImpactDamage = true;
        }
    }

    @Inject(method = "onImpact", at = @At("RETURN"), remap = false)
    private void lspecial$finishDragonsFuryHitRules(RayTraceResult hit, CallbackInfo callback) {
        if (lspecial$boostedImpactDamage) {
            damage = lspecial$damageBeforeImpact;
            lspecial$boostedImpactDamage = false;
        }
        if (lspecial$igniteAfterImpact != null && !lspecial$igniteAfterImpact.isDead) {
            lspecial$igniteAfterImpact.setFire(3);
            lspecial$igniteAfterImpact = null;
        }
    }

    @Unique
    private void lspecial$resetMissedStreak(EntityBulletBaseMK4 projectile) {
        ItemStack held = lspecial$getDragonsFuryStack(projectile.getThrower());
        if (held.isEmpty()) return;
        net.minecraft.nbt.NBTTagCompound tag = held.getTagCompound();
        if (tag != null) tag.setInteger("LSPECIALDragonsFuryHitStreak", 0);
    }
}
