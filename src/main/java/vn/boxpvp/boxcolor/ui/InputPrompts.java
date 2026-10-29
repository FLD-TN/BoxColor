package vn.boxpvp.boxcolor.ui;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.style.Hex;

/**
 * Chờ người chơi gõ vào chat sau khi bấm "Tự chỉnh mã màu" hoặc "Đổi nick" trong GUI. Dòng chat đó bị chặn (không ai
 * khác thấy). Map đồng thời vì sự kiện chat chạy async; xử lý luôn chuyển về main thread.
 */
public final class InputPrompts {

    public enum Kind {
        CHAT_COLOR,
        NAME_COLOR,
        NICK
    }

    private record Pending(Kind kind, BukkitTask timeout) {
    }

    private static final List<String> CANCEL_WORDS = List.of("huy", "hủy", "cancel", "thoat", "thoát");

    private final BoxColorPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public InputPrompts(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(Player player, Kind kind) {
        cancel(player.getUniqueId());
        int seconds = plugin.settings().inputTimeoutSeconds();
        UUID uuid = player.getUniqueId();
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(uuid) == null) return;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.messages().send(p, "prompt.expired");
        }, seconds * 20L);
        pending.put(uuid, new Pending(kind, timeout));
        String[] ph = {
                "max", String.valueOf(plugin.settings().customMaxColors()),
                "seconds", String.valueOf(seconds),
                "min", String.valueOf(plugin.settings().nickRules().minLength()),
                "maxlength", String.valueOf(plugin.settings().nickRules().maxLength())
        };
        plugin.messages().send(player, kind == Kind.NICK ? "prompt.nick" : "prompt.color", ph);
        plugin.messages().send(player, "prompt.cancel-hint", ph);
    }

    public boolean waiting(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void cancel(UUID uuid) {
        Pending p = pending.remove(uuid);
        if (p != null) p.timeout().cancel();
    }

    /** Dòng chat người chơi vừa gõ, gọi trên main thread. */
    public void handle(Player player, String input) {
        Pending p = pending.get(player.getUniqueId());
        if (p == null) return;
        String text = input.trim();
        if (CANCEL_WORDS.contains(text.toLowerCase(Locale.ROOT))) {
            cancel(player.getUniqueId());
            plugin.messages().send(player, "prompt.cancelled");
            return;
        }
        if (p.kind() == Kind.NICK) {
            // Sai luật thì vẫn chờ, người chơi gõ lại được
            if (plugin.nickCommand().apply(player, text)) cancel(player.getUniqueId());
            return;
        }
        Channel channel = p.kind() == Kind.CHAT_COLOR ? Channel.CHAT : Channel.NAME;
        ColorStyle style = parseCustom(player, text);
        if (style == null) return;
        cancel(player.getUniqueId());
        plugin.menus().open(player, new CustomConfirmMenu(plugin, player, channel, style));
    }

    /** Đọc danh sách mã; sai thì báo lỗi và trả {@code null}. */
    public ColorStyle parseCustom(Player player, String text) {
        List<Integer> rgb = Hex.parseList(text);
        if (rgb == null) {
            plugin.messages().send(player, "color.custom-invalid");
            return null;
        }
        int max = plugin.settings().customMaxColors();
        if (rgb.size() > max) {
            plugin.messages().send(player, "color.custom-too-many", "max", String.valueOf(max));
            return null;
        }
        return ColorStyle.custom(rgb);
    }

    public void clear() {
        for (UUID uuid : List.copyOf(pending.keySet())) cancel(uuid);
    }
}
