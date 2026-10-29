package vn.boxpvp.boxcolor.tier;

/** Tính năng mở theo tier, khoá trong {@code tiers.<n>.features}. */
public enum Feature {
    CUSTOM_CHAT("custom-chat"),
    CUSTOM_NAME("custom-name"),
    NICK("nick");

    private final String key;

    Feature(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Feature parse(String s) {
        for (Feature f : values()) {
            if (f.key.equalsIgnoreCase(s.trim())) return f;
        }
        return null;
    }
}
