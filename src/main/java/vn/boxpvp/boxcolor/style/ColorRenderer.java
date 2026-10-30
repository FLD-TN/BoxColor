package vn.boxpvp.boxcolor.style;

import java.awt.Color;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

/**
 * Tô màu một chuỗi theo {@link ColorStyle}. Chỉ ký tự nhìn thấy được mới tính vị trí trong gradient (dấu cách
 * giữ nguyên, không tốn mã màu). Không có trạng thái, an toàn đa luồng (placeholder có thể được gọi async).
 */
public final class ColorRenderer {

    /** Cách viết mã màu trong chuỗi trả về cho placeholder. */
    public enum HexFormat {
        /** {@code &#RRGGBB}: TAB, UnlimitedNameTags, EzChat và phần lớn plugin đọc mã '&'. */
        AMPERSAND,
        /** {@code §x§R§R§G§G§B§B}: định dạng gốc của Bukkit, cho plugin không tự dịch '&'. */
        SECTION,
        /** {@code <#RRGGBB>}: plugin dùng MiniMessage. */
        MINIMESSAGE
    }

    private ColorRenderer() {
    }

    /** Pha chuyển động tại thời điểm {@code nowMillis}, trong [0, 1). Kiểu tĩnh luôn là 0. */
    public static double phase(ColorStyle style, long nowMillis) {
        if (!style.animated()) return 0;
        return (nowMillis % style.cycleMillis()) / (double) style.cycleMillis();
    }

    /** Màu RGB của ký tự nhìn thấy thứ {@code index} (đếm từ 0) trong chuỗi có {@code length} ký tự nhìn thấy. */
    public static int rgbAt(ColorStyle style, int index, int length, double phase) {
        int len = Math.max(1, length);
        switch (style.kind()) {
            case SOLID:
                return style.colors().isEmpty() ? 0xFFFFFF : style.colors().get(0);
            case PER_CHAR: {
                List<Integer> colors = style.colors();
                if (colors.isEmpty()) return 0xFFFFFF;
                int rgb = colors.get(Math.min(index, colors.size() - 1));
                return rgb == ColorStyle.NO_COLOR ? 0xFFFFFF : rgb;
            }
            case RAINBOW: {
                float hue = (float) frac(index / (double) len - phase);
                return Color.HSBtoRGB(hue, style.saturation(), style.brightness()) & 0xFFFFFF;
            }
            case GRADIENT:
            default: {
                List<Integer> stops = style.colors();
                if (stops.isEmpty()) return 0xFFFFFF;
                if (stops.size() == 1) return stops.get(0);
                if (style.animated()) {
                    // Vòng kín c0 -> c1 -> ... -> c0 để màu chạy liền mạch, không giật ở điểm nối
                    double t = frac(index / (double) len - phase);
                    return interpolateLoop(stops, t);
                }
                double t = len <= 1 ? 0 : index / (double) (len - 1);
                return interpolate(stops, t);
            }
        }
    }

    /**
     * Màu của chữ nhìn thấy thứ {@code index} theo kiểu chuyển động {@code effect}, tại thời điểm {@code nowMillis}
     * (người gọi đã làm tròn theo khung hình). Cùng thời điểm luôn ra cùng màu: ai xem cũng thấy giống nhau.
     */
    public static int rgbAt(ColorStyle style, Effect effect, int index, int length, long nowMillis) {
        double phase = phase(style, nowMillis);
        int len = Math.max(1, length);
        int rgb = switch (effect.base()) {
            case CLASSIC -> rgbAt(style, index, len, phase);
            case WAVE -> loopColor(style, frac(index / (double) effect.wave() - phase));
            case PULSE -> loopColor(style, frac(phase - index * 0.025));
            case STATIC -> staticColor(style, index, len);
        };
        if (effect.shine()) {
            // Quét qua tên trong 60% vòng, nghỉ 40% (lúc nghỉ chuỗi không đổi, TAB/UNT không gửi lại)
            double q = phase / 0.6;
            if (q <= 1) {
                double width = 2.5;
                double pos = -width + q * (len + 2 * width);
                double k = Math.max(0, 1 - Math.abs(index - pos) / width);
                k = k * k * (3 - 2 * k);
                if (k > 0) rgb = lerp(rgb, effect.highlight(), k * 0.92);
            }
        }
        if (effect.spark()) {
            // Mỗi nhịp 120ms vài chữ bắt đầu chớp, tắt dần trong 4 nhịp
            long slot = Math.floorDiv(nowMillis, 120L);
            double k = 0;
            for (int age = 0; age < 4; age++) {
                if (hash(index + 1, slot - age) < effect.sparkDensity()) k = Math.max(k, 1 - age / 4.0);
            }
            if (k > 0) rgb = lerp(rgb, effect.highlight(), k * 0.95);
        }
        return rgb;
    }

    /** Màu tại vị trí {@code t} trên vòng màu kín (cầu vồng hoặc c0 -> c1 -> ... -> c0). */
    private static int loopColor(ColorStyle style, double t) {
        if (style.kind() == StyleKind.RAINBOW) {
            return Color.HSBtoRGB((float) t, style.saturation(), style.brightness()) & 0xFFFFFF;
        }
        List<Integer> stops = style.colors();
        if (stops.isEmpty()) return 0xFFFFFF;
        if (stops.size() == 1) return stops.get(0);
        return interpolateLoop(stops, t);
    }

