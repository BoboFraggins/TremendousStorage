package net.bobofraggins.tremendousstorage.shared.util;

/** Shared formatting for large item counts. */
public final class CountFormat {

    private CountFormat() {}

    /**
     * Formats a count as an abbreviated string with one decimal place when meaningful:
     * {@code 999} → {@code "999"}, {@code 1200} → {@code "1.2k"}, {@code 1000} → {@code "1k"},
     * {@code 1_200_000} → {@code "1.2M"}.
     */
    public static String format(long n) {
        if (n >= 1_000_000) {
            long whole = n / 1_000_000;
            long frac = (n % 1_000_000) / 100_000;
            return frac == 0 ? whole + "M" : whole + "." + frac + "M";
        }
        if (n >= 1_000) {
            long whole = n / 1_000;
            long frac = (n % 1_000) / 100;
            return frac == 0 ? whole + "k" : whole + "." + frac + "k";
        }
        return Long.toString(n);
    }
}
