package vn.boxpvp.boxcolor.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import vn.boxpvp.boxcolor.nick.NickRules;
import vn.boxpvp.boxcolor.style.ColorRenderer.HexFormat;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.style.Hex;

/**
 * config.yml. Mọi giá trị đều có mặc định trong code: file cũ thiếu khoá vẫn chạy, cập nhật chỉ cần thay jar.
 */
public final class Settings {

    /** Màu tên mặc định của một rank: người có {@code permission} mà chưa chọn màu thì dùng {@code style}. */
    public record RankColor(String permission, ColorStyle style) {
    }

    private final HexFormat placeholderFormat;
    private final ColorStyle defaultName;
    private final List<RankColor> rankNameColors;
    private final boolean setDisplayName;
    private final int accessCheckSeconds;
    private final boolean chatEnabled;
    private final boolean keepTypedColors;
    private final Pattern keepPlain;
    private final NickRules nickRules;
    private final long nickCooldownMillis;
    private final boolean checkRealNames;
    private final int customMaxColors;
    private final int inputTimeoutSeconds;
    private final String chatPreview;
    private final Material border;
    private final Material filler;
    private final Material defaultIcon;

    private Settings(FileConfiguration c, Consumer<String> warn) {
        placeholderFormat = parseFormat(c.getString("placeholder-format", "ampersand"), warn);
        int rgb = Hex.parse(c.getString("default-name-color", "#FFFFFF"));
        if (rgb < 0) {
            warn.accept("default-name-color sai, dùng #FFFFFF");
            rgb = 0xFFFFFF;
        }
        defaultName = ColorStyle.custom(List.of(rgb));
        rankNameColors = rankColors(c, warn);
        setDisplayName = c.getBoolean("set-display-name", true);
        accessCheckSeconds = Math.max(5, c.getInt("access-check-seconds", 30));

        chatEnabled = c.getBoolean("chat.enabled", true);
        keepTypedColors = c.getBoolean("chat.keep-typed-colors", true);
        List<String> plain = c.isList("chat.keep-plain")
                ? c.getStringList("chat.keep-plain")
                : List.of("[item]", "[i]", "[inv]", "[hand]", "[ec]");
        List<String> quoted = new ArrayList<>();
        for (String s : plain) {
            if (!s.isEmpty()) quoted.add(Pattern.quote(s));
        }
        keepPlain = quoted.isEmpty() ? null : Pattern.compile(String.join("|", quoted), Pattern.CASE_INSENSITIVE);
        chatPreview = c.getString("chat.preview-text", "Xin chào mọi người");

        List<String> blocked = c.isList("nick.blocked-words")
                ? c.getStringList("nick.blocked-words")
                : List.of("admin", "owner", "staff", "moderator", "helper", "console");
        nickRules = new NickRules(c.getInt("nick.min-length", 3), c.getInt("nick.max-length", 16),
                c.getBoolean("nick.allow-spaces", true), c.getBoolean("nick.allow-vietnamese", true), blocked);
        nickCooldownMillis = Math.max(0L, c.getLong("nick.cooldown-seconds", 3600L)) * 1000L;
        checkRealNames = c.getBoolean("nick.check-real-names", true);

        customMaxColors = Math.max(1, Math.min(10, c.getInt("custom.max-colors", 5)));
        inputTimeoutSeconds = Math.max(10, c.getInt("custom.input-timeout-seconds", 45));

        border = material(c.getString("gui.border", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE, warn);
        filler = material(c.getString("gui.filler", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE, warn);
        defaultIcon = material(c.getString("gui.default-color-icon", "LEATHER_CHESTPLATE"), Material.LEATHER_CHESTPLATE, warn);
    }

    /** Theo thứ tự trong file; khoá là tên group LuckPerms (kiểm tra bằng permission {@code group.<tên>}). */
    static List<RankColor> rankColors(Configuration c, Consumer<String> warn) {
        // File cũ chưa có khoá này: lấy bảng trong jar (getConfigurationSection chỉ trả mục rỗng)
        ConfigurationSection section = c.contains("rank-name-colors", true) || c.getDefaults() == null
                ? c.getConfigurationSection("rank-name-colors")
                : c.getDefaults().getConfigurationSection("rank-name-colors");
        if (section == null) return List.of();
        List<RankColor> out = new ArrayList<>();
        for (String group : section.getKeys(false)) {
            List<Integer> rgb = Hex.parseList(String.join(" ", section.getStringList(group)));
            if (rgb == null) {
                warn.accept("rank-name-colors." + group + ": mã màu sai, ví dụ ['#FF7C12', '#F3C890']");
                continue;
            }
            out.add(new RankColor("group." + group.toLowerCase(Locale.ROOT), ColorStyle.custom(rgb)));
        }
        return List.copyOf(out);
    }

    public static Settings load(FileConfiguration config, Consumer<String> warn) {
        return new Settings(config, warn);
    }

    private static HexFormat parseFormat(String s, Consumer<String> warn) {
        switch (s.toLowerCase(Locale.ROOT).trim()) {
            case "ampersand":
                return HexFormat.AMPERSAND;
            case "section":
                return HexFormat.SECTION;
            case "minimessage":
                return HexFormat.MINIMESSAGE;
            default:
                warn.accept("placeholder-format phải là ampersand, section hoặc minimessage; dùng ampersand");
                return HexFormat.AMPERSAND;
        }
    }

    private static Material material(String name, Material fallback, Consumer<String> warn) {
        Material m = name == null ? null : Material.matchMaterial(name);
        if (m == null || !m.isItem()) {
            warn.accept("Không có item '" + name + "', dùng " + fallback.name());
            return fallback;
        }
        return m;
    }

    public HexFormat placeholderFormat() {
        return placeholderFormat;
    }

    /** Màu tên khi người chơi chưa chọn màu nào và không có màu rank. */
    public ColorStyle defaultName() {
        return defaultName;
    }

    public List<RankColor> rankNameColors() {
        return rankNameColors;
    }

    public boolean setDisplayName() {
        return setDisplayName;
    }

    public int accessCheckSeconds() {
        return accessCheckSeconds;
    }

    public boolean chatEnabled() {
        return chatEnabled;
    }

    /** Người chơi tự gõ mã màu trong chat thì giữ màu của họ, không tô màu chat. */
    public boolean keepTypedColors() {
        return keepTypedColors;
    }

    /** Đoạn chat giữ nguyên không tô màu, {@code null} nếu không có. */
    public Pattern keepPlain() {
        return keepPlain;
    }

    public String chatPreview() {
        return chatPreview;
    }

    public NickRules nickRules() {
        return nickRules;
    }

    public long nickCooldownMillis() {
        return nickCooldownMillis;
    }

    public boolean checkRealNames() {
        return checkRealNames;
    }

    public int customMaxColors() {
        return customMaxColors;
    }

    public int inputTimeoutSeconds() {
        return inputTimeoutSeconds;
    }

    public Material border() {
        return border;
    }

    public Material filler() {
        return filler;
    }

    public Material defaultIcon() {
        return defaultIcon;
    }
}
