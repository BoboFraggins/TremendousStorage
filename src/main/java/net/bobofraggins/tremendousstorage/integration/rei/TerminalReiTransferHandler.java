package net.bobofraggins.tremendousstorage.integration.rei;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.bobofraggins.tremendousstorage.shared.network.SatFillCraftingGridPacket;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * REI recipe transfer handler for the Storage Access Terminal.
 *
 * <p>When the player clicks REI's transfer arrow while viewing a vanilla crafting recipe, this
 * handler sends {@link SatFillCraftingGridPacket} to the server, which clears the current grid
 * and refills it with ingredients drawn from the connected network.
 */
public class TerminalReiTransferHandler implements TransferHandler {

    private final Class<? extends AccessTerminalMenu> menuClass;

    public TerminalReiTransferHandler(Class<? extends AccessTerminalMenu> menuClass) {
        this.menuClass = menuClass;
    }

    @Override
    public Result handle(Context context) {
        // Only handle Access Terminal menus.
        if (!menuClass.isInstance(context.getMenu())) return Result.createNotApplicable();
        AccessTerminalMenu menu = menuClass.cast(context.getMenu());

        if (!menu.hasNetwork()) return Result.createNotApplicable();

        // Only handle vanilla crafting displays.
        if (!(context.getDisplay() instanceof DefaultCraftingDisplay<?> display)) {
            return Result.createNotApplicable();
        }

        // Get the recipe's ResourceLocation directly from the display.
        ResourceLocation recipeId = display.getDisplayLocation().orElse(null);

        if (recipeId == null) return Result.createNotApplicable();

        if (!context.isActuallyCrafting()) {
            // REI is testing feasibility for UI indication — we're always ready if connected.
            return Result.createSuccessful();
        }

        PacketDistributor.sendToServer(new SatFillCraftingGridPacket(menu.getSatPos(), recipeId));
        return Result.createSuccessful();
    }
}