    /** Gradient đứng yên trải đều trên tên. */
    private static int staticColor(ColorStyle style, int index, int len) {
        double t = len <= 1 ? 0 : index / (double) (len - 1);
        if (style.kind() == StyleKind.RAINBOW) {
            return Color.HSBtoRGB((float) (t * 0.85), style.saturation(), style.brightness()) & 0xFFFFFF;
        }
        List<Integer> stops = style.colors();
        if (stops.isEmpty()) return 0xFFFFFF;
        if (stops.size() == 1) return stops.get(0);
        return interpolate(stops, t);
    }

    private static double hash(long a, long b) {
        int h = (int) (a * 374761393L + b * 668265263L);
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFFFFL) / 4294967296.0;
    }

    /** Chuỗi có mã màu cho placeholder. Chuỗi rỗng trả về rỗng. */
    public static String legacy(String text, ColorStyle style, double phase, HexFormat format) {
        return legacy(text, (index, length) -> rgbAt(style, index, length, phase), format);
    }

    /** Như {@link #legacy(String, ColorStyle, double, HexFormat)} nhưng chạy theo kiểu chuyển động. */
    public static String legacy(String text, ColorStyle style, Effect effect, long nowMillis, HexFormat format) {
        return legacy(text, (index, length) -> rgbAt(style, effect, index, length, nowMillis), format);
    }

    private interface CharColor {
        int at(int index, int length);
    }

    private static String legacy(String text, CharColor color, HexFormat format) {
        int length = visibleLength(text);
        StringBuilder out = new StringBuilder(text.length() * (format == HexFormat.SECTION ? 15 : 9));
        int last = -1;
        int index = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isWhitespace(cp)) {
                out.appendCodePoint(cp);
                continue;
            }
            int rgb = color.at(index++, length);
            if (rgb != last) {
                appendCode(out, rgb, format);
                last = rgb;
            }
            if (format == HexFormat.MINIMESSAGE && (cp == '<' || cp == '\\')) out.append('\\');
            out.appendCodePoint(cp);
        }
        return out.toString();
    }

    /** Mã màu đơn (không kèm chữ), ví dụ để đặt trước {@code {message}} trong format chat. */
    public static String code(int rgb, HexFormat format) {
        StringBuilder sb = new StringBuilder(14);
        appendCode(sb, rgb, format);
        return sb.toString();
    }

    /**
     * Component đã tô màu, cho nội dung chat và GUI. Đoạn nào khớp {@code keepPlain} (ví dụ {@code [item]}) được
     * giữ nguyên một khối không màu để plugin khác (InteractiveChat...) vẫn nhận ra; {@code null} thì tô hết.
     */
    public static Component component(String text, ColorStyle style, double phase, Pattern keepPlain) {
        TextComponent.Builder root = Component.text();
        if (keepPlain == null) {
            appendColored(root, text, style, phase, 0, visibleLength(text));
            return root.build();
        }
        // Độ dài gradient chỉ tính phần được tô
        Matcher m = keepPlain.matcher(text);
        int colored = 0;
        int cursor = 0;
        while (m.find()) {
            colored += visibleLength(text.substring(cursor, m.start()));
            cursor = m.end();
        }
        colored += visibleLength(text.substring(cursor));

        m.reset();
        cursor = 0;
        int index = 0;
        while (m.find()) {
            String before = text.substring(cursor, m.start());
            index = appendColored(root, before, style, phase, index, colored);
            if (m.end() > m.start()) root.append(Component.text(m.group()));
            cursor = m.end();
        }
        appendColored(root, text.substring(cursor), style, phase, index, colored);
        return root.build();
    }

    /** Số ký tự nhìn thấy (không tính khoảng trắng), theo code point. */
    public static int visibleLength(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isWhitespace(cp)) n++;
        }
        return n;
    }

    /** @return chỉ số ký tự nhìn thấy kế tiếp */
    private static int appendColored(TextComponent.Builder root, String text, ColorStyle style, double phase,
            int index, int length) {
        StringBuilder run = new StringBuilder();
        int runColor = -1;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isWhitespace(cp)) {
                run.appendCodePoint(cp);
                continue;
            }
            int rgb = rgbAt(style, index++, length, phase);
            if (rgb != runColor && !run.isEmpty()) {
                root.append(runComponent(run, runColor));
                run.setLength(0);
            }
            runColor = rgb;
            run.appendCodePoint(cp);
        }
        if (!run.isEmpty()) root.append(runComponent(run, runColor));
        return index;
    }

    private static Component runComponent(StringBuilder run, int rgb) {
        // Chỉ có khoảng trắng (chưa gặp ký tự nào): không cần màu
        return rgb < 0 ? Component.text(run.toString()) : Component.text(run.toString(), TextColor.color(rgb));
    }

    private static void appendCode(StringBuilder out, int rgb, HexFormat format) {
        String hex = String.format("%06X", rgb & 0xFFFFFF);
        switch (format) {
            case SECTION -> {
                out.append("§x");
                for (int i = 0; i < 6; i++) out.append('§').append(hex.charAt(i));
            }
            case MINIMESSAGE -> out.append("<#").append(hex).append('>');
            default -> out.append("&#").append(hex);
        }
    }

    private static int interpolate(List<Integer> stops, double t) {
        double pos = Math.max(0, Math.min(1, t)) * (stops.size() - 1);
        int i = Math.min(stops.size() - 2, (int) Math.floor(pos));
        return lerp(stops.get(i), stops.get(i + 1), pos - i);
    }

    private static int interpolateLoop(List<Integer> stops, double t) {
        int n = stops.size();
        double pos = t * n;
        int i = Math.min(n - 1, (int) Math.floor(pos));
        return lerp(stops.get(i), stops.get((i + 1) % n), pos - i);
    }

    private static int lerp(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static double frac(double x) {
        double f = x - Math.floor(x);
        return f >= 1 ? 0 : f;
    }
}
