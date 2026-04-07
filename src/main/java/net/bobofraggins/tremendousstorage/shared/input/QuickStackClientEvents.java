package net.bobofraggins.tremendousstorage.shared.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for terminal keybinds. */
@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QuickStackClientEvents {

    private QuickStackClientEvents() {}

    /** Keybind to Quick Stack matching items into the open storage UI. Default: numpad +. */
    public static KeyMapping QUICK_STACK;

    /** Keybind to cycle the network grid sort mode while a storage terminal is open. Unbound by default. */
    public static KeyMapping CYCLE_SORT;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        QUICK_STACK = new KeyMapping(
                "key.tremendousstorage.quick_stack",
                KeyConflictContext.GUI,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_ADD,
                "key.categories.tremendousstorage");
        event.register(QUICK_STACK);

        CYCLE_SORT = new KeyMapping(
                "key.tremendousstorage.cycle_sort",
                KeyConflictContext.GUI,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.tremendousstorage");
        event.register(CYCLE_SORT);
    }
}
