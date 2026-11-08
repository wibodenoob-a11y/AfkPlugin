package dev.afk;

public final class TimeUtil {
    private TimeUtil() {}

    public static long parseToMillis(String s) {
        if (s == null || s.isEmpty()) return 0;
        char unit = s.charAt(s.length() - 1);
        long value;
        try {
            value = Long.parseLong(s.substring(0, s.length() - 1));
        } catch (NumberFormatException e) {
            return 0;
        }
        return switch (Character.toLowerCase(unit)) {
            case 's' -> value * 1000L;
            case 'm' -> value * 60_000L;
            case 'h' -> value * 3_600_000L;
            case 'd' -> value * 86_400_000L;
            default -> value * 1000L;
        };
    }
}
