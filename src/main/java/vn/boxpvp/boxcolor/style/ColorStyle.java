package vn.boxpvp.boxcolor.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;

/**
 * Một kiểu màu trong colors.yml, hoặc kiểu tự chỉnh của người chơi ({@link #custom}).
 *
 * @param colors  các mốc màu RGB (SOLID: 1 mốc; GRADIENT: 2 mốc trở lên; RAINBOW: bỏ qua)
 * @param animated gradient/cầu vồng chạy liên tục (chỉ có tác dụng ở chỗ tự vẽ lại: TAB, nametag, GUI)
 * @param cycleMillis thời gian chạy hết một vòng màu khi {@code animated}
 * @param saturation độ đậm của cầu vồng (0..1)
 * @param brightness độ sáng của cầu vồng (0..1)
 * @param icon icon trong GUI, {@code null} thì dùng icon mặc định của config.yml
 */
public record ColorStyle(String id, String display, StyleKind kind, List<Integer> colors, boolean animated,
        long cycleMillis, float saturation, float brightness, Material icon) {

    /** Mã của kiểu tự chỉnh trong dữ liệu người chơi. */
    public static final String CUSTOM_ID = "custom";

    public ColorStyle {
        colors = List.copyOf(colors);
        cycleMillis = Math.max(200L, cycleMillis);
        saturation = Math.max(0f, Math.min(1f, saturation));
        brightness = Math.max(0f, Math.min(1f, brightness));
    }

    /** Kiểu tự chỉnh: 1 mã là màu đơn, từ 2 mã trở lên là gradient tĩnh. */
    public static ColorStyle custom(List<Integer> rgb) {
        StyleKind kind = rgb.size() == 1 ? StyleKind.SOLID : StyleKind.GRADIENT;
        return new ColorStyle(CUSTOM_ID, CUSTOM_ID, kind, rgb, false, 3000L, 1f, 1f, null);
    }

    public boolean isCustom() {
        return CUSTOM_ID.equals(id);
    }

    /** Màu đầu tiên, dùng cho icon da nhuộm và placeholder {@code chat}. */
    public int firstColor() {
        if (kind == StyleKind.RAINBOW || colors.isEmpty()) return ColorRenderer.rgbAt(this, 0, 1, 0);
        return colors.get(0);
    }

    /**
     * Mã lưu vào database của kiểu tự chỉnh: {@code #FFB3C6,#A0E7E5}. Kiểu có sẵn lưu bằng {@link #id()}.
     */
    public String encodeCustom() {
        List<String> parts = new ArrayList<>(colors.size());
        for (int rgb : colors) parts.add(hex(rgb));
        return String.join(",", parts);
    }

    public static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }
}
