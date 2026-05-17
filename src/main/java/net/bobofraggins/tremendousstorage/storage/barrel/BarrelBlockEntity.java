package net.bobofraggins.tremendousstorage.storage.barrel;

import java.util.List;
import java.util.Optional;
import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.shared.util.PullerUtil;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkListable;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class BarrelBlockEntity extends BlockEntity implements MenuProvider, NetworkListable {

    /** Type key — always count=1; empty means unlocked. */
    protected ItemStack storedItem = ItemStack.EMPTY;

    protected long count = 0L;
    protected StorageTier tier = StorageTier.WOOD;
    protected boolean voidExcess = false;
    protected boolean compactingUpgrade = false;
    // Which IItemHandler slot (0–2) the storedItem appears in; slots below this are empty.
    protected int baseSlot = 0;
    protected boolean hasPullerUpgrade = false;
    protected int pullerSides = 0;
    protected int pullerTickCounter = 0;
    protected Priority priority = Priority.NORMAL;

    private static final int PULL_TICKS = 4;
    private static final int PULL_AMOUNT = 4;

    // Compacting recipe cache — server-side only, rebuilt lazily when locked item or baseSlot changes
    ItemStack compactTier1Item = ItemStack.EMPTY;
    int compactTier1Ratio = 0;
    ItemStack compactTier2Item = ItemStack.EMPTY;
    int compactTier2Ratio = 0;
    protected ItemStack compactCacheKey = null;
    protected int compactCacheBaseSlot = -1;

    private final NiLink niLink = new NiLink();

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        this(BETypeHelper.get("barrel"), pos, state);
    }

    protected BarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean isLocked() {
        return !storedItem.isEmpty();
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public long getCount() {
        return count;
    }

    public long getCapacity() {
        return compactingUpgrade ? tier.getCapacity() * 9L : tier.getCapacity();
    }

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BarrelBlock.setTierBlockState(level, worldPosition, tier);
        }
    }

    public boolean isVoidExcess() {
        return voidExcess;
    }

    public void setVoidExcess(boolean value) {
        this.voidExcess = value;
        setChanged();
    }

    public boolean hasCompactingUpgrade() {
        return compactingUpgrade;
    }

    public void setCompactingUpgrade(boolean value) {
        this.compactingUpgrade = value;
        compactCacheKey = null;
        setChanged();
    }

    public int getBaseSlot() {
        return baseSlot;
    }

    public ItemStack getCompactTier1Item() {
        return compactTier1Item;
    }

    public int getCompactTier1Ratio() {
        return compactTier1Ratio;
    }

    public ItemStack getCompactTier2Item() {
        return compactTier2Item;
    }

    public int getCompactTier2Ratio() {
        return compactTier2Ratio;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority value) {
        this.priority = value;
        setChanged();
    }

    public boolean hasPullerUpgrade() {
        return hasPullerUpgrade;
    }

    public void setPullerUpgrade(boolean value) {
        hasPullerUpgrade = value;
        setChanged();
    }

    public int getPullerSides() {
        return pullerSides;
    }

    public void setPullerSides(int mask) {
        pullerSides = mask;
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            BarrelBlock.setTierBlockState(level, worldPosition, tier);
        }
    }

    // -------------------------------------------------------------------------
    // Insert / extract
    // -------------------------------------------------------------------------

    /**
     * Inserts up to {@code amount} items from {@code stack} into the barrel.
     *
     * @return count not inserted (remainder)
     */
    public long insert(ItemStack stack, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0) return amount;
        if (isLocked() && !ItemStack.isSameItemSameComponents(storedItem, stack)) return amount;

        long space = getCapacity() - count;
        long toInsert = Math.min(amount, voidExcess ? amount : space);
        if (toInsert <= 0) return amount;

        if (!simulate) {
            if (!isLocked()) storedItem = stack.copyWithCount(1);
            if (voidExcess) {
                count = Math.min(count + amount, getCapacity());
            } else {
                count += toInsert;
            }
            notifyChanged();
        }
        return voidExcess ? 0 : (amount - toInsert);
    }

    /** Extracts up to {@code amount} items. */
    public ItemStack extract(long amount, boolean simulate) {
        if (!isLocked() || count == 0 || amount <= 0) return ItemStack.EMPTY;
        long toExtract = Math.min(amount, count);
        if (toExtract <= 0) return ItemStack.EMPTY;
        ItemStack result = storedItem.copyWithCount((int) Math.min(toExtract, Integer.MAX_VALUE));
        if (!simulate) {
            count -= toExtract;
            notifyChanged();
        }
        return result;
    }

    /** Clears lock. Only safe when count == 0. */
    public void clearItem() {
        storedItem = ItemStack.EMPTY;
        count = 0;
        baseSlot = 0;
        compactCacheKey = null;
        notifyChanged();
    }

    // -------------------------------------------------------------------------
    // Drop items when block is broken without preserving container
    // -------------------------------------------------------------------------

    public void drops() {
        // Items are preserved in the dropped block item via collectImplicitComponents.
        // This method intentionally left empty (contents stay in item).
    }

    // -------------------------------------------------------------------------
    // setChanged / notify
    // -------------------------------------------------------------------------

    /** Directly sets item type and count without going through insert/extract logic. */
    protected void loadContents(ItemStack newStoredItem, long newCount) {
        storedItem = newStoredItem.isEmpty() ? ItemStack.EMPTY : newStoredItem.copyWithCount(1);
        count = newCount;
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    protected void notifyChanged() {
        super.setChanged();
        if (level instanceof ServerLevel sl) {
            niLink.notifyChanged(sl, worldPosition, getBlockState());
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setChanged() {
        niLink.invalidate();
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // MenuProvider / ExtendedMenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.barrel");
    }

    @Override
    public String getNetworkName() {
        return Component.translatable("block.tremendousstorage.barrel").getString() + buildSuffix(false);
    }

    protected String buildSuffix(boolean ender) {
        StringBuilder sb = new StringBuilder();
        if (tier != StorageTier.WOOD) {
            sb.append(Character.toUpperCase(tier.getId().charAt(0)))
                    .append(tier.getId().substring(1));
        }
        if (ender) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Ender");
        }
        if (compactingUpgrade) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append("Compacting");
        }
        return sb.isEmpty() ? "" : " (" + sb + ")";
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new SimpleContainerData(2) {
            @Override
            public int get(int index) {
                if (index == 0) return voidExcess ? 1 : 0;
                if (index == 1) return priority.ordinal();
                return 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        };
        return new BarrelMenu(id, inv, worldPosition, data, hasPullerUpgrade);
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    public static void serverTick(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,
            BarrelBlockEntity be) {
        if (be.hasPullerUpgrade && be.pullerSides != 0 && ++be.pullerTickCounter >= PULL_TICKS) {
            be.pullerTickCounter = 0;
            PullerUtil.tickPuller(level, pos, state, be.pullerSides, be::pullFromHandler);
        }
    }

    private void pullFromHandler(ResourceHandler<ItemResource> handler) {
        CompactingBarrelItemHandler compact = compactingUpgrade ? new CompactingBarrelItemHandler(this) : null;
        for (int s = 0; s < handler.size(); s++) {
            ItemResource res = handler.getResource(s);
            if (res.isEmpty()) continue;
            int available = (int) Math.min(handler.getAmountAsLong(s), PULL_AMOUNT);
            if (available <= 0) continue;
            ItemStack probe = res.toStack(available);
            int canInsert = simulatePullInsert(compact, probe);
            if (canInsert <= 0) continue;
            int extracted = handler.extract(s, res, canInsert, null);
            if (extracted > 0) doPullInsert(compact, res.toStack(extracted));
            break;
        }
    }

    private int simulatePullInsert(CompactingBarrelItemHandler compact, ItemStack stack) {
        if (compact != null) {
            net.neoforged.neoforge.transfer.item.ItemResource resource =
                    net.neoforged.neoforge.transfer.item.ItemResource.of(stack);
            for (int ts = 0; ts < 3; ts++) {
                int n = compact.simulateInsert(ts, resource, stack.getCount());
                if (n > 0) return n;
            }
            return 0;
        }
        return (int) (stack.getCount() - insert(stack, stack.getCount(), true));
    }

    private void doPullInsert(CompactingBarrelItemHandler compact, ItemStack stack) {
        if (compact != null) {
            net.neoforged.neoforge.transfer.item.ItemResource resource =
                    net.neoforged.neoforge.transfer.item.ItemResource.of(stack);
            for (int ts = 0; ts < 3 && !stack.isEmpty(); ts++) {
                int inserted = compact.insert(ts, resource, stack.getCount(), null);
                if (inserted > 0) stack.shrink(inserted);
            }
        } else {
            insert(stack, stack.getCount(), false);
        }
    }

    // -------------------------------------------------------------------------
    // Compacting chain lock
    // -------------------------------------------------------------------------

    /**
     * Configures the compacting chain by specifying which item belongs at which slot. Walks DOWN
     * from {@code seedItem} up to {@code targetSlot} times to find the base item (stored item),
     * then the recipe cache walks UP from the base to fill higher slots. Resets count to zero.
     */
    public void lockCompactingChain(ItemStack seedItem, int targetSlot) {
        if (level == null || level.isClientSide() || !compactingUpgrade) return;
        if (!(level instanceof ServerLevel sl)) return;
        RecipeManager rm = sl.getServer().getRecipeManager();

        ItemStack base = seedItem.copyWithCount(1);
        int newBaseSlot = targetSlot;
        for (int i = 0; i < targetSlot; i++) {
            CompactResult lower = findLowerTier(rm, base);
            if (lower == null) break;
            base = lower.result();
            newBaseSlot--;
        }

        storedItem = base;
        count = 0;
        baseSlot = newBaseSlot;
        compactCacheKey = null;
        compactCacheBaseSlot = -1;
        notifyChanged();
    }

    /**
     * Like {@link #lockCompactingChain} but called when the compacting upgrade is applied to an
     * already-locked barrel. Treats the current stored item as {@code targetSlot}, walks down to
     * find the base item, and converts the existing raw count at each step so no items are lost.
     * Example: 100 Iron Ingots at slot 1 → storedItem = Iron Nugget, count = 900, baseSlot = 0.
     */
    public void lockCompactingChainPreserving(int targetSlot) {
        if (level == null || level.isClientSide() || !compactingUpgrade || !isLocked()) return;
        if (!(level instanceof ServerLevel sl)) return;
        RecipeManager rm = sl.getServer().getRecipeManager();

        ItemStack base = storedItem.copyWithCount(1);
        long newCount = count;
        int newBaseSlot = targetSlot;
        for (int i = 0; i < targetSlot; i++) {
            CompactResult lower = findLowerTier(rm, base);
            if (lower == null) break;
            newCount = newCount * lower.ratio();
            base = lower.result();
            newBaseSlot--;
        }

        storedItem = base;
        count = newCount;
        baseSlot = newBaseSlot;
        compactCacheKey = null;
        compactCacheBaseSlot = -1;
        notifyChanged();
    }

    // -------------------------------------------------------------------------
    // Compacting recipe cache
    // -------------------------------------------------------------------------

    void ensureCompactingCache() {
        if (level == null || level.isClientSide() || !compactingUpgrade || !isLocked()) {
            compactCacheKey = null;
            compactCacheBaseSlot = -1;
            return;
        }
        if (compactCacheKey != null
                && ItemStack.isSameItemSameComponents(compactCacheKey, storedItem)
                && compactCacheBaseSlot == baseSlot) return;
        compactCacheKey = storedItem.copyWithCount(1);
        compactCacheBaseSlot = baseSlot;
        if (!(level instanceof ServerLevel sl)) return;
        buildCompactingChain(sl.getServer().getRecipeManager());
    }

    private void buildCompactingChain(RecipeManager rm) {
        compactTier1Item = ItemStack.EMPTY;
        compactTier1Ratio = 0;
        compactTier2Item = ItemStack.EMPTY;
        compactTier2Ratio = 0;
        int maxTiers = 2 - baseSlot; // slots available above storedItem
        if (maxTiers < 1) return;
        CompactResult t1 = findUpperTier(rm, storedItem);
        if (t1 == null) return;
        compactTier1Item = t1.result();
        compactTier1Ratio = t1.ratio();
        if (maxTiers < 2) return;
        CompactResult t2 = findUpperTier(rm, compactTier1Item);
        compactTier2Item = t2 != null ? t2.result() : ItemStack.EMPTY;
        compactTier2Ratio = t2 != null ? t2.ratio() : 0;
    }

    private record CompactResult(ItemStack result, int ratio) {}

    /**
     * Finds the compressed form of {@code item} using 3×3 (ratio 9), 2×2 (ratio 4), or ring
     * (ratio 8) patterns. No round-trip check: compressing only produces items the player could
     * craft themselves from the base item; the risk of unobtainable items only flows downward.
     */
    private CompactResult findUpperTier(RecipeManager rm, ItemStack item) {
        if (item.isEmpty() || level == null) return null;
        var in9 = CraftingInput.of(3, 3, List.of(item, item, item, item, item, item, item, item, item));
        var r9 = rm.getRecipeFor(RecipeType.CRAFTING, in9, level);
        if (r9.isPresent()) {
            var out = r9.get().value().assemble(in9);
            if (!out.isEmpty()) return new CompactResult(out.copyWithCount(1), 9);
        }
        var in4 = CraftingInput.of(2, 2, List.of(item, item, item, item));
        var r4 = rm.getRecipeFor(RecipeType.CRAFTING, in4, level);
        if (r4.isPresent()) {
            var out = r4.get().value().assemble(in4);
            if (!out.isEmpty()) return new CompactResult(out.copyWithCount(1), 4);
        }
        var inRing = CraftingInput.of(3, 3, List.of(item, item, item, item, ItemStack.EMPTY, item, item, item, item));
        var rRing = rm.getRecipeFor(RecipeType.CRAFTING, inRing, level);
        if (rRing.isPresent()) {
            var out = rRing.get().value().assemble(inRing);
            if (!out.isEmpty()) return new CompactResult(out.copyWithCount(1), 8);
        }
        return null;
    }

    /**
     * Finds the decompressed form of {@code item} via a 1×1 crafting recipe. Among all matching
     * uncraft recipes, candidates are preferred in ratio order 9 → 4 → 8 (same order as
     * {@link #findUpperTier}). Each candidate is round-trip verified: its output must re-compress
     * back to {@code item} with the same ratio, preventing unobtainable lower-tier items.
     */
    private CompactResult findLowerTier(RecipeManager rm, ItemStack item) {
        if (item.isEmpty() || level == null) return null;
        var in1 = CraftingInput.of(1, 1, List.of(item));
        ItemStack out9 = ItemStack.EMPTY, out4 = ItemStack.EMPTY, out8 = ItemStack.EMPTY;
        for (var holder : rm.recipeMap().byType(RecipeType.CRAFTING)) {
            if (!holder.value().matches(in1, level)) continue;
            var out = holder.value().assemble(in1);
            if (out.isEmpty()) continue;
            if (out.getCount() == 9 && out9.isEmpty()) out9 = out;
            else if (out.getCount() == 4 && out4.isEmpty()) out4 = out;
            else if (out.getCount() == 8 && out8.isEmpty()) out8 = out;
        }
        for (var candidate : new ItemStack[] {out9, out4, out8}) {
            if (candidate.isEmpty()) continue;
            int n = candidate.getCount();
            CompactResult upper = findUpperTier(rm, candidate);
            if (upper == null) continue;
            if (!ItemStack.isSameItemSameComponents(upper.result(), item) || upper.ratio() != n) continue;
            return new CompactResult(candidate.copyWithCount(1), n);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    private static final String TAG_ITEM = "StoredItem";
    private static final String TAG_COUNT = "Count";
    private static final String TAG_TIER = "Tier";
    private static final String TAG_VOID_EXCESS = "VoidExcess";
    private static final String TAG_COMPACTING = "CompactingUpgrade";
    private static final String TAG_BASE_SLOT = "BaseSlot";
    private static final String TAG_PRIORITY = "Priority";

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!storedItem.isEmpty()) {
            output.store(TAG_ITEM, ItemStack.OPTIONAL_CODEC, storedItem);
        }
        output.putLong(TAG_COUNT, count);
        output.putString(TAG_TIER, tier.getId());
        output.putBoolean(TAG_VOID_EXCESS, voidExcess);
        output.putBoolean(TAG_COMPACTING, compactingUpgrade);
        if (compactingUpgrade && baseSlot != 0) output.putInt(TAG_BASE_SLOT, baseSlot);
        if (hasPullerUpgrade) {
            output.putBoolean("PullerUpgrade", true);
            output.putInt("PullerSides", pullerSides);
        }
        output.putInt(TAG_PRIORITY, priority.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedItem = input.read(TAG_ITEM, ItemStack.OPTIONAL_CODEC)
                .filter(s -> !s.isEmpty())
                .map(s -> s.copyWithCount(1))
                .orElse(ItemStack.EMPTY);
        count = input.getLongOr(TAG_COUNT, 0L);
        tier = StorageTier.fromId(input.getStringOr(TAG_TIER, ""));
        voidExcess = input.getBooleanOr(TAG_VOID_EXCESS, false);
        compactingUpgrade = input.getBooleanOr(TAG_COMPACTING, false);
        baseSlot = Math.max(0, Math.min(2, input.getIntOr(TAG_BASE_SLOT, 0)));
        compactCacheKey = null;
        compactCacheBaseSlot = -1;
        hasPullerUpgrade = input.getBooleanOr("PullerUpgrade", false);
        pullerSides = input.getIntOr("PullerSides", 0);
        if (compactingUpgrade) {
            compactTier1Item = input.read("Compact1", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            compactTier1Ratio = input.getIntOr("Compact1Ratio", 0);
            compactTier2Item = input.read("Compact2", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            compactTier2Ratio = input.getIntOr("Compact2Ratio", 0);
        }
        priority = Priority.fromOrdinal(input.getIntOr(TAG_PRIORITY, 0));
    }

    // -------------------------------------------------------------------------
    // Item component sync
    // -------------------------------------------------------------------------

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(
                BETypeHelper.<BarrelContents>getDataComponentType("barrel_contents"),
                new BarrelContents(storedItem.isEmpty() ? Optional.empty() : Optional.of(storedItem), count));
        // Explicitly populate BLOCK_ENTITY_DATA with tier and upgrade info so the loot table's
        // copy_components function can copy it to the dropped item. The base BlockEntity
        // implementation requires live registries to serialize ItemStacks and may produce an
        // empty tag; we avoid that by only writing primitive fields here (the item/count are
        // preserved separately via BarrelContents above).
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TIER, tier.getId());
        tag.putBoolean(TAG_VOID_EXCESS, voidExcess);
        tag.putBoolean(TAG_COMPACTING, compactingUpgrade);
        if (compactingUpgrade && baseSlot != 0) tag.putInt(TAG_BASE_SLOT, baseSlot);
        if (hasPullerUpgrade) {
            tag.putBoolean("PullerUpgrade", true);
            tag.putInt("PullerSides", pullerSides);
        }
        tag.putInt(TAG_PRIORITY, priority.ordinal());
        if (compactingUpgrade) {
            ensureCompactingCache();
            if (!compactTier1Item.isEmpty()) {
                Identifier k1 = BuiltInRegistries.ITEM.getKey(compactTier1Item.getItem());
                if (k1 != null) tag.putString("Compact1Id", k1.toString());
            }
            if (!compactTier2Item.isEmpty()) {
                Identifier k2 = BuiltInRegistries.ITEM.getKey(compactTier2Item.getItem());
                if (k2 != null) tag.putString("Compact2Id", k2.toString());
            }
        }
        components.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(getType(), tag));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        BarrelContents contents = input.get(BETypeHelper.<BarrelContents>getDataComponentType("barrel_contents"));
        if (contents != null) {
            storedItem = contents.storedItem().map(s -> s.copyWithCount(1)).orElse(ItemStack.EMPTY);
            count = contents.count();
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.world.level.storage.ValueOutput output) {
        output.discard(TAG_ITEM);
        output.discard(TAG_COUNT);
    }

    // -------------------------------------------------------------------------
    // Client sync
    // -------------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        if (compactingUpgrade && level != null && !level.isClientSide()) {
            ensureCompactingCache();
            net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> ops =
                    registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            if (!compactTier1Item.isEmpty()) {
                ItemStack.OPTIONAL_CODEC
                        .encodeStart(ops, compactTier1Item)
                        .result()
                        .ifPresent(t -> tag.put("Compact1", t));
                tag.putInt("Compact1Ratio", compactTier1Ratio);
            }
            if (!compactTier2Item.isEmpty()) {
                ItemStack.OPTIONAL_CODEC
                        .encodeStart(ops, compactTier2Item)
                        .result()
                        .ifPresent(t -> tag.put("Compact2", t));
                tag.putInt("Compact2Ratio", compactTier2Ratio);
            }
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
