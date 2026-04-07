package net.bobofraggins.tremendousstorage.shared.util;

/** Shared formatting for large item counts. */
public final class CountFormat {

    private CountFormat() {}

    /**
     * Formats a count as an abbreviated string:
     * {@code 999} → {@code "999"}, {@code 1000} → {@code "1k"}, {@code 1048576} → {@code "1M"}.
     */
    public static String format(long n) {
        if (n >= 1_000_000) return (n / 1_000_000) + "M";
        if (n >= 1_000) return (n / 1_000) + "k";
        return Long.toString(n);
    }
}
