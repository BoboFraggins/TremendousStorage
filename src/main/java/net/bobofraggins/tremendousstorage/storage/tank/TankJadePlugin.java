package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade (WAILA) plugin for the Tank.
 *
 * <p>Shows the stored fluid name and amount, e.g. "512k mB of 1M mB Water", or "Empty".
 */
@WailaPlugin
public class TankJadePlugin implements IWailaPlugin {

    static final Identifier TANK_PROVIDER = Identifier.fromNamespaceAndPath("tremendousstorage", "tank");

    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_CAPACITY = "Capacity";
    private static final String KEY_FLUID = "Fluid";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(TankServerProvider.INSTANCE, TankBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TankClientProvider.INSTANCE, TankBlock.class);
    }

    // -------------------------------------------------------------------------
    // Server-side data provider
    // -------------------------------------------------------------------------

    enum TankServerProvider implements snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TankBlockEntity be)) return;
            data.putLong(KEY_AMOUNT, be.getAmount());
            data.putLong(KEY_CAPACITY, be.getCapacity());
            FluidStack fluid = be.getStoredFluid();
            if (!fluid.isEmpty()) {
                net.minecraft.nbt.Tag fluidTag = net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_CODEC
                        .encodeStart(
                                accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), fluid)
                        .result()
                        .orElse(new CompoundTag());
                data.put(KEY_FLUID, fluidTag);
            }
        }

        @Override
        public Identifier getUid() {
            return TANK_PROVIDER;
        }
    }

    // -------------------------------------------------------------------------
    // Client-side component provider
    // -------------------------------------------------------------------------

    enum TankClientProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();

            if (!data.contains(KEY_FLUID)) {
                tooltip.add(Component.translatable("jade.tremendousstorage.tank.empty"));
                return;
            }

            FluidStack fluid = net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_CODEC
                    .parse(
                            accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                            data.getCompound(KEY_FLUID).orElse(new CompoundTag()))
                    .result()
                    .orElse(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
            if (fluid.isEmpty()) return;

            long amount = data.getLongOr(KEY_AMOUNT, 0L);
            long capacity = data.getLongOr(KEY_CAPACITY, 0L);
            tooltip.add(Component.translatable(
                    "jade.tremendousstorage.tank.contents",
                    CountFormat.format(amount),
                    CountFormat.format(capacity),
                    fluid.getHoverName()));
        }

        @Override
        public Identifier getUid() {
            return TANK_PROVIDER;
        }
    }
}
