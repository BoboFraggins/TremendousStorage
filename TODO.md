# Upgrade TODOs

## Commented-out dependencies (no 1.21.4 builds available)

- **EMI** (`maven.modrinth:emi`) — latest build is for 1.21.1. Re-enable when 1.21.4 build is released.
  - Affected: `src/main/java/net/bobofraggins/tremendousstorage/integration/emi/`
- **REI** (`me.shedaniel:RoughlyEnoughItems-neoforge`) — 1.21.4 version unconfirmed.
  - Affected: `src/main/java/net/bobofraggins/tremendousstorage/external/rei/`
- **Mekanism** (`mekanism:Mekanism:api`) — no 1.21.4 build.
  - Affected: any Mekanism integration classes
- **Ars Nouveau** (`com.hollingsworth.ars_nouveau`) — no confirmed 1.21.4 build.
  - Affected: any Ars Nouveau integration classes
- **Cucumber** (`com.blakebr0.cucumber`) — no 1.21.4 build.
- **Mystical Agriculture** (`com.blakebr0.mysticalagriculture`) — no 1.21.4 build.
  - Affected: any Mystical Agriculture integration classes
- **Create** (`curse.maven:create-328085`) — 1.21.4 version unconfirmed.
  - Affected: any Create integration classes
- **Structure Pool API** (`maven.modrinth:structure-pool-api`) — no 1.21.4 build confirmed.

## Remaining deprecation warnings (not yet fixed)

- `removeComponentsFromTag(CompoundTag)` override in `BarrelBlockEntity` and `TankBlockEntity` — deprecated in NeoForge, replacement unclear.
- `IVanillaRecipeFactory.createBrewingRecipe()` in `TremendousStorageJeiPlugin` — JEI deprecated this method.
- `EmiPlayerInventory(Player)` in `TerminalEmiRecipeHandler` — moot while EMI is disabled.

## Features lost or degraded in the 26.1.2 upgrade

### Confirmed losses

- **Custom fluid textures and tints (Positive Vibes, XP Juice, Honey)**
  `IClientFluidTypeExtensions` texture/tint registration was removed. Custom still/flowing textures
  and tint colours for all three fluids were replaced with empty no-op registrations. In-world fluid
  blocks and the cauldron likely render with a fallback or missing texture.
  Affected: `XpJuiceClientEvents`, `PositiveVibesClientEvents`, `HoneyClientEvents`

- **Jade HUD: tube attachment icon**
  `TubeJadeComponentProvider.getIcon()` used to show the attachment's item icon (e.g. Import
  Interface) in the Jade tooltip. Jade 26.1.1 removed `IElementHelper` with no documented
  replacement for creating item icon elements; the method now always returns the default tube block
  icon.
  Affected: `TubeJadeComponentProvider`

### Degraded behaviour

- **JEI folder recipe ingredient search highlighting**
  The four `FolderRecipeExtensions` classes return an empty list from `getIngredients()` because
  the custom recipe types do not expose a standard ingredient list. JEI uses this for
  focus/search highlighting; clicking an item in JEI no longer highlights it as a relevant
  ingredient for folder recipes (display via `setRecipe` is unchanged).
  Affected: `FolderRecipeExtensions`

- **HAARP weather changes may not sync to clients immediately**
  `ServerLevel.setWeatherParameters()` sent an immediate client packet. The replacement
  (`serverLevel.getWeatherData()` setters) is a `SavedData` mutation that may only propagate to
  clients on the server's next natural weather broadcast cycle rather than instantly.
  Affected: `WirelessHubBlockEntity`

- **Chest renderer: no directional shading on tier colour overlay**
  `Level.getShade(Direction)` was removed. The chest's colour tint is now applied uniformly across
  all faces instead of being slightly darkened on bottom/side faces. In practice the `level`
  parameter was already always `null` in the renderer calls, so this was already a no-op at
  runtime.
  Affected: `ChestRenderer`

- **Positive Vibes regeneration effect: feet-only fluid detection**
  `LivingEntity.isInFluidType(FluidType)` was removed. The replacement checks
  `level.getFluidState(player.blockPosition())`, which only tests the block at the player's feet.
  A player at the edge of the fluid, or with only their head submerged, may not receive the
  regeneration effect.
  Affected: `PositiveVibesEffectHandler`

## Client item JSON files

Only items with special renderers have explicit `assets/tremendousstorage/items/` JSON files.
All other items rely on NeoForge's backwards-compatibility fallback to `models/item/<name>.json`.
If items appear with missing models in-game, create explicit client item JSON files for them.
