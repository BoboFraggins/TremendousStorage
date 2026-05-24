package net.bobofraggins.tremendousstorage.shared.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Client-only mod-bus events for terminal keybinds. */
public final class QuickStackClientEvents {

    private QuickStackClientEvents() {}

    /** Keybind to Quick Stack matching items into the open storage UI. Default: numpad +. */
    public static MouseAwareKeyMapping QUICK_STACK;

    /** Keybind to cycle the network grid sort mode while a storage terminal is open. Unbound by default. */
    public static MouseAwareKeyMapping CYCLE_SORT;

    /** Keybind to clear the crafting grid back to storage while a crafting upgrade screen is open. Default: X. */
    public static MouseAwareKeyMapping CLEAR_GRID_TO_STORAGE;

    /** Keybind to clear the crafting grid to the player's inventory. Unbound by default. */
    public static MouseAwareKeyMapping CLEAR_GRID_TO_INVENTORY;

    /** Returns true when {@link #QUICK_STACK} is set to {@code button} and that mouse button was pressed. */
    public static boolean quickStackMatchesMouse(int button) {
        return QUICK_STACK != null && QUICK_STACK.matchesMouse(button);
    }

    /** Returns true when {@link #CYCLE_SORT} is set to {@code button} and that mouse button was pressed. */
    public static boolean cycleSortMatchesMouse(int button) {
        return CYCLE_SORT != null && CYCLE_SORT.matchesMouse(button);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        QUICK_STACK = new MouseAwareKeyMapping(
                "key.tremendousstorage.quick_stack",
                KeyConflictContext.GUI,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_ADD,
                TremendousStorageKeys.CATEGORY);
        event.register(QUICK_STACK);

        CYCLE_SORT = new MouseAwareKeyMapping(
                "key.tremendousstorage.cycle_sort",
                KeyConflictContext.GUI,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                TremendousStorageKeys.CATEGORY);
        event.register(CYCLE_SORT);

        CLEAR_GRID_TO_STORAGE = new MouseAwareKeyMapping(
                "key.tremendousstorage.clear_grid_to_storage",
                KeyConflictContext.GUI,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                TremendousStorageKeys.CATEGORY);
        event.register(CLEAR_GRID_TO_STORAGE);

        CLEAR_GRID_TO_INVENTORY = new MouseAwareKeyMapping(
                "key.tremendousstorage.clear_grid_to_inventory",
                KeyConflictContext.GUI,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                TremendousStorageKeys.CATEGORY);
        event.register(CLEAR_GRID_TO_INVENTORY);
    }

    /** {@link KeyMapping} subclass that exposes a mouse-button match check via the protected {@code key} field. */
    public static final class MouseAwareKeyMapping extends KeyMapping {
        MouseAwareKeyMapping(
                String description,
                KeyConflictContext ctx,
                KeyModifier mod,
                InputConstants.Type type,
                int keyCode,
                KeyMapping.Category category) {
            super(description, ctx, mod, type, keyCode, category);
        }

        public boolean matchesMouse(int button) {
            return key.getType() == InputConstants.Type.MOUSE && key.getValue() == button;
        }
    }
}
