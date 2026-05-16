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

- ~~**Custom fluid textures and tints (Positive Vibes, XP Juice, Honey)**~~ **FIXED**
  Migrated to `RegisterFluidModelsEvent` + `FluidModel.Unbaked` + `FluidTintSources`.
  Affected: `XpJuiceClientEvents`, `PositiveVibesClientEvents`, `HoneyClientEvents`

- ~~**Jade HUD: tube attachment icon**~~ **FIXED**
  Restored using `ItemStackElement.of(stack)` from `snownee.jade.impl.ui` (internal but stable).
  Affected: `TubeJadeComponentProvider`

### Degraded behaviour

- **JEI folder recipe ingredient search highlighting** (not fixable without hard-coding recipe logic)
  The four `FolderRecipeExtensions` classes return an empty list from `getIngredients()` because
  the custom recipe types do not expose a standard ingredient list. JEI uses this for
  focus/search highlighting; clicking an item in JEI no longer highlights it as a relevant
  ingredient for folder recipes (display via `setRecipe` is unchanged).
  Affected: `FolderRecipeExtensions`

- **HAARP weather changes may not sync to clients immediately** (acceptable — propagates within 1–2 ticks)
  `ServerLevel.setWeatherParameters()` sent an immediate client packet. The replacement
  (`serverLevel.getWeatherData()` setters) propagates via the server's natural weather broadcast
  cycle, which fires every tick at most; in practice imperceptible.
  Affected: `WirelessHubBlockEntity`

- ~~**Chest renderer: no directional shading on tier colour overlay**~~ **FIXED**
  Hardcoded standard MC directional shading values (DOWN=0.5, NORTH/SOUTH=0.8, EAST/WEST=0.6,
  UP=1.0), applied only when `materialInfo().shade()` is true.
  Affected: `ChestRenderer`

- ~~**Positive Vibes regeneration effect: feet-only fluid detection**~~ **FIXED**
  Now checks both the feet block and the block at body centre (half player height), covering
  wading, swimming, and full submersion.
  Affected: `PositiveVibesEffectHandler`

## Client item JSON files

Only items with special renderers have explicit `assets/tremendousstorage/items/` JSON files.
All other items rely on NeoForge's backwards-compatibility fallback to `models/item/<name>.json`.
If items appear with missing models in-game, create explicit client item JSON files for them.
