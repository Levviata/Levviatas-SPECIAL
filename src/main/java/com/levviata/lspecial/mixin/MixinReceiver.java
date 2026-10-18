package com.levviata.lspecial.mixin;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.levviata.lspecial.LSPECIALConfig;
import com.levviata.lspecial.LSPECIALExpression;
import com.levviata.lspecial.LSPECIALSounds;
import com.levviata.lspecial.ReceiverStateAccess;
import com.levviata.lspecial.SPECIALScoreboard;
import com.levviata.lspecial.SPECIALPlayerContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Applies Intelligence perk firing delays without permanently mutating NTM receiver data. */
// rationale: NTM has no public per-shot sound selector for the Topaz flamer, so this changes Receiver#getFireSound only for that item. Other addons targeting this method may conflict; an NTM sound callback would avoid the mixin.
@Mixin(value = Receiver.class, remap = false)
public abstract class MixinReceiver implements ReceiverStateAccess {

    @Shadow protected int index;
    @Shadow protected boolean refireOnHold_DNA;
    @Shadow protected boolean refireAfterDry_DNA;

    @Override
    public boolean lspecial$getNativeRefireOnHold() {
        return refireOnHold_DNA;
    }

    @Override
    public boolean lspecial$getNativeRefireAfterDry() {
        return refireAfterDry_DNA;
    }

    /** calculates firing delay from the native receiver value so switching weapons cannot retain another gun's delay. */
    @Inject(method = "getDelayAfterFire", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$calculateFiringDelay(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (stack == null || stack.isEmpty() || !LSPECIALConfig.modifyGuns)  {
            return;
        }

        int delay = callback.getReturnValueI();
        int intelligence = lspecial$getIntelligence();
        BulletConfig activeAmmo = lspecial$getActiveAmmo(stack);

        if (LSPECIALConfig.intelligence && intelligence >= 0) {
            Map<String, Double> variables = new HashMap<>();
            variables.put("intelligence", (double) intelligence);
            variables.put("originalDelay", (double) delay);
            int modifiedDelay = (int) Math.round(delay - LSPECIALExpression.eval(LSPECIALConfig.bonusDelayFormula, variables, 0.0D));

            if (modifiedDelay > 0 && LSPECIALConfig.buckshotDelayNerf && lspecial$isBuckshotAmmo(activeAmmo)) {
                modifiedDelay = (int) Math.round(LSPECIALExpression.eval(
                        LSPECIALConfig.buckshotDelayFormula, variables, (double) modifiedDelay));
            }
            delay = Math.max(0, modifiedDelay);
        }

        String itemId = lspecial$registryId(stack);
        if ("hbm:gun_fatman".equals(itemId) && LSPECIALConfig.intelligence && LSPECIALConfig.revolverPerk && intelligence >= 15 && LSPECIALConfig.fatmanHigherPerk && LSPECIALConfig.fatmanHigherPerkDelay) {
            delay = LSPECIALConfig.fatmanHigherPerkDelayTicks;
        } else if ("hbm:gun_laser_pistol".equals(itemId) && LSPECIALConfig.laserPistolDelayBalance && LSPECIALConfig.intelligence && LSPECIALConfig.revolverPerk && intelligence >= 15) {
            delay = LSPECIALConfig.laserPistolDelayTicks;
        } else if ("hbm:gun_flamer_topaz".equals(itemId)) {
            net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
            boolean hitBonus = tag != null && tag.getBoolean("LSPECIALDragonsFuryNextShotFast");
            delay = hitBonus ? 11 : 16; // 0.8 s base pressure cycle; 50% faster repressurization after a hit.
            if (hitBonus) tag.setBoolean("LSPECIALDragonsFuryNextShotFast", false);
        } else if (LSPECIALConfig.weaponSecondaryAmmoCycling && lspecial$isTertiaryMode(stack)) {
            if (lspecial$isStarF(itemId)) {
                delay = LSPECIALConfig.starFFiftyBmgDelayTicks;
            } else if (lspecial$isUzi(itemId)) {
                delay = LSPECIALConfig.uzi556DelayTicks;
            } else if (lspecial$isG3(itemId)) {
                delay = LSPECIALConfig.g3TenGaugeDelayTicks;
            }
        }

        callback.setReturnValue(Math.max(0, delay));
    }

    /** returns the player's Intelligence score, or zero when the scoreboard is not ready. */
    @Unique
    private static int lspecial$getIntelligence() {
        EntityPlayer player = SPECIALPlayerContext.getPlayer();
        if (SPECIALScoreboard.inst == null || player == null) {
            return 0;
        }
        return SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD);
    }

