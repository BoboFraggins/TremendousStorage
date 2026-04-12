# Code Review: TremendousStorage

Reviewed 262 Java source files across all packages. The codebase is generally well-structured and readable. No crash-level bugs were found. Issues are grouped by category below.

---

## Crash / Correctness

**No crashes found.** All `Optional.get()` calls are properly guarded by preceding `isEmpty()` / `isPresent()` checks. Null returns from capability lookups are handled. The `input.copy().getCapability()` pattern in `TankBlockEntity` and `RecyclingBinBlockEntity` is intentional: the fluid handler mutates its container item, so the copy is needed to produce the correct output stack.

---

## Performance

No major hot-path issues. One minor note:

- **`NetworkInterfaceBFS.scan()`** allocates several `ArrayList`s and a `TreeMap` per scan. Scans are triggered by topology changes, not per-tick, so this is acceptable. No action needed.

---

## Code Duplication

### 1. `bitToWorldDir` — exact duplicate (minor)

`ChestBlockEntity.java:493–502` and `FilingCabinetBlockEntity.java:401–410` contain identical static methods:

```java
private static Direction bitToWorldDir(int bit, Direction facing) {
    return switch (bit) {
        case 0 -> Direction.UP;
        case 1 -> Direction.DOWN;
        case 2 -> facing.getCounterClockWise();
        case 3 -> facing.getClockWise();
        case 4 -> facing;
        default -> facing.getOpposite();
    };
}
```

**Suggestion:** Move to a shared utility (e.g., `shared/util/PullerUtil.java`).

---

### 2. `tickPuller` / `pullFromHandler` — near-duplicate (moderate)

Both `ChestBlockEntity.java:466–491` and `FilingCabinetBlockEntity.java:337–362` implement the same puller algorithm. The only difference is the `insert` method used (`ChestBlockEntity.insert(ItemStack, long, boolean)` returns a `long` remainder; `FilingCabinetBlockEntity` calls `pullerAbsorb`).

If a common interface or abstract base class is ever introduced for these two block entities, this logic is a natural candidate to pull up. Until then, changes to the puller behaviour must be applied in both places.

---

### 3. Ender smithing recipes — structural duplication (moderate)

`EnderChestSmithingRecipe`, `EnderBackpackSmithingRecipe`, `EnderTankSmithingRecipe`, and `EnderFolderSmithingRecipe` all share the same pattern verbatim:

```java
private static final ThreadLocal<long[]> PENDING_LINK = ThreadLocal.withInitial(() -> new long[] {-1L});

// assemble():
PENDING_LINK.get()[0] = linkId;

// getRemainingItems():
long linkId = PENDING_LINK.get()[0];
if (linkId != -1L) {
    PENDING_LINK.get()[0] = -1L;
    ...
}
```

The ThreadLocal is used correctly here — Minecraft's crafting pipeline always calls `assemble()` immediately before `getRemainingItems()` on the same server thread, and the value is cleared after each use. The data held (`long[]`) is tiny so there is no meaningful memory leak risk.

The duplication is purely structural: each of the four classes repeats ~30 lines of identical boilerplate. An abstract base class (e.g., `AbstractEnderSmithingRecipe`) could hold `PENDING_LINK` and the guard logic, leaving only `assemble()`, `makeEnderItem()`, and the ingredient predicates to differ per subclass.

---

## Code Quality

### Broad exception catch in optional integration

`BackpackItem.java:158`:

```java
} catch (NoClassDefFoundError | Exception ignored) {
    return ItemStack.EMPTY;
}
```

Catching the raw `Exception` type alongside `NoClassDefFoundError` is intentional — Curios is an optional dependency and the catch prevents crashes when it is absent. However, it also silently discards any unexpected runtime error (e.g., a `NullPointerException` inside the Curios API itself). Narrowing the catch to `NoClassDefFoundError | ClassNotFoundException | ExceptionInInitializerError` would be safer, though this is low risk in practice.

---

## Summary Table

| # | Location | Category | Severity |
|---|----------|----------|----------|
| 1 | `ChestBlockEntity:493`, `FilingCabinetBlockEntity:401` | Duplication — identical static method | Minor |
| 2 | `ChestBlockEntity:466–491`, `FilingCabinetBlockEntity:337–362` | Duplication — near-identical puller logic | Moderate |
| 3 | Four `Ender*SmithingRecipe` classes | Duplication — ThreadLocal boilerplate | Moderate |
| 4 | `BackpackItem:158` | Quality — overly broad exception catch | Minor |
