package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity for the placed Picnic Basket.
 *
 * <p>Extends {@link ChestBlockEntity} so that all storage logic, lid animation,
 * opener counting, sound, and NBT serialization are inherited unchanged. The only
 * differences are the block entity type and display name.
 */
public class PicnicBasketBlockEntity extends ChestBlockEntity {

    private boolean autoFeed = true;

    public boolean isAutoFeed() {
        return autoFeed;
    }

    public void setAutoFeed(boolean value) {
        autoFeed = value;
        setChanged();
    }

    public PicnicBasketBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.PICNIC_BASKET_BE_TYPE.get(), pos, state);
        setPriority(Priority.HIGH);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.picnic_basket");
    }

    @Override
    public String getNetworkName() {
        return Component.translatable("block.tremendousstorage.picnic_basket").getString() + buildSuffix(false);
    }

    @Override
    public boolean acceptsItem(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }

    @Override
    public boolean isUpgradeable() {
        return false;
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
        return new ChestMenu(id, inv, worldPosition, data, false, hasPullerUpgrade());
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("AutoFeed", autoFeed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        autoFeed = input.getBooleanOr("AutoFeed", true);
    }

    // -------------------------------------------------------------------------
    // Tickers (delegate to inherited implementation via static method wrappers)
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, PicnicBasketBlockEntity be) {
        ChestBlockEntity.serverTick(level, pos, state, be);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PicnicBasketBlockEntity be) {
        ChestBlockEntity.clientTick(level, pos, state, be);
    }
}
