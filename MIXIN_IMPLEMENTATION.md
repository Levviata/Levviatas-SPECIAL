# LSPECIAL Mixin integration

The mixins integrate with NTM's native Sedna weapon APIs and avoid replacing the overall gun implementation.

## Implemented hooks

- `MixinReceiver` adjusts Fat Man firing delay, Tau secondary damage, Lag/Henry Lincoln secondary burst values, and scoped spread.
- `MixinMagazineSingleTypeBase` applies configured Fat Man and Pew-Pew magazine capacities while the Intelligence higher perk is active.
- `MixinMagazineFluid` adds configured fluid capacity to the drill.
- `MixinWeaponSecondary` handles crouch-secondary ammunition cycling, the Lag self-kill action, and the double-barrel entity grapple on the server.

## Behavior and limitations

- ammo switching adds only the explicitly selected native NTM bullet configuration to the relevant magazine list, then updates the stored magazine type after refunding loaded ammunition.
- the grapple's target selection and pull are server-authoritative. chain rendering and the crosshair marker are not yet included.
- the TODOs that require a missing loot injection point or weapon-origin metadata remain listed in `IMPLEMENTATION_STATUS.md`.

## follow-up hooks

- `MixinStingerProjectile` cancels direct self-impact for the supported launcher family. `StingerDamageHandler` also checks NTM projectile ownership during living-attack events, including NTM throwable, bullet, and rocket classes, and requires the matching launcher to be held in either hand.
- `MixinReceiver` disables Carbine automatic refire at the receiver getter and wraps the native recoil callback to scale the rotation delta. `carbineRecoilMultiplier` is `0` for no recoil and `1` for unchanged native recoil.
- `MixinLootContainer`, `LootPerkHandler`, and `MixinLootTable` provide player-aware Perception duplication and Luck bonus rolls. Luck's rare-table matching is based on the configured path substring rather than a hardcoded list of NTM table IDs.
- player-specific loot perks require a player context. The container hook covers vanilla lockable loot containers; mob and other loot generators use the killer-player context when provided. Non-player-generated loot is left unchanged.


### light revolver rapid-fire

`MixinWeaponSecondary` uses the native secondary-fire keybind as the activation edge. `SPECIALGameplayHandler` records the exact stack identity, hotbar slot, and duration, then holds native primary input while preventing slot changes. `MixinLightRevolverRapidFire` keeps the native primary state asserted only while that stack owns an active burst. This preserves NTM's receiver firing code instead of simulating projectiles separately.

### Phlogistinator and Dragon's Fury

- `MixinReceiver` enables NTM's native held-fire refire path for `hbm:gun_flamer` while the configurable Intelligence perk is active. The native receiver still performs ammunition checks, projectile creation, wear, and recoil; the configured firing delay is applied through `getDelayAfterFire`.
- `MixinDragonFuryProjectile` recognizes the exact Topaz flamer `BulletConfig` instances from `XFactoryFlamer` and temporarily scales `EntityBulletBaseMK4.damage` only during direct entity-impact callbacks. It restores the original damage at method return and does not scale block impacts. The shooter must be a player and meet the configured Intelligence level.
- the double-barrel grapple now activates on sneak + secondary fire as requested. Entity targeting continues to accept `EntityLivingBase`, which includes players as well as mobs.


## latest ammunition and aiming follow-up

- `MixinWeaponSecondary` now selects explicit native NTM bullet configs for Pepperbox (.44 FMJ), Star-F (.50 BMG FMJ), UZI (5.56 mm FMJ), and G3 (10 gauge), including the relevant akimbo/variant IDs. it returns the loaded old caliber as inventory ammunition before setting the new magazine type.
- the Minigun and Dual Minigun use `XFactoryEnergy.energy_las` for the alternate laser mode, reusing the native laser beam projectile configuration.
- `MixinReceiver` applies configured firing delays while the alternate caliber is selected and reduces both innate and ammunition spread while copied scope aiming is active.
- `MixinMagazineFluid` already applies the configured drill fluid-capacity bonus additively to NTM's returned capacity.