    /** checks the selected ammunition configuration rather than any matching item in the inventory. */
    @Unique
    private static boolean lspecial$isBuckshotAmmo(BulletConfig config) {
        if (config == null || config.ammo == null) {
            return false;
        }

        ItemStack ammo = config.ammo.getStack();

        if (ammo.isEmpty()) {
            return false;
        }

        for (String raw : LSPECIALConfig.buckshotGaugeList.split(",")) {
            String token = raw.trim();
            try {
                if (token.matches("\\d+") && ammo.getItem() == ModItems.ammo_standard
                        && ammo.getMetadata() == Integer.parseInt(token)) return true;
                String[] parts = token.split(":");
                if (parts.length == 3 && "hbm".equals(parts[0]) && "ammo_secret".equals(parts[1])
                        && ammo.getItem() == ModItems.ammo_secret
                        && ammo.getMetadata() == Integer.parseInt(parts[2])) return true;
            } catch (NumberFormatException ignored) {
                // ignore malformed gauge entries in the configuration.
            }
        }
        return false;
    }

    /** reads the active magazine bullet configuration without forcing an inventory-based caliber choice. */
    @Unique
    private BulletConfig lspecial$getActiveAmmo(ItemStack stack) {
        IMagazine magazine = ((Receiver) (Object) this).getMagazine(stack);

        if (magazine == null) {
            return null;
        }

        Object ammo = magazine.getType(stack, null);
        return ammo instanceof BulletConfig ? (BulletConfig) ammo : null;
    }

    @Unique
    private static boolean lspecial$isTertiaryMode(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean("LSPECIALTertiaryMode");
    }

    @Unique
    private static boolean lspecial$isStarF(String itemId) {
        return "hbm:gun_star_f".equals(itemId) || "hbm:gun_star_f_akimbo".equals(itemId);
    }

    @Unique
    private static boolean lspecial$isUzi(String itemId) {
        return "hbm:gun_uzi".equals(itemId) || "hbm:gun_uzi_akimbo".equals(itemId);
    }

    @Unique
    private static boolean lspecial$isG3(String itemId) {
        return "hbm:gun_g3".equals(itemId) || "hbm:gun_g3_zebra".equals(itemId);
    }

