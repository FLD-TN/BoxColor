package vn.boxpvp.boxcolor.style;

import java.util.Locale;

public enum StyleKind {
    SOLID,
    GRADIENT,
    RAINBOW,
    /** Màu riêng cho từng chữ, theo đúng mã người chơi gõ (chỉ có ở tên tùy chỉnh, không đọc từ colors.yml). */
    PER_CHAR;

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
