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

## Client item JSON files

Only items with special renderers have explicit `assets/tremendousstorage/items/` JSON files.
All other items rely on NeoForge's backwards-compatibility fallback to `models/item/<name>.json`.
If items appear with missing models in-game, create explicit client item JSON files for them.
