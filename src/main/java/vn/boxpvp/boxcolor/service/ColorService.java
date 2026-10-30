package vn.boxpvp.boxcolor.service;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.config.Settings;
import vn.boxpvp.boxcolor.data.PlayerData;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorRenderer;
import vn.boxpvp.boxcolor.style.ColorRenderer.HexFormat;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.style.Effect;
import vn.boxpvp.boxcolor.style.Motion;
import vn.boxpvp.boxcolor.tier.Feature;

/**
 * Quyền dùng màu và tên hiển thị. Kiểm tra quyền chỉ chạy trên main thread (khi vào server, khi chọn màu và định kỳ);
 * kết quả ghi vào {@link PlayerData} để placeholder ở luồng khác chỉ việc đọc.
 */
public final class ColorService {

    /** Permission lẻ cho một màu, ví dụ quà event: {@code boxcolor.color.chat.hong_pastel}. */
    public static final String COLOR_PERMISSION = "boxcolor.color.";
    /** Permission lẻ cho một tính năng: {@code boxcolor.feature.nick}. */
    public static final String FEATURE_PERMISSION = "boxcolor.feature.";

    private final BoxColorPlugin plugin;
    private BukkitTask checkTask;

    public ColorService(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        long ticks = plugin.settings().accessCheckSeconds() * 20L;
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, ticks, ticks);
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    /** Tính lại tier, màu và nick đang hiện của một người; cập nhật display name. */
    public void refresh(Player player) {
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null) return;
        data.realName(player.getName());
        int level = plugin.tiers().levelOf(player);
        data.tierLevel(level);
        // Bản cũ lưu màu động chung với màu tên: chuyển sang hiệu ứng động (chỉ TAB và nametag)
        ColorStyle legacy = PlayerData.resolve(data.selection(Channel.NAME), Channel.NAME, plugin.styles());
        if (legacy != null && legacy.animated()) {
            if (data.decor() == null) data.decor(legacy.id());
            data.selection(Channel.NAME, null);
            plugin.cache().save(data);
        }
        for (Channel channel : Channel.values()) {
            ColorStyle style = PlayerData.resolve(data.selection(channel), channel, plugin.styles());
            data.shown(channel, style != null && canUse(player, level, channel, style) ? style : null);
        }
        ColorStyle decor = plugin.styles().get(Channel.NAME, data.decor());
        data.shownDecor(decor != null && decor.animated() && canUse(player, level, Channel.NAME, decor) ? decor : null);
        String nick = data.nick();
        data.shownNick(nick != null && hasFeature(player, level, Feature.NICK) ? nick : null);
        data.rankName(rankNameStyle(player));
        applyDisplayName(player, data);
    }

    /**
     * Lưu lựa chọn ({@code null} = bỏ màu), tính lại phần đang hiện và báo cho người chơi.
     * Người gọi đã kiểm tra quyền.
     */
    public void select(Player player, Channel channel, ColorStyle style) {
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null) return;
        data.selection(channel, style == null ? null : style.isCustom() ? style.encodeCustom() : style.id());
        plugin.cache().save(data);
        refresh(player);
        String channelName = plugin.messages().raw("menu.channel." + channel.key());
        if (style == null) {
            plugin.messages().send(player, "color.reset", "channel", channelName);
            return;
        }
        String display = style.isCustom() ? plugin.messages().raw("menu.custom-display") : style.display();
        plugin.messages().send(player, "color.selected", "channel", channelName,
                "style", ColorRenderer.legacy(display, style, 0, HexFormat.AMPERSAND));
    }

    /**
     * Lưu hiệu ứng tên động cho TAB và nametag ({@code null} = bỏ). Chỉ tô màu lên tên thật, không đổi chữ.
     * Người gọi đã kiểm tra quyền.
     */
    public void selectDecor(Player player, ColorStyle style) {
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null) return;
        data.decor(style == null ? null : style.id());
        plugin.cache().save(data);
        refresh(player);
        if (style == null) {
            plugin.messages().send(player, "color.decor-reset");
            return;
        }
        plugin.messages().send(player, "color.decor-selected",
                "style", ColorRenderer.legacy(style.display(), style, 0, HexFormat.AMPERSAND));
    }

    /** Lưu kiểu chuyển động của hiệu ứng tên động. Không cần quyền: áp lên hiệu ứng người chơi đã sở hữu. */
    public void selectMotion(Player player, Motion motion) {
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null) return;
        data.motion(motion.key());
        plugin.cache().save(data);
        plugin.messages().send(player, "color.motion-selected",
                "motion", plugin.messages().raw("menu.motion." + motion.key() + ".name"));
    }

    /** Kiểu chuyển động người chơi đã chọn, chưa chọn thì {@code animation.default-motion}. */
    public Motion motionOf(PlayerData data) {
        Motion motion = Motion.parse(data.motion());
        return motion != null ? motion : plugin.settings().defaultMotion();
    }

    /** Cách chạy thực tế của hiệu ứng {@code style} với kiểu chuyển động của người chơi. */
    public Effect effectOf(PlayerData data, ColorStyle style) {
        return motionOf(data).apply(style.effect());
    }

    /** Thời điểm hiện tại làm tròn theo khung hình: trong một khung chuỗi trả về không đổi, TAB/UNT không gửi lại. */
    public long frameNow() {
        long now = System.currentTimeMillis();
        return now - now % plugin.settings().animationFrameMillis();
    }

    public boolean canUse(Player player, Channel channel, ColorStyle style) {
        return canUse(player, plugin.tiers().levelOf(player), channel, style);
    }

    private boolean canUse(Player player, int level, Channel channel, ColorStyle style) {
        if (style.isCustom()) return hasFeature(player, level, customFeature(channel));
        return plugin.tiers().allows(level, channel, style.id())
                || player.hasPermission(COLOR_PERMISSION + channel.key() + "." + style.id());
    }

    public boolean hasFeature(Player player, Feature feature) {
        return hasFeature(player, plugin.tiers().levelOf(player), feature);
    }

    private boolean hasFeature(Player player, int level, Feature feature) {
        return plugin.tiers().hasFeature(level, feature) || player.hasPermission(FEATURE_PERMISSION + feature.key());
    }

    /** Ảnh chụp quyền của người chơi lúc này, cho GUI vẽ lại liên tục. */
    public Access access(Player player) {
        return new Access(player, plugin.tiers().levelOf(player));
    }

    /** Tier tính một lần, kết quả từng màu nhớ lại: vẽ lại GUI không kiểm tra quyền lặp lại. */
    public final class Access {

        private final Player player;
        private final int level;
        private final Map<String, Boolean> colors = new HashMap<>();

        private Access(Player player, int level) {
            this.player = player;
            this.level = level;
        }

        public boolean canUse(Channel channel, ColorStyle style) {
            return colors.computeIfAbsent(channel.key() + ":" + style.id(),
                    k -> ColorService.this.canUse(player, level, channel, style));
        }

        public boolean hasFeature(Feature feature) {
            return ColorService.this.hasFeature(player, level, feature);
        }
    }

    /** Màu chat tự chỉnh: {@code custom-chat}. Màu tên tự chỉnh là "Màu tùy chỉnh": {@code nick}. */
    public static Feature customFeature(Channel channel) {
        return channel == Channel.CHAT ? Feature.CUSTOM_CHAT : Feature.NICK;
    }

    /** Màu tên đang hiện; chưa chọn thì màu rank; không có màu rank thì {@code default-name-color}. */
    public ColorStyle nameStyle(PlayerData data) {
        ColorStyle style = data.shown(Channel.NAME);
        if (style != null) return style;
        ColorStyle rank = data.rankName();
        return rank != null ? rank : plugin.settings().defaultName();
    }

    /** Màu rank, không có thì {@code default-name-color} (màu tên ở TAB khi không có hiệu ứng động). */
    public ColorStyle rankOrDefault(PlayerData data) {
        ColorStyle rank = data.rankName();
        return rank != null ? rank : plugin.settings().defaultName();
    }

    /** Group đầu tiên trong rank-name-colors mà người chơi có. */
    private ColorStyle rankNameStyle(Player player) {
        for (Settings.RankColor rank : plugin.settings().rankNameColors()) {
            if (player.hasPermission(rank.permission())) return rank.style();
        }
        return null;
    }

    /**
     * Tên cho chat và scoreboard: nick (nếu có) với màu tên tĩnh, màu tùy chỉnh hoặc màu rank. Luôn đứng yên.
     */
    public String name(PlayerData data, HexFormat format) {
        return ColorRenderer.legacy(data.displayText(), nameStyle(data), 0, format);
    }

    /**
     * Tên cho TAB: luôn là tên thật (chống giả danh), có hiệu ứng động thì chạy màu, không thì màu rank.
     */
    public String tabName(PlayerData data, HexFormat format) {
        ColorStyle decor = data.shownDecor();
        if (decor != null) return animated(data, decor, format);
        return ColorRenderer.legacy(data.realName(), rankOrDefault(data), 0, format);
    }

    /**
     * Tên cho nametag trên đầu: luôn là tên thật, có hiệu ứng động thì chạy màu, không thì
     * {@code nametag-default-color} (mặc định vàng &e).
     */
    public String nametagName(PlayerData data, HexFormat format) {
        ColorStyle decor = data.shownDecor();
        if (decor != null) return animated(data, decor, format);
        return ColorRenderer.legacy(data.realName(), plugin.settings().nametagDefault(), 0, format);
    }

    /** Tên thật chạy hiệu ứng theo kiểu chuyển động của người chơi; tắt animation thì đứng yên. */
    private String animated(PlayerData data, ColorStyle style, HexFormat format) {
        if (!plugin.settings().animationEnabled()) return ColorRenderer.legacy(data.realName(), style, 0, format);
        return ColorRenderer.legacy(data.realName(), style, effectOf(data, style), frameNow(), format);
    }

    /** Tên thật tô màu mặc định, cho người chưa nạp dữ liệu hoặc đang offline. */
    public String fallbackName(String realName, HexFormat format) {
        return ColorRenderer.legacy(realName, plugin.settings().defaultName(), 0, format);
    }

    private void applyDisplayName(Player player, PlayerData data) {
        if (!plugin.settings().setDisplayName()) return;
        player.displayName(ColorRenderer.component(data.displayText(), nameStyle(data), 0, null));
    }

    /** Đặt lại display name gốc (khi tắt plugin). */
    public void resetDisplayNames() {
        if (!plugin.settings().setDisplayName()) return;
        for (Player player : Bukkit.getOnlinePlayers()) player.displayName(null);
    }
}
