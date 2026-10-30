package vn.boxpvp.boxcolor.style;

import java.util.Locale;

/**
 * Cách chuyển động của một hiệu ứng tên động.
 *
 * @param base         nền màu, xem {@link Base}
 * @param wave         số chữ của một vòng màu khi {@link Base#WAVE}
 * @param shine        vệt sáng quét qua tên rồi nghỉ
 * @param spark        vài chữ chớp sáng rồi tắt dần
 * @param highlight    màu của vệt sáng và tia lấp lánh
 * @param sparkDensity tỉ lệ chữ bắt đầu chớp ở mỗi nhịp lấp lánh (0..0.5)
 */
public record Effect(Base base, int wave, boolean shine, boolean spark, int highlight, double sparkDensity) {

    public enum Base {
        /** Một vòng màu trải trên cả tên, trượt chậm (cách của bản 1.9 trở về trước). */
        CLASSIC,
        /** Vòng màu lặp lại mỗi {@code wave} chữ: chữ cạnh nhau khác màu hẳn. */
        WAVE,
        /** Gradient đứng yên, dùng làm nền cho vệt sáng/lấp lánh. */
        STATIC,
        /** Cả tên đổi màu cùng lúc qua các mốc màu. */
        PULSE;

        /** @return {@code null} nếu không đọc được */
        public static Base parse(String s) {
            if (s == null) return null;
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static final int DEFAULT_WAVE = 6;
    public static final int DEFAULT_HIGHLIGHT = 0xFFFFFF;
    public static final double DEFAULT_SPARK_DENSITY = 0.09;
    public static final Effect CLASSIC = new Effect(Base.CLASSIC, DEFAULT_WAVE, false, false, DEFAULT_HIGHLIGHT,
            DEFAULT_SPARK_DENSITY);

    public Effect {
        if (base == null) base = Base.CLASSIC;
        wave = Math.max(2, wave);
        sparkDensity = Math.max(0, Math.min(0.5, sparkDensity));
    }

    public Effect with(Base base, int wave, boolean shine, boolean spark) {
        return new Effect(base, wave, shine, spark, highlight, sparkDensity);
    }
}
