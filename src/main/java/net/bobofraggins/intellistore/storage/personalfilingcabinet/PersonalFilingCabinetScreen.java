package net.bobofraggins.intellistore.storage.personalfilingcabinet;

import net.bobofraggins.intellistore.shared.network.SetPfcVoidExcessPacket;
import net.bobofraggins.intellistore.shared.ui.AbstractFilingCabinetScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Personal Filing Cabinet item.
 *
 * <p>Layout (176 × 170 px):
 * <ul>
 *   <li>Title centred at y=6
 *   <li>"Void Excess: ON/OFF" toggle button (120×14) centred at y=18
 *   <li>2 rows × 4 columns of folder slots starting at x=29, y=44 (18×18 each)
 *   <li>Player inventory 3×9 starting at y=86
 *   <li>Player hotbar at y=144
 * </ul>
 */
public class PersonalFilingCabinetScreen extends AbstractFilingCabinetScreen<PersonalFilingCabinetMenu> {

    private static final int BG_HEIGHT = 170;

    public PersonalFilingCabinetScreen(PersonalFilingCabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, BG_HEIGHT);
    }

    @Override
    protected Button.OnPress voidExcessAction() {
        return btn -> {
            boolean newValue = !menu.isVoidExcess();
            menu.setVoidExcess(newValue);
            PacketDistributor.sendToServer(new SetPfcVoidExcessPacket(menu.getPfcSlot(), newValue));
        };
    }
}
