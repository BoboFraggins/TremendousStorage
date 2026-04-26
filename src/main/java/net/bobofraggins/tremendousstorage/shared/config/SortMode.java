package net.bobofraggins.tremendousstorage.shared.config;

/** Sort mode for the network item grid. Cycles through Amount → Name → Mod → … */
public enum SortMode {
    AMOUNT("Amount", "Amt"),
    NAME("Name", "Name"),
    MOD("Mod", "Mod");

    private final String displayName;
    private final String shortLabel;

    SortMode(String displayName, String shortLabel) {
        this.displayName = displayName;
        this.shortLabel = shortLabel;
    }

    public String displayName() {
        return displayName;
    }

    public String shortLabel() {
        return shortLabel;
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
