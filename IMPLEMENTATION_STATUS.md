# LSPECIAL complex TODO implementation status

## Implemented in this revision

- added server-side crouch + secondary ammo switching for Pepperbox (.44 FMJ), Star-F (.50 BMG FMJ), UZI (5.56 mm FMJ), G3 (10 gauge), and Minigun laser mode. loaded old-caliber ammunition is refunded before changing the magazine type.
- added the Lag's crouch + secondary self-kill action through the entity kill-command path.
- added the double-barrel grapple: secondary fire selects the nearest living entity inside a forward view cone and applies a server-side pull velocity. Range and pull speed are configurable.
- added the Tau secondary-fire damage multiplier.
- added the Lag and Henry Lincoln three-projectile secondary burst settings, damage fraction, and Lag secondary delay through NTM receiver getters.
- added Heretic-style held-secondary aiming and reduced both innate and ammunition spread for the listed lesser-perk weapons.
- added configuration switches and values for each of these behaviors.
- documented the complex mixin methods with lowercase-starting Javadocs and normalized Java source comments to start in lowercase.
- retained the existing Fat Man, Pew-Pew, drill capacity, strength tooltip, stat logic, and GUI changes.

## Still incomplete or constrained

- Perception found-loot duplication and Luck special-loot injection remain blocked because no reliable NTM found-loot inventory or injection API has been identified. ordinary mob/chest drops were not substituted.
- the double-barrel grapple's chain glyph rendering and crosshair V marker are not implemented; the gameplay pull is implemented.
- stinger-family self-damage suppression remains incomplete because the projectile's damage configuration does not identify which weapon fired it. disabling self-damage globally would affect unrelated NTM weapons, so that behavior was not broadened.
- controller navigation remains limited to whatever keybind integration NTM itself exposes; no separate controller library was added.

## Verification

- source archive integrity is checked after packaging.
- compilation and in-game behavior are not verified in this environment. use the project's configured Gradle wrapper and test on both an integrated and dedicated server before release.

## follow-up implementation: stinger, carbine, and loot perks

- added a server-side self-damage guard for supported NTM projectile classes when the player who fired the projectile is also the damage target and has a supported Stinger-family launcher in either hand. a projectile-impact mixin also cancels direct self-impact for `EntityBulletBaseNT`.
- added a Carbine higher-perk hook that disables native held-button refire and scales the native recoil callback using `carbineRecoilMultiplier`.
- added loot-table generation hooks. Perception duplicates generated non-stackable stacks at configurable thresholds (two total copies at the first threshold, three total at the second). Luck can add one extra copy of a generated result on tables whose resource path contains `luckRareLootTablePattern`, subject to the configured chance formula and cap.
- loot generation reads the looting player's scoreboard objectives. The loot-container hook temporarily exposes the player while vanilla `TileEntityLockableLoot` generates chest/container contents. Loot generated without a player context cannot receive player-specific perks.
- configuration keys added: `carbineSemiAutoPerk`, `carbineRecoilMultiplier`, `perceptionLootDuplication`, `perceptionLootDuplicateLevel`, `perceptionLootTripleLevel`, `luckRareLootBonus`, `luckRareLootLevel`, `luckRareLootChancePerPoint`, `luckRareLootChanceCap`, and `luckRareLootTablePattern`.
- compilation and in-game behavior remain unverified because the Gradle wrapper distribution could not be downloaded in this environment.


## light revolver rapid-fire burst

- secondary fire triggers the light revolver's native primary-fire state without requiring the player to hold primary fire.
- the exact hotbar ItemStack is locked for the configured burst duration; attempts to switch slots are corrected until the burst ends.
- the native receiver, ammo checks, firing cadence, projectile creation, and recoil remain in use.
- the `lightRevolverRapidFirePerk` option enables the perk and `lightRevolverRapidFireTicks` sets the burst duration (default 20 ticks).
- the lock ends when the duration expires, the stack leaves the hotbar, the player dies, or the perk is disabled.

## approved flamethrower and grapple follow-up

- the double-barrel grapple input is now sneak + secondary fire on both client and server. Its target filter accepts living entities, including other players.
- the Phlogistinator-style perk uses NTM's native receiver firing code for `hbm:gun_flamer`, enables native refire-on-hold while the configured Intelligence threshold is met, and exposes a configurable native firing delay.
- the Dragon's Fury-style perk hooks NTM's existing `EntityBulletBaseMK4.onImpact` path. It recognizes the four exact Topaz flame `BulletConfig` objects and temporarily multiplies damage only during direct entity-impact callbacks, restoring the original value afterward.
- new configuration keys: `phlogistinatorSustainedFirePerk`, `phlogistinatorSustainedFireLevel`, `phlogistinatorFireDelayTicks`, `dragonsFuryDirectHitPerk`, `dragonsFuryDirectHitLevel`, and `dragonsFuryDirectHitMultiplier`.
- compilation and in-game behavior remain unverified in this environment.


## latest requested weapon follow-up

- crouch + secondary switches Pepperbox to .44 FMJ, Star-F to .50 BMG FMJ, UZI (including akimbo) to 5.56 mm FMJ, and G3 (including Zebra variant) to 10 gauge. it empties the current magazine and returns complete old-ammo units to the player before changing the stored bullet configuration. if NTM's configured ammo bundle size makes an exact refund impossible, that magazine is not switched.
- crouch + secondary switches Minigun and Dual Minigun to the native `XFactoryEnergy.energy_las` beam projectile, then switches back to the magazine's first non-laser bullet configuration on the next use. the existing ammo-switch toggle and dedicated minigun option gate the feature.
- alternate-mode delays are configurable: Star-F .50 BMG 20 ticks, UZI 5.56 mm 2 ticks, G3 10 gauge 10 ticks, and minigun laser mode 4 ticks.
- secondary-fire hold-to-aim is copied to the Mare's Leg, Liberator, Auto Shotgun, Congo Lake, and AM180; the spread multiplier applies to both innate and ammunition spread. NTM's native Heretic item behavior is not intercepted.
- animation work still incomplete: the Lag's self-kill action does not play the requested empty-magazine animation; the double-barrel grapple still lacks its chain rendering and crosshair V marker; the alternate caliber/laser modes use NTM's existing firing/reload animations rather than bespoke mode-specific animations. these are visual tasks, not implemented by the ammo and aiming hooks.
- build and in-game verification remain unverified in this environment.


## Lag self-kill animation and grapple text visuals

- the Lag's crouch + secondary action now plays NTM's native `RELOAD_EMPTY` animation and schedules the self-kill 40 ticks later by default (2 seconds). the animation and delay each have their own configuration option.
- the double-barrel grapple now draws a simple `o-o-o-o...` text chain for a configurable short duration after activation, with the chain length based on target distance.
- while the player aims the double barrel at a living entity within grapple range and meets the Intelligence requirement, a lowercase `v` is drawn beside the normal crosshair.
- these are client-rendered text visuals; the actual grapple movement remains server-authoritative. build and in-game behavior remain unverified.
