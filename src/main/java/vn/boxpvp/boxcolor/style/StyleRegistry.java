package vn.boxpvp.boxcolor.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Các kiểu màu có sẵn trong colors.yml, theo đúng thứ tự trong file (thứ tự hiện trong GUI).
 * Thay bằng bản mới khi reload; đối tượng cũ vẫn dùng được cho tới khi bị bỏ.
 */
public final class StyleRegistry {

    private final Map<Channel, Map<String, ColorStyle>> styles;
    /** {@code kênh.mã} → {@code unlock-text} trong colors.yml (cách mở màu không nằm trong tier nào). */
    private final Map<String, String> unlockTexts;

    private StyleRegistry(Map<Channel, Map<String, ColorStyle>> styles, Map<String, String> unlockTexts) {
        this.styles = styles;
        this.unlockTexts = unlockTexts;
    }

    /**
     * @param warn nhận từng dòng cảnh báo (kiểu bị bỏ qua và lý do)
     */
    public static StyleRegistry load(YamlConfiguration yml, Consumer<String> warn) {
        Map<Channel, Map<String, ColorStyle>> all = new EnumMap<>(Channel.class);
        Map<String, String> unlockTexts = new HashMap<>();
        for (Channel channel : Channel.values()) {
            Map<String, ColorStyle> map = new LinkedHashMap<>();
            ConfigurationSection section = yml.getConfigurationSection(channel.key());
            if (section != null) {
                for (String id : section.getKeys(false)) {
                    ConfigurationSection s = section.getConfigurationSection(id);
                    String key = id.toLowerCase(Locale.ROOT);
                    if (s == null) continue;
                    if (ColorStyle.CUSTOM_ID.equals(key)) {
                        warn.accept(channel.key() + "." + id + ": mã 'custom' dành cho màu tự chỉnh");
                        continue;
                    }
                    ColorStyle style = parse(key, s, channel.key() + "." + id, warn);
                    if (style == null) continue;
                    map.put(key, style);
                    String unlock = s.getString("unlock-text");
                    if (unlock != null && !unlock.isBlank()) unlockTexts.put(channel.key() + "." + key, unlock);
                }
            }
            all.put(channel, Collections.unmodifiableMap(map));
        }
        return new StyleRegistry(all, Map.copyOf(unlockTexts));
    }

    private static ColorStyle parse(String id, ConfigurationSection s, String path, Consumer<String> warn) {
        StyleKind kind = StyleKind.parse(s.getString("type", "solid"));
        if (kind == null || kind == StyleKind.PER_CHAR) {
            warn.accept(path + ": type phải là solid, gradient hoặc rainbow");
            return null;
        }
        List<Integer> colors = new ArrayList<>();
        for (String hex : s.getStringList("colors")) {
            int rgb = Hex.parse(hex);
            if (rgb < 0) {
                warn.accept(path + ": mã màu sai '" + hex + "'");
                return null;
            }
            colors.add(rgb);
        }
        if (kind == StyleKind.SOLID && colors.isEmpty()) {
            warn.accept(path + ": màu đơn cần 1 mã trong colors");
            return null;
        }
        if (kind == StyleKind.GRADIENT && colors.size() < 2) {
            warn.accept(path + ": gradient cần ít nhất 2 mã trong colors");
            return null;
        }
        Material icon = null;
        String iconName = s.getString("icon");
        if (iconName != null && !iconName.isBlank()) {
            icon = Material.matchMaterial(iconName);
            if (icon == null || !icon.isItem()) {
                warn.accept(path + ": icon không tồn tại '" + iconName + "', dùng icon mặc định");
                icon = null;
            }
        }
        boolean animated = s.getBoolean("animated", false) && kind != StyleKind.SOLID;
        return new ColorStyle(id, s.getString("display", id), kind, colors, animated,
                s.getLong("speed", 3000L),
                (float) s.getDouble("saturation", 0.6),
                (float) s.getDouble("brightness", 1.0),
                icon);
    }

    /** {@code unlock-text} của màu, {@code null} nếu không đặt. */
    public String unlockText(Channel channel, String id) {
        return unlockTexts.get(channel.key() + "." + id);
    }

    public ColorStyle get(Channel channel, String id) {
        return id == null ? null : styles.get(channel).get(id.toLowerCase(Locale.ROOT));
    }

    /** Theo thứ tự trong colors.yml. */
    public List<ColorStyle> list(Channel channel) {
        return List.copyOf(styles.get(channel).values());
    }

    public int count(Channel channel) {
        return styles.get(channel).size();
    }
}
