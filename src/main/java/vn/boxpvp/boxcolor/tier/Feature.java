package vn.boxpvp.boxcolor.tier;

/** Tính năng mở theo tier, khoá trong {@code tiers.<n>.features}. */
public enum Feature {
    CUSTOM_CHAT("custom-chat", 0),
    /** "Màu tùy chỉnh": tự đặt tên, tô màu từng chữ ({@code /nick}). */
    NICK("nick", 0),
    /** Không còn dùng từ 1.9.1 (ai cũng vào được trang hiệu ứng động); giữ để tiers.yml cũ không báo lỗi. */
    ANIMATED_NAME("animated-name", 0),
    /** Tự chọn kiểu chuyển động cho hiệu ứng tên động; chưa mở thì luôn chạy kiểu Mặc định. */
    MOTION("motion", 7);

    private final String key;
    private final int defaultTier;

    Feature(String key, int defaultTier) {
        this.key = key;
        this.defaultTier = defaultTier;
    }

    public String key() {
        return key;
    }

    /** Tier mở tính năng khi tiers.yml không tier nào ghi nó (tiers.yml của bản cũ); 0 = không có. */
    public int defaultTier() {
        return defaultTier;
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
