package vn.boxpvp.boxcolor.style;

import java.util.Locale;

public enum StyleKind {
    SOLID,
    GRADIENT,
    RAINBOW;

    /** {@code solid} / {@code gradient} / {@code rainbow}, không phân biệt hoa thường; sai thì {@code null}. */
    public static StyleKind parse(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
