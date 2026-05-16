package net.bobofraggins.tremendousstorage.guidebook;

import java.lang.reflect.Method;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** The Tremendous Storage Guide book item. Right-clicking opens the Patchouli guide if present. */
public class TremendousStorageGuideItem extends Item {

    private static final Identifier BOOK_ID = Identifier.fromNamespaceAndPath("tremendousstorage", "guide");

    public TremendousStorageGuideItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && ModList.get().isLoaded("patchouli")) {
            try {
                Object api = Class.forName("vazkii.patchouli.api.PatchouliAPI")
                        .getMethod("get")
                        .invoke(null);
                for (Method m : api.getClass().getMethods()) {
                    if (m.getName().equals("openBookGUI") && m.getParameterCount() == 2) {
                        m.invoke(api, (ServerPlayer) player, BOOK_ID);
                        break;
                    }
                }
            } catch (ReflectiveOperationException | NoClassDefFoundError ignored) {
            }
        }
        return InteractionResult.SUCCESS;
    }
}
