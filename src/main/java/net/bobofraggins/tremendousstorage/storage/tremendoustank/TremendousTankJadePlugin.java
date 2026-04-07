package net.bobofraggins.tremendousstorage.storage.tremendoustank;

import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * Jade (WAILA) plugin for the Tremendous Tank.
 *
 * <p>Shows the stored fluid name and amount, e.g. "512k mB of 1M mB Water", or "Empty".
 */
@WailaPlugin
public class TremendousTankJadePlugin implements IWailaPlugin {

    static final ResourceLocation TREMENDOUS_TANK_PROVIDER =
            ResourceLocation.fromNamespaceAndPath("tremendousstorage", "tremendous_tank");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(TankDataProvider.INSTANCE, TremendousTankBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TankDataProvider.INSTANCE, TremendousTankBlock.class);
    }

    enum TankDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_AMOUNT = "Amount";
        private static final String KEY_CAPACITY = "Capacity";
        private static final String KEY_FLUID = "Fluid";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TremendousTankBlockEntity be)) return;
            data.putLong(KEY_AMOUNT, be.getAmount());
            data.putLong(KEY_CAPACITY, be.getCapacity());
            FluidStack fluid = be.getStoredFluid();
            if (!fluid.isEmpty()) {
                data.put(KEY_FLUID, fluid.save(accessor.getLevel().registryAccess()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return TREMENDOUS_TANK_PROVIDER;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();

            if (!data.contains(KEY_FLUID)) {
                tooltip.add(Component.translatable("jade.tremendousstorage.tremendous_tank.empty"));
                return;
            }

            FluidStack fluid =
                    FluidStack.parseOptional(accessor.getLevel().registryAccess(), data.getCompound(KEY_FLUID));
            if (fluid.isEmpty()) return;

            long amount = data.getLong(KEY_AMOUNT);
            long capacity = data.getLong(KEY_CAPACITY);
            tooltip.add(Component.translatable(
                    "jade.tremendousstorage.tremendous_tank.contents",
                    CountFormat.format(amount),
                    CountFormat.format(capacity),
                    fluid.getHoverName()));
        }
    }
}
