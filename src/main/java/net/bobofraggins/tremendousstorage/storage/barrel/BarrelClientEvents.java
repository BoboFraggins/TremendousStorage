package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Registers the block entity renderer and GUI screen for the Barrel. */
public final class BarrelClientEvents {

    private BarrelClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.BARREL_BE_TYPE.get(), BarrelRenderer::new);
        event.registerBlockEntityRenderer(Registration.ENDER_BARREL_BE_TYPE.get(), BarrelRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (StorageTier tier : StorageTier.values()) {
            String id = tier.getId();
            event.register(ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath("tremendousstorage", "block/barrels/barrel_body_" + id)));
            event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                    "tremendousstorage", "block/ender_barrels/ender_barrel_body_" + id)));
            event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                    "tremendousstorage", "block/barrels_compacting/barrel_body_" + id)));
        }
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.BARREL_MENU.get(), BarrelScreen::new);
    }

    /**
     * Intercepts left-clicks on the barrel's front face so the held item is inserted into the
     * barrel rather than beginning block-break. Only fires on the first press (Action.START).
     * Runs on the logical client only; hit precision is not available for left-click events so
     * the entire front face acts as the insert target.
     */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        BlockPos pos = event.getPos();
        BlockState state = event.getEntity().level().getBlockState(pos);
        if (!(state.getBlock() instanceof BarrelBlock)) return;
        Direction facing = state.getValue(BarrelBlock.FACING);
        if (event.getFace() != facing) return;
        event.setCanceled(true);
        PacketDistributor.sendToServer(new BarrelInsertPacket(pos));
    }
}
