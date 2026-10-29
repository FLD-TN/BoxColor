package vn.boxpvp.boxcolor.nick;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.data.PlayerData;

/**
 * Chống trùng nick và giả mạo: so khoá ({@link NickRules#key}) với nick của mọi người (kể cả offline, lấy từ
 * database) và với tên thật của mọi người từng vào server. Chỉ dùng trên main thread.
 */
public final class NickService {

    private final BoxColorPlugin plugin;
    /** khoá nick → chủ nick */
    private final Map<String, UUID> nickKeys = new HashMap<>();
    /** khoá tên thật → người chơi */
    private final Map<String, UUID> realKeys = new HashMap<>();

    public NickService(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    /** Lúc bật plugin: nick trong database và tên thật của mọi người server đã biết. */
    public void init(Map<String, UUID> storedNicks) {
        nickKeys.clear();
        nickKeys.putAll(storedNicks);
        realKeys.clear();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            String name = player.getName();
            if (name != null) realKeys.putIfAbsent(NickRules.key(name), player.getUniqueId());
        }
    }

    /**
     * Người chơi vào server: ghi nhận tên thật. Nếu đã có người khác lấy nick trùng tên này (đặt trước khi người
     * này vào lần đầu), nick đó bị xoá để không ai đội tên người thật.
     */
    public void onJoin(Player player) {
        String key = NickRules.key(player.getName());
        realKeys.put(key, player.getUniqueId());
        if (!plugin.settings().checkRealNames()) return;
        UUID owner = nickKeys.get(key);
        if (owner == null || owner.equals(player.getUniqueId())) return;

        nickKeys.remove(key);
        PlayerData data = plugin.cache().get(owner);
        if (data != null) {
            String old = data.nick();
            data.nick(null);
            plugin.cache().save(data);
            Player ownerPlayer = Bukkit.getPlayer(owner);
            if (ownerPlayer != null) {
                plugin.colors().refresh(ownerPlayer);
                plugin.messages().send(ownerPlayer, "nick.removed-conflict", "nick", old, "player", player.getName());
            }
        } else {
            plugin.cache().clearNickOffline(owner);
        }
        plugin.getLogger().info(plugin.messages().log("log.nick-conflict", "player", player.getName(),
                "owner", owner.toString()));
    }

    /** Người khác (không phải {@code self}) đang dùng nick hoặc tên thật trùng khoá; {@code null} nếu còn trống. */
    public UUID takenBy(UUID self, String nick) {
        String key = NickRules.key(nick);
        UUID owner = nickKeys.get(key);
        if (owner != null && !owner.equals(self)) return owner;
        if (plugin.settings().checkRealNames()) {
            UUID real = realKeys.get(key);
            if (real != null && !real.equals(self)) return real;
        }
        return null;
    }

    /** Đặt nick ({@code null} là xoá) và cập nhật chỉ mục. Không kiểm tra luật, không lưu database. */
    public void set(PlayerData data, String nick) {
        String old = data.nick();
        if (old != null) nickKeys.remove(NickRules.key(old), data.uuid());
        data.nick(nick);
        if (nick != null) nickKeys.put(NickRules.key(nick), data.uuid());
    }

    /** Admin xoá nick của người đang offline. */
    public void forgetOffline(UUID uuid) {
        nickKeys.values().removeIf(uuid::equals);
    }

    /** Chủ của nick (so theo khoá, không cần gõ đúng dấu/hoa thường), {@code null} nếu không có. */
    public UUID ownerOf(String nick) {
        return nickKeys.get(NickRules.key(nick));
    }
}
