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
 * <p>Layout (176 × 270 px):
 * <ul>
 *   <li>Title centred in title bar
 *   <li>"Void Excess: ON/OFF" toggle button (120×14) centred at y=18
 *   <li>8 folder slots (left col) + 8 extraction slots (right col), y=36..179
 *   <li>Player inventory 3×9 starting at y=186
 *   <li>Player hotbar at y=244
 * </ul>
 */
public class PersonalFilingCabinetScreen extends AbstractFilingCabinetScreen<PersonalFilingCabinetMenu> {

    private static final int BG_HEIGHT = 270;
    private static final int PLAYER_INV_Y = 186;

    public PersonalFilingCabinetScreen(PersonalFilingCabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, BG_HEIGHT, PLAYER_INV_Y);
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