    /** selects the custom projectile sound for the Dragon's Fury fireball. */
    @Inject(method = "getFireSound", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$useSecondaryFlamerSound(ItemStack stack, CallbackInfoReturnable<SoundEvent> callback) {
        String itemId = lspecial$registryId(stack);
        if (LSPECIALConfig.dragonsFuryPerk && "hbm:gun_flamer_topaz".equals(itemId)) {
            net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
            boolean criticalShot = tag != null && tag.getInteger("LSPECIALDragonsFuryHitStreak") >= 4;
            callback.setReturnValue(criticalShot ? LSPECIALSounds.DRAGONS_FURY_SHOOT_CRIT
                    : LSPECIALSounds.DRAGONS_FURY_SHOOT);
        }
    }

    /** Keeps the native Carbine semi-auto perk without changing Lag/Lincoln refire behavior. */
    @Inject(method = "getRefireOnHold", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$configureSecondaryRefire(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (LSPECIALConfig.carbineSemiAutoPerk && lspecial$isCarbineHigherPerk(stack) && lspecial$isSecondaryHeld(stack)) {
            callback.setReturnValue(false);
        }
    }

    /** scales the native Carbine recoil callback without replacing its ordinary firing implementation. */
    @Inject(method = "getRecoil", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$scaleCarbineRecoil(ItemStack stack,
            CallbackInfoReturnable<BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext>> callback) {
        if (!lspecial$isCarbineHigherPerk(stack)) return;
        final double multiplier = LSPECIALConfig.carbineRecoilMultiplier;
        final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> original = callback.getReturnValue();
        if (original == null) return;
        callback.setReturnValue((gunStack, context) -> {
            EntityLivingBase entity = context == null ? null : context.entity;
            if (entity == null || multiplier >= 0.999999D) {
                original.accept(gunStack, context);
                return;
            }
            float pitchBefore = entity.rotationPitch;
            float yawBefore = entity.rotationYaw;
            float headYawBefore = entity.rotationYawHead;
            original.accept(gunStack, context);
            float pitchDelta = entity.rotationPitch - pitchBefore;
            float yawDelta = MathHelper.wrapDegrees(entity.rotationYaw - yawBefore);
            float headYawDelta = MathHelper.wrapDegrees(entity.rotationYawHead - headYawBefore);
            entity.rotationPitch = pitchBefore + (float) (pitchDelta * multiplier);
            entity.rotationYaw = yawBefore + (float) (yawDelta * multiplier);
            entity.rotationYawHead = headYawBefore + (float) (headYawDelta * multiplier);
        });
    }

    /** checks that the stack is the Carbine and the higher Intelligence perk is active. */
    @Unique
    private static boolean lspecial$isCarbineHigherPerk(ItemStack stack) {
        EntityPlayer player = SPECIALPlayerContext.getPlayer();
        return stack != null && !stack.isEmpty() && LSPECIALConfig.intelligence
                && LSPECIALConfig.revolverPerk && SPECIALScoreboard.inst != null && player != null
                && SPECIALScoreboard.inst.getPlayerScore(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) != null
                && SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) >= 15
                && "hbm:gun_carbine".equals(lspecial$registryId(stack));
    }

    /** tightens spread while the supported lesser-perk weapons are scoped. */
    @Inject(method = "getInnateSpread", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$adjustScopedSpread(ItemStack stack, CallbackInfoReturnable<Float> callback) {
        if (stack == null || stack.isEmpty() || !LSPECIALConfig.scopedSecondarySpread
                || !ItemGunBaseNT.getIsAiming(stack) || !lspecial$hasIntelligence(5)) return;
        String itemId = lspecial$registryId(stack);
        if ("hbm:gun_maresleg".equals(itemId) || "hbm:gun_liberator".equals(itemId)
                || "hbm:gun_autoshotgun".equals(itemId) || "hbm:gun_autoshotgun_heretic".equals(itemId)
                || "hbm:gun_autoshotgun_sexy".equals(itemId)
                || "hbm:gun_congolake".equals(itemId) || "hbm:gun_am180".equals(itemId)) {
            callback.setReturnValue((float) (callback.getReturnValue() * LSPECIALConfig.scopedSpreadMultiplier));
        }
    }

    /** tightens ammunition-derived spread as well as innate spread while a copied scope is active. */
    @Inject(method = "getAmmoSpread", at = @At("RETURN"), cancellable = true, remap = false)
    private void lspecial$adjustScopedAmmoSpread(ItemStack stack, CallbackInfoReturnable<Float> callback) {
        if (stack == null || stack.isEmpty() || !LSPECIALConfig.scopedSecondarySpread
                || !ItemGunBaseNT.getIsAiming(stack) || !lspecial$hasIntelligence(5)) return;
        String itemId = lspecial$registryId(stack);
        if ("hbm:gun_maresleg".equals(itemId) || "hbm:gun_liberator".equals(itemId)
                || "hbm:gun_autoshotgun".equals(itemId) || "hbm:gun_autoshotgun_heretic".equals(itemId)
                || "hbm:gun_autoshotgun_sexy".equals(itemId)
                || "hbm:gun_congolake".equals(itemId) || "hbm:gun_am180".equals(itemId)) {
            callback.setReturnValue((float) (callback.getReturnValue() * LSPECIALConfig.scopedSpreadMultiplier));
        }
    }

    /** checks the shared SPECIAL score safely because NTM can query receiver values on either side. */
    @Unique
    private static boolean lspecial$hasIntelligence(int level) {
        EntityPlayer player = SPECIALPlayerContext.getPlayer();
        return LSPECIALConfig.intelligence
                && (level >= 15 ? LSPECIALConfig.revolverPerk : LSPECIALConfig.pepperboxPerk)
                && SPECIALScoreboard.inst != null && player != null
                && SPECIALScoreboard.inst.getPlayerScore(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) != null
                && SPECIALScoreboard.inst.getPlayerScorePoints(player, SPECIALScoreboard.inst.INTELLIGENCE_BOARD) >= level;
    }

    /** checks the NTM per-config secondary input state stored on the weapon stack. */
    @Unique
    private boolean lspecial$isSecondaryHeld(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (ItemGunBaseNT.getSecondary(stack, index)
                || (stack.hasTagCompound() && stack.getTagCompound().getBoolean("LSPECIALSecondaryMode")));
    }

    /** returns the namespaced item ID without depending on translated display text. */
    @Unique
    private static String lspecial$registryId(ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null
                ? "" : stack.getItem().getRegistryName().toString();
    }
}
