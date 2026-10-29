package vn.boxpvp.boxcolor.data;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.nick.NickRules;
import vn.boxpvp.boxcolor.style.Channel;

/**
 * Dữ liệu của người đang online. Đọc/ghi database trên MỘT luồng riêng theo đúng thứ tự gửi, nên lần lưu sau
 * luôn đè lần trước. Map là ConcurrentHashMap vì placeholder có thể đọc từ luồng khác.
 */
public final class PlayerCache {

    private final BoxColorPlugin plugin;
    private final Storage storage;
    private final Map<UUID, PlayerData> online = new ConcurrentHashMap<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BoxColor-IO");
        t.setDaemon(true);
        return t;
    });

    public PlayerCache(BoxColorPlugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public PlayerData get(UUID uuid) {
        return uuid == null ? null : online.get(uuid);
    }

    public Collection<PlayerData> all() {
        return online.values();
    }

    /** Nạp dữ liệu ở luồng IO rồi gọi {@code done} trên main thread (bỏ qua nếu người chơi đã thoát). */
    public void load(Player player, Consumer<PlayerData> done) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        io.execute(() -> {
            Storage.Row row;
            try {
                row = storage.load(uuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, plugin.messages().log("log.load-failed", "player", name), e);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return;
                PlayerData data = new PlayerData(uuid, p.getName());
                if (row != null) {
                    data.selection(Channel.CHAT, row.chat());
                    data.selection(Channel.NAME, row.name());
                    data.decor(row.decor());
                    data.nick(row.nick());
                    data.nickChangedAt(row.nickChangedAt());
                }
                online.put(uuid, data);
                done.accept(data);
            });
        });
    }

    public void unload(UUID uuid) {
        online.remove(uuid);
    }

    /** Chụp dữ liệu trên main thread rồi ghi ở luồng IO. */
    public void save(PlayerData data) {
        String nick = data.nick();
        Storage.Row row = new Storage.Row(data.uuid(), data.selection(Channel.CHAT), data.selection(Channel.NAME),
                data.decor(),
                nick, nick == null ? null : NickRules.key(nick), data.nickChangedAt());
        io.execute(() -> {
            try {
                storage.save(row);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        plugin.messages().log("log.save-failed", "player", data.realName()), e);
            }
        });
    }

    /** Xoá nick của người đang offline. */
    public void clearNickOffline(UUID uuid) {
        io.execute(() -> {
            try {
                storage.clearNick(uuid);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, plugin.messages().log("log.save-failed", "player", uuid.toString()), e);
            }
        });
    }

    /** Đọc nick của người đang offline ở luồng IO, trả kết quả trên main thread. */
    public void nickOfOffline(UUID uuid, Consumer<String> done) {
        io.execute(() -> {
            String nick;
            try {
                nick = storage.nickOf(uuid);
            } catch (SQLException e) {
                nick = null;
            }
            String result = nick;
            Bukkit.getScheduler().runTask(plugin, () -> done.accept(result));
        });
    }

    /** Lúc tắt plugin: chờ các lần ghi còn lại xong (tối đa 10 giây). */
    public void shutdown() {
        io.shutdown();
        try {
            if (!io.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning(plugin.messages().log("log.shutdown-timeout"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
