package net.bobofraggins.tremendousstorage.shared.input;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Shared keybind constants for TremendousStorage. */
public final class TremendousStorageKeys {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "tremendous_storage"));

    private TremendousStorageKeys() {}
}
