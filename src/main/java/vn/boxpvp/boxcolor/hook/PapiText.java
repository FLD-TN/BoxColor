package vn.boxpvp.boxcolor.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;

/**
 * Thay placeholder PlaceholderAPI trong chữ cấu hình không gắn với người chơi, ví dụ tên tier là ảnh rank của
 * ItemsAdder ({@code %img_wellsetups_ranks_ultimate%}). Không có PlaceholderAPI thì trả nguyên văn.
 * Chỉ chạm tới lớp của PlaceholderAPI khi plugin đó đang bật.
 */
public final class PapiText {

    private PapiText() {
    }

    public static String apply(String text) {
        if (text == null || text.indexOf('%') < 0) return text;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return text;
        return Hook.apply(text);
    }

    /** Lớp riêng để JVM chỉ nạp PlaceholderAPI khi thật sự gọi tới. */
    private static final class Hook {
        static String apply(String text) {
            return PlaceholderAPI.setPlaceholders(null, text);
        }
    }
}
