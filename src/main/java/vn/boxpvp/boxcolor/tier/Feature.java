package vn.boxpvp.boxcolor.tier;

/** Tính năng mở theo tier, khoá trong {@code tiers.<n>.features}. */
public enum Feature {
    CUSTOM_CHAT("custom-chat"),
    /** "Màu tùy chỉnh": tự đặt tên, tô màu từng chữ ({@code /nick}). */
    NICK("nick"),
    /** Không còn dùng từ 1.9.1 (ai cũng vào được trang hiệu ứng động); giữ để tiers.yml cũ không báo lỗi. */
    ANIMATED_NAME("animated-name");

    private final String key;

    Feature(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Feature parse(String s) {
        // custom-name (1.3) đã gộp vào nick
        if (s.trim().equalsIgnoreCase("custom-name")) return NICK;
        for (Feature f : values()) {
            if (f.key.equalsIgnoreCase(s.trim())) return f;
        }
        return null;
    }
}
