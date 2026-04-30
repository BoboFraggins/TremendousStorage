package net.bobofraggins.tremendousstorage.glamping.dankfannypack;

import net.bobofraggins.tremendousstorage.shared.network.SetFannyPackPriorityPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetFannyPackVoidExcessPacket;
import net.bobofraggins.tremendousstorage.shared.ui.AbstractFilingCabinetScreen;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityPane;
import net.bobofraggins.tremendousstorage.shared.ui.VoidExcessPane;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Dank Fanny Pack — identical layout to the Filing Cabinet.
 *
 * <p>Config drawer exposes void-excess toggle and priority control; changes are sent as
 * {@code SetFannyPack*} packets so the server can persist them to the item component.
 */
public class DankFannyPackScreen extends AbstractFilingCabinetScreen<DankFannyPackMenu> {

    private static final int BG_HEIGHT = 228;
    private static final int PLAYER_INV_Y = 142;

    public DankFannyPackScreen(DankFannyPackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, BG_HEIGHT, PLAYER_INV_Y, buildConfigDrawer(menu));
    }

    private static ConfigDrawer buildConfigDrawer(DankFannyPackMenu menu) {
        return new ConfigDrawer(
                new VoidExcessPane(menu::isVoidExcess, () -> {
                    boolean next = !menu.isVoidExcess();
                    menu.setVoidExcess(next);
                    PacketDistributor.sendToServer(new SetFannyPackVoidExcessPacket(
                            menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), next));
                }),
                new PriorityPane(
                        menu::getPriority,
                        p -> PacketDistributor.sendToServer(new SetFannyPackPriorityPacket(
                                menu.getSlotType(), menu.getSlotIndex(), menu.getSlotId(), p))));
    }
}
