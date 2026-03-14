package net.bobofraggins.intellistore.shared.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for the Quick Stack keybind. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QuickStackClientEvents {

    private QuickStackClientEvents() {}

    /** Keybind to Quick Stack matching items into the open storage UI. Default: numpad +. */
    public static KeyMapping QUICK_STACK;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        QUICK_STACK = new KeyMapping(
                "key.intellistore.quick_stack",
                KeyConflictContext.GUI,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_ADD,
                "key.categories.intellistore");
        event.register(QUICK_STACK);
    }
}
