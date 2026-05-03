package net.bobofraggins.tremendousstorage.guidebook;

import java.lang.reflect.Method;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** The Tremendous Storage Guide book item. Right-clicking opens the Patchouli guide if present. */
public class TremendousStorageGuideItem extends Item {

    private static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath("tremendousstorage", "guide");

    public TremendousStorageGuideItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && ModList.get().isLoaded("patchouli")) {
            try {
                Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
                Object api = apiClass.getMethod("get").invoke(null);
                Method openBook = api.getClass().getMethod("openBookGUI", ServerPlayer.class, ResourceLocation.class);
                openBook.invoke(api, (ServerPlayer) player, BOOK_ID);
            } catch (ReflectiveOperationException | NoClassDefFoundError ignored) {
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
