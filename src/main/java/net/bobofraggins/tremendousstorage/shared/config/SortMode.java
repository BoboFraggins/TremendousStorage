package net.bobofraggins.tremendousstorage.shared.config;

/** Sort mode for the network item grid. Cycles through Amount → Name → Mod → … */
public enum SortMode {
    AMOUNT("sort.tremendousstorage.amount"),
    NAME("sort.tremendousstorage.name"),
    MOD("sort.tremendousstorage.mod");

    private final String translationKey;

    SortMode(String translationKey) {
        this.translationKey = translationKey;
    }

    /** Returns the localised display name for this sort mode. */
    public String displayName() {
        return net.minecraft.network.chat.Component.translatable(translationKey).getString();
    }

    public SortMode next() {
        SortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public SortMode prev() {
        SortMode[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }
}
