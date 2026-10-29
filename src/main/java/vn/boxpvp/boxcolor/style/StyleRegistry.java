package vn.boxpvp.boxcolor.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Các kiểu màu có sẵn trong colors.yml, theo đúng thứ tự trong file (thứ tự hiện trong GUI).
 * Thay bằng bản mới khi reload; đối tượng cũ vẫn dùng được cho tới khi bị bỏ.
 */
public final class StyleRegistry {

    /** Số hiệu ứng shader tối đa (1..16). */
    public static final int MAX_SHADER_EFFECTS = 16;

    private final Map<Channel, Map<String, ColorStyle>> styles;

    private StyleRegistry(Map<Channel, Map<String, ColorStyle>> styles) {
        this.styles = styles;
    }

    /**
     * @param warn nhận từng dòng cảnh báo (kiểu bị bỏ qua và lý do)
     */
    public static StyleRegistry load(YamlConfiguration yml, Consumer<String> warn) {
        Map<Channel, Map<String, ColorStyle>> all = new EnumMap<>(Channel.class);
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
                    if (style != null) map.put(key, style);
                }
            }
            all.put(channel, Collections.unmodifiableMap(map));
        }
        return new StyleRegistry(all);
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
        int shader = s.getInt("shader", 0);
        if (shader != 0 && (shader < 1 || shader > MAX_SHADER_EFFECTS)) {
            warn.accept(path + ": shader phải từ 1 đến " + MAX_SHADER_EFFECTS);
            shader = 0;
        }
        if (shader != 0 && !animated) {
            warn.accept(path + ": shader chỉ dùng cho màu có animated: true, bỏ qua");
            shader = 0;
        }
        return new ColorStyle(id, s.getString("display", id), kind, colors, animated,
                s.getLong("speed", 3000L),
                (float) s.getDouble("saturation", 0.6),
                (float) s.getDouble("brightness", 1.0),
                icon, shader, ColorStyle.NO_COLOR);
    }

    /**
     * Bản sao đã gán mã tín hiệu shader cho mọi màu có {@code shader:}. Mã chọn gần màu đầu tiên của hiệu ứng (người
     * chơi không cài pack thấy gần giống), không trùng mã khác, không trùng màu nào trong colors.yml và {@code reserved}.
     * Cùng một colors.yml luôn ra cùng mã.
     */
    public StyleRegistry withShaderMarkers(Set<Integer> reserved, Consumer<String> warn) {
        Set<Integer> taken = new HashSet<>(reserved);
        for (Map<String, ColorStyle> map : styles.values()) {
            for (ColorStyle style : map.values()) taken.addAll(style.colors());
        }
        // Theo số hiệu ứng, rồi kênh, rồi thứ tự trong file: mã ổn định khi thêm màu không dùng shader
        Map<Integer, String> owners = new TreeMap<>();
        Map<String, Integer> assigned = new HashMap<>();
        for (Channel channel : Channel.values()) {
            for (ColorStyle style : styles.get(channel).values()) {
                if (style.shader() == 0) continue;
                String key = channel.key() + "." + style.id();
                String previous = owners.putIfAbsent(style.shader(), key);
                if (previous != null) {
                    warn.accept(key + ": shader " + style.shader() + " đã dùng cho " + previous + ", bỏ qua");
                }
            }
        }
        for (Map.Entry<Integer, String> e : owners.entrySet()) {
            String[] parts = e.getValue().split("\\.", 2);
            ColorStyle style = styles.get(Channel.valueOf(parts[0].toUpperCase(Locale.ROOT))).get(parts[1]);
            int marker = pickMarker(style.firstColor(), taken);
            taken.add(marker);
            assigned.put(e.getValue(), marker);
        }
        Map<Channel, Map<String, ColorStyle>> out = new EnumMap<>(Channel.class);
        for (Channel channel : Channel.values()) {
            Map<String, ColorStyle> map = new LinkedHashMap<>();
            for (ColorStyle style : styles.get(channel).values()) {
                Integer marker = assigned.get(channel.key() + "." + style.id());
                map.put(style.id(), marker == null ? style : style.withMarker(marker));
            }
            out.put(channel, Collections.unmodifiableMap(map));
        }
        return new StyleRegistry(out);
    }

    /** Màu gần {@code base} nhất (đổi vài bit thấp) chưa bị dùng. */
    static int pickMarker(int base, Set<Integer> taken) {
        for (int d = 1; d < 0x1000; d++) {
            int candidate = (base ^ (d & 0xF) ^ ((d >> 4 & 0xF) << 8) ^ ((d >> 8 & 0xF) << 16)) & 0xFFFFFF;
            if (!taken.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("không tìm được mã tín hiệu");
    }

    /** Các màu đang chạy bằng shader, theo số hiệu ứng. */
    public List<ColorStyle> shaderStyles() {
        List<ColorStyle> out = new ArrayList<>();
        for (Map<String, ColorStyle> map : styles.values()) {
            for (ColorStyle style : map.values()) {
                if (style.usesShader()) out.add(style);
            }
        }
        out.sort(Comparator.comparingInt(ColorStyle::shader));
        return out;
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
