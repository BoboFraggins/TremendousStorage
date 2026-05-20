package net.bobofraggins.tremendousstorage.storage.backpack;

import java.util.ArrayList;
import java.util.List;
import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the placed Tremendous Backpack.
 *
 * <p>Extends {@link ChestBlockEntity} so that all storage logic, lid animation,
 * opener counting, sound, and NBT serialization are inherited unchanged. The only differences
 * are the block entity type and display name.
 *
 * <p>Uses {@link ChestMenu} for its UI (the chest screen is identical to what we need).
 */
public class BackpackBlockEntity extends ChestBlockEntity {

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(BETypeHelper.get("backpack"), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.backpack");
    }

    @Override
    public String getNetworkName() {
        return Component.translatable("block.tremendousstorage.backpack").getString() + buildSuffix(false);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? getPriority().ordinal() : 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
        return new ChestMenu(id, inv, worldPosition, data, hasCraftingUpgrade());
    }

    // -------------------------------------------------------------------------
    // Item ↔ block entity component sync
    // -------------------------------------------------------------------------

    /**
     * Writes the current inventory into {@link BackpackContents} so items survive a
     * place-and-break cycle without needing the block entity data to be present.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        List<BackpackContents.Entry> entries = new ArrayList<>();
        for (int i = 0; i < typeCount(); i++) {
            ItemStack type = getType(i);
            long count = getCount(i);
            if (!type.isEmpty() && count > 0) {
                entries.add(new BackpackContents.Entry(type, count));
            }
        }
        components.set(
                BackpackContents.type(),
                new BackpackContents(entries, getTier(), getPriority(), getSortMode(), hasCraftingUpgrade()));
    }

    // -------------------------------------------------------------------------
    // Tickers (delegate to inherited implementation via static method wrappers)
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, BackpackBlockEntity be) {
        ChestBlockEntity.serverTick(level, pos, state, be);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BackpackBlockEntity be) {
        ChestBlockEntity.clientTick(level, pos, state, be);
    }
}
