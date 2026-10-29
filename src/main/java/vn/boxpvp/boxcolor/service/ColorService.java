package vn.boxpvp.boxcolor.service;

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
        for (Channel channel : Channel.values()) {
            ColorStyle style = PlayerData.resolve(data.selection(channel), channel, plugin.styles());
            data.shown(channel, style != null && canUse(player, level, channel, style) ? style : null);
        }
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

    public static Feature customFeature(Channel channel) {
        return channel == Channel.CHAT ? Feature.CUSTOM_CHAT : Feature.CUSTOM_NAME;
    }

    /** Màu tên đang hiện; chưa chọn thì màu rank; không có màu rank thì {@code default-name-color}. */
    public ColorStyle nameStyle(PlayerData data) {
        ColorStyle style = data.shown(Channel.NAME);
        if (style != null) return style;
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

    /** Tên đã tô màu cho placeholder. {@code animated = false}: khung đầu, dùng cho chat. */
    public String name(PlayerData data, boolean animated, HexFormat format) {
        ColorStyle style = nameStyle(data);
        double phase = animated ? ColorRenderer.phase(style, System.currentTimeMillis()) : 0;
        return ColorRenderer.legacy(data.displayText(), style, phase, format);
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
