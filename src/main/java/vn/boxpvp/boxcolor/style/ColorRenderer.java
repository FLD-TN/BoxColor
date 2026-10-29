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
        if (!style.animated() || style.usesShader()) return 0;
        return (nowMillis % style.cycleMillis()) / (double) style.cycleMillis();
    }

    /** Màu RGB của ký tự nhìn thấy thứ {@code index} (đếm từ 0) trong chuỗi có {@code length} ký tự nhìn thấy. */
    public static int rgbAt(ColorStyle style, int index, int length, double phase) {
        // Chạy bằng shader: mọi chữ cùng một mã tín hiệu, resource pack vẽ chuyển động
        if (style.usesShader()) return style.marker();
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

    /** Chuỗi có mã màu cho placeholder. Chuỗi rỗng trả về rỗng. */
    public static String legacy(String text, ColorStyle style, double phase, HexFormat format) {
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
            int rgb = rgbAt(style, index++, length, phase);
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
