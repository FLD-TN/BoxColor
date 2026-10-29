package vn.boxpvp.boxcolor.style;

/** Màu chat hay màu tên. {@link #key()} là khoá trong colors.yml, tiers.yml và messages.yml. */
public enum Channel {
    CHAT("chat"),
    NAME("name");

    private final String key;

    Channel(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
