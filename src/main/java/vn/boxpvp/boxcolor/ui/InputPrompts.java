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
import vn.boxpvp.boxcolor.nick.NickInput;

/**
 * Chờ người chơi gõ vào chat sau khi bấm "Màu tùy chỉnh" hoặc "Đổi chữ" (tên) trong GUI. Dòng chat đó bị chặn (không ai
 * khác thấy). Map đồng thời vì sự kiện chat chạy async; xử lý luôn chuyển về main thread.
 */
public final class InputPrompts {

    public enum Kind {
        /** "Màu tùy chỉnh": tên kèm mã màu từng chữ. */
        NICK,
        /** Chỉ đổi chữ của tên (trang màu động): mã màu gõ kèm bị bỏ, màu động giữ nguyên. */
        RENAME
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
                "seconds", String.valueOf(seconds),
                "min", String.valueOf(plugin.settings().nickRules().minLength()),
                "maxlength", String.valueOf(plugin.settings().nickRules().maxLength())
        };
        plugin.messages().send(player, "prompt." + kind.name().toLowerCase(Locale.ROOT).replace('_', '-'), ph);
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
        String name = p.kind() == Kind.RENAME ? NickInput.parse(text).text() : text;
        // Sai luật thì vẫn chờ, người chơi gõ lại được
        if (plugin.nickCommand().apply(player, name)) cancel(player.getUniqueId());
    }

    public void clear() {
        for (UUID uuid : List.copyOf(pending.keySet())) cancel(uuid);
    }
}
