package vn.boxpvp.boxcolor.util;

import vn.boxpvp.boxcolor.config.Messages;

/** Thời gian còn lại kiểu "1 giờ 5 phút", tối đa hai đơn vị. */
public final class Durations {

    private Durations() {
    }

    public static String format(long millis, Messages messages) {
        long total = Math.max(1L, (millis + 999L) / 1000L);
        long days = total / 86_400L;
        long hours = total % 86_400L / 3_600L;
        long minutes = total % 3_600L / 60L;
        long seconds = total % 60L;
        StringBuilder sb = new StringBuilder();
        int parts = 0;
        parts = append(sb, days, messages.raw("units.day"), parts);
        parts = append(sb, hours, messages.raw("units.hour"), parts);
        parts = append(sb, minutes, messages.raw("units.minute"), parts);
        append(sb, seconds, messages.raw("units.second"), parts);
        return sb.toString();
    }

    private static int append(StringBuilder sb, long value, String unit, int parts) {
        if (value <= 0 || parts >= 2) return parts;
        if (!sb.isEmpty()) sb.append(' ');
        sb.append(value).append(' ').append(unit);
        return parts + 1;
    }
}
