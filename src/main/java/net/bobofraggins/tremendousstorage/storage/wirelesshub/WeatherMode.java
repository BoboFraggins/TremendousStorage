package net.bobofraggins.tremendousstorage.storage.wirelesshub;

/** The HAARP upgrade's desired weather state for the Wireless Hub. */
public enum WeatherMode {
    OFF,
    NO_RAIN,
    RAIN,
    THUNDERSTORM;

    public static WeatherMode fromOrdinal(int i) {
        WeatherMode[] values = values();
        return values[Math.max(0, Math.min(i, values.length - 1))];
    }
}
