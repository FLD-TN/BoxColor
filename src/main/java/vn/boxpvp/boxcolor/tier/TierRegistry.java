package vn.boxpvp.boxcolor.tier;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permissible;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.StyleRegistry;

/**
 * Các tier trong tiers.yml. Người chơi có {@code boxcolor.tier.<n>} cao nhất là tier của họ, và dùng được mọi màu,
 * tính năng của tier đó cùng các tier thấp hơn. Logic chỉ làm việc với số tier để test được không cần server.
 */
public final class TierRegistry {

    public static final String TIER_PERMISSION = "boxcolor.tier.";
    /** Toàn quyền: mọi màu, mọi tính năng. */
    public static final String ADMIN_PERMISSION = "boxcolor.admin";
    /** Tier của người có {@link #ADMIN_PERMISSION}. */
    public static final int ALL = Integer.MAX_VALUE;

    private final NavigableMap<Integer, Tier> tiers;

    private TierRegistry(NavigableMap<Integer, Tier> tiers) {
        this.tiers = tiers;
    }

    public static TierRegistry load(YamlConfiguration yml, StyleRegistry styles, Consumer<String> warn) {
        NavigableMap<Integer, Tier> map = new TreeMap<>();
        ConfigurationSection root = yml.getConfigurationSection("tiers");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                int level;
                try {
                    level = Integer.parseInt(key.trim());
                } catch (NumberFormatException e) {
                    warn.accept("tiers." + key + ": tên tier phải là số (1, 2, 3...)");
                    continue;
                }
                if (level < 1) {
                    warn.accept("tiers." + key + ": số tier phải từ 1 trở lên");
                    continue;
                }
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) continue;
                Set<Feature> features = EnumSet.noneOf(Feature.class);
                for (String f : s.getStringList("features")) {
                    Feature feature = Feature.parse(f);
                    if (feature == null) {
                        warn.accept("tiers." + key + ".features: không có tính năng '" + f
                                + "' (nick, motion)");
                    } else {
                        features.add(feature);
                    }
                }
                map.put(level, new Tier(level, s.getString("name", "Tier " + level),
                        ids(s.getStringList("chat-colors"), styles, Channel.CHAT, "tiers." + key, warn),
                        ids(s.getStringList("name-colors"), styles, Channel.NAME, "tiers." + key, warn),
                        features));
            }
        }
        return new TierRegistry(Collections.unmodifiableNavigableMap(map));
    }

    private static Set<String> ids(List<String> raw, StyleRegistry styles, Channel channel, String path,
            Consumer<String> warn) {
        Set<String> out = new HashSet<>();
        for (String id : raw) {
            String key = id.toLowerCase(Locale.ROOT).trim();
            if (styles != null && styles.get(channel, key) == null) {
                warn.accept(path + "." + channel.key() + "-colors: không có màu '" + id + "' trong colors.yml");
                continue;
            }
            out.add(key);
        }
        return out;
    }

    /** Tier cao nhất người này có quyền; 0 nếu không có tier nào. Gọi trên main thread. */
    public int levelOf(Permissible who) {
        if (who.hasPermission(ADMIN_PERMISSION)) return ALL;
        for (int level : tiers.descendingKeySet()) {
            if (who.hasPermission(TIER_PERMISSION + level)) return level;
        }
        return 0;
    }

    /** Tier thấp nhất có liệt kê màu này, {@code null} nếu không tier nào có (chỉ mở bằng permission riêng). */
    public Tier unlockTier(Channel channel, String styleId) {
        for (Tier tier : tiers.values()) {
            if (tier.colors(channel).contains(styleId)) return tier;
        }
        return null;
    }

    public Tier featureTier(Feature feature) {
        for (Tier tier : tiers.values()) {
            if (tier.features().contains(feature)) return tier;
        }
        return feature.defaultTier() > 0 ? tiers.get(feature.defaultTier()) : null;
    }

    public boolean allows(int level, Channel channel, String styleId) {
        if (level == ALL) return true;
        Tier tier = unlockTier(channel, styleId);
        return tier != null && tier.level() <= level;
    }

    public boolean hasFeature(int level, Feature feature) {
        if (level == ALL) return true;
        Tier tier = featureTier(feature);
        return tier != null && tier.level() <= level;
    }

    /** Tier đang hưởng (cao nhất mà {@code <= level}), {@code null} nếu chưa có. */
    public Tier current(int level) {
        Map.Entry<Integer, Tier> e = tiers.floorEntry(level);
        return e == null ? null : e.getValue();
    }

    public List<Tier> all() {
        return List.copyOf(tiers.values());
    }
}
