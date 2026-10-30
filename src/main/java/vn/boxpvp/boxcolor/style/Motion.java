package vn.boxpvp.boxcolor.style;

import java.util.Locale;
import vn.boxpvp.boxcolor.style.Effect.Base;

/**
 * Kiểu chuyển động người chơi chọn cho hiệu ứng tên động của mình. {@link #key()} lưu vào database và dùng trong
 * messages.yml. Màu vệt sáng/lấp lánh lấy từ hiệu ứng đang dùng.
 */
public enum Motion {
    /** Kiểu riêng của từng hiệu ứng (colors.yml). */
    SIGNATURE("signature"),
    /** Cách chạy cũ: một vòng màu trên cả tên. */
    CLASSIC("classic"),
    WAVE("wave"),
    SHINE("shine"),
    PULSE("pulse"),
    SPARK("spark"),
    WAVE_SHINE("wave-shine"),
    WAVE_SPARK("wave-spark"),
    SHINE_SPARK("shine-spark");

    private final String key;

    Motion(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** @return {@code null} nếu không đọc được */
    public static Motion parse(String s) {
        if (s == null) return null;
        String k = s.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (Motion m : values()) {
            if (m.key.equals(k)) return m;
        }
        return null;
    }

    /** Cách chạy thực tế khi áp kiểu này lên hiệu ứng có kiểu riêng {@code signature}. */
    public Effect apply(Effect signature) {
        return switch (this) {
            case SIGNATURE -> signature;
            case CLASSIC -> signature.with(Base.CLASSIC, Effect.DEFAULT_WAVE, false, false);
            case WAVE -> signature.with(Base.WAVE, 5, false, false);
            case SHINE -> signature.with(Base.STATIC, Effect.DEFAULT_WAVE, true, false);
            case PULSE -> signature.with(Base.PULSE, Effect.DEFAULT_WAVE, false, false);
            case SPARK -> signature.with(Base.STATIC, Effect.DEFAULT_WAVE, false, true);
            case WAVE_SHINE -> signature.with(Base.WAVE, Effect.DEFAULT_WAVE, true, false);
            case WAVE_SPARK -> signature.with(Base.WAVE, Effect.DEFAULT_WAVE, false, true);
            case SHINE_SPARK -> signature.with(Base.STATIC, Effect.DEFAULT_WAVE, true, true);
        };
    }
}
