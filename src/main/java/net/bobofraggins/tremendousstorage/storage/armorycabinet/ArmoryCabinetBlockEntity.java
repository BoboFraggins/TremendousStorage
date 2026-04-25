package net.bobofraggins.tremendousstorage.storage.armorycabinet;

import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Armory Cabinet.
 *
 * <p>Accepts only items that have durability (max damage &gt; 0) or are tagged as
 * {@code c:tools}, {@code c:weapons}, or {@code c:armor}. No upgrades.
 */
public class ArmoryCabinetBlockEntity extends ChestBlockEntity {

    private static final TagKey<Item> TAG_TOOLS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools"));
    private static final TagKey<Item> TAG_WEAPONS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "weapons"));
    private static final TagKey<Item> TAG_ARMOR =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "armor"));

    // -------------------------------------------------------------------------
    // Animation state (client-side) — two-phase open/close
    // -------------------------------------------------------------------------

    /** Animation progress last tick (0 = closed, 1 = fully open). */
    public float prevOpenFraction = 0f;

    /** Animation progress this tick. */
    public float openFraction = 0f;

    // -------------------------------------------------------------------------
    // Openers counter — overrides the one in ChestBlockEntity
    // -------------------------------------------------------------------------

    private final ContainerOpenersCounter acOpenersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.IRON_DOOR_OPEN,
                    SoundSource.BLOCKS,
                    0.5f,
                    level.random.nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.IRON_DOOR_CLOSE,
                    SoundSource.BLOCKS,
                    0.5f,
                    level.random.nextFloat() * 0.1f + 0.9f);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ArmoryCabinetMenu m
                    && m.getPos().equals(worldPosition);
        }
    };

    @Override
    public void startOpen(Player player) {
        if (!isRemoved() && !player.isSpectator()) {
            acOpenersCounter.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!isRemoved() && !player.isSpectator()) {
            acOpenersCounter.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    public void recheckOpeners(Level level, BlockPos pos, BlockState state) {
        acOpenersCounter.recheckOpeners(level, pos, state);
    }

    // -------------------------------------------------------------------------
    // Tickers
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArmoryCabinetBlockEntity be) {
        be.acOpenersCounter.recheckOpeners(level, pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ArmoryCabinetBlockEntity be) {
        be.prevOpenFraction = be.openFraction;
        if (be.openCount > 0 && be.openFraction < 1f) {
            be.openFraction = Math.min(1f, be.openFraction + 0.1f);
        } else if (be.openCount == 0 && be.openFraction > 0f) {
            be.openFraction = Math.max(0f, be.openFraction - 0.1f);
        }
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ArmoryCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.ARMORY_CABINET_BE_TYPE.get(), pos, state);
        setPriority(Priority.HIGH);
    }

    // -------------------------------------------------------------------------
    // Item filter
    // -------------------------------------------------------------------------

    @Override
    public boolean acceptsItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getMaxDamage() > 0) return true;
        return stack.is(TAG_TOOLS) || stack.is(TAG_WEAPONS) || stack.is(TAG_ARMOR);
    }

    @Override
    public long getCapacity() {
        return 1_024L;
    }

    @Override
    public boolean isUpgradeable() {
        return false;
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.armory_cabinet");
    }

    @Override
    public String getNetworkName() {
        return Component.translatable("block.tremendousstorage.armory_cabinet").getString();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ContainerData data = new SimpleContainerData(1);
        return new ArmoryCabinetMenu(id, inv, worldPosition, data);
    }
}
