package net.bobofraggins.tremendousstorage.storage.filingcabinet;

import net.bobofraggins.tremendousstorage.shared.network.SetPriorityPacket;
import net.bobofraggins.tremendousstorage.shared.network.SetVoidExcessPacket;
import net.bobofraggins.tremendousstorage.shared.ui.AbstractFilingCabinetScreen;
import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.PriorityPane;
import net.bobofraggins.tremendousstorage.shared.ui.VoidExcessPane;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Filing Cabinet block.
 *
 * <p>Layout (176 × 300 px):
 * <ul>
 *   <li>Title centred in title bar; "≡" config button in top-left of title bar
 *   <li>Config drawer (slides left): void-excess toggle + priority control
 *   <li>8 folder slots (left col) + 8 extraction slots (right col), y=36..179
 *   <li>Player inventory 3×9 starting at y=214
 *   <li>Player hotbar at y=272
 * </ul>
 */
public class FilingCabinetScreen extends AbstractFilingCabinetScreen<FilingCabinetMenu> {

    private static final int BG_HEIGHT = 300;
    private static final int PLAYER_INV_Y = 214;

    public FilingCabinetScreen(FilingCabinetMenu menu, Inventory inv, Component title) {
        super(
                menu,
                inv,
                title,
                BG_HEIGHT,
                PLAYER_INV_Y,
                new ConfigDrawer(
                        new VoidExcessPane(menu::isVoidExcess, () -> {
                            boolean next = !menu.isVoidExcess();
                            menu.setVoidExcess(next);
                            PacketDistributor.sendToServer(new SetVoidExcessPacket(menu.getPos(), next));
                        }),
                        new PriorityPane(
                                menu::getPriority,
                                p -> PacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), p)))));
    }
}
