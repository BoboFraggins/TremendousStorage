package net.bobofraggins.intellistore.external.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.bobofraggins.intellistore.shared.network.SetImportExportFilterPacket;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalMenu;
import net.bobofraggins.intellistore.storage.tubeattachments.ExportInterfaceScreen;
import net.bobofraggins.intellistore.storage.tubeattachments.ImportInterfaceScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI plugin for IntelliStore.
 *
 * <p>Registers the Storage Access Terminal as a crafting station, and adds ghost ingredient
 * drag support for Import Interface and Export Interface filter screens.
 */
@JeiPlugin
public class IntelliStoreJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath("intellistore", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(
                Registration.STORAGE_ACCESS_TERMINAL_ITEM.get().getDefaultInstance(), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(
                AccessTerminalMenu.class,
                Registration.STORAGE_ACCESS_TERMINAL_MENU.get(),
                RecipeTypes.CRAFTING,
                1,
                9,
                10,
                36);
    }

    /**
     * Registers ghost ingredient handlers so players can drag items from JEI directly
     * into the 9 filter slots of the Import Interface and Export Interface screens.
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addGhostIngredientHandler(ImportInterfaceScreen.class, new InterfaceGhostHandler<>());
        reg.addGhostIngredientHandler(ExportInterfaceScreen.class, new InterfaceGhostHandler<>());
    }

    // -------------------------------------------------------------------------
    // Ghost ingredient handler — shared by both screens via duck typing
    // -------------------------------------------------------------------------

    /**
     * Handles JEI ingredient drags into Import/Export Interface filter slot grids.
     * Works for both {@link ImportInterfaceScreen} and {@link ExportInterfaceScreen}.
     */
    private static final class InterfaceGhostHandler<
                    S extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>>
            implements IGhostIngredientHandler<S> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(S screen, ITypedIngredient<I> ingredient, boolean doStart) {
            // Only handle ItemStack ingredients
            Optional<ItemStack> itemOpt = ingredient.getItemStack();
            if (itemOpt.isEmpty()) return List.of();

            List<Target<I>> targets = new ArrayList<>(9);

            for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
                final int si = slotIndex;
                int sx, sy;

                if (screen instanceof ImportInterfaceScreen is) {
                    sx = is.getGhostSlotX(si);
                    sy = is.getGhostSlotY(si);
                } else if (screen instanceof ExportInterfaceScreen es) {
                    sx = es.getGhostSlotX(si);
                    sy = es.getGhostSlotY(si);
                } else {
                    continue;
                }

                int size = ImportInterfaceScreen.getGhostSlotInnerSize();
                Rect2i area = new Rect2i(sx, sy, size, size);

                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return area;
                    }

                    @Override
                    public void accept(I rawIngredient) {
                        if (!(rawIngredient instanceof ItemStack itemStack)) return;
                        ItemStack ghost = itemStack.copyWithCount(1);

                        // Update menu client-side and notify server
                        if (screen instanceof ImportInterfaceScreen is) {
                            is.getMenu().setFilterSlot(si, ghost);
                            PacketDistributor.sendToServer(new SetImportExportFilterPacket(
                                    is.getMenu().getPos(), is.getMenu().getFaceIndex(), si, ghost));
                        } else if (screen instanceof ExportInterfaceScreen es) {
                            es.getMenu().setFilterSlot(si, ghost);
                            PacketDistributor.sendToServer(new SetImportExportFilterPacket(
                                    es.getMenu().getPos(), es.getMenu().getFaceIndex(), si, ghost));
                        }
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
            // Nothing to clean up when the drag operation completes
        }
    }
}
