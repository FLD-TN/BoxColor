package vn.boxpvp.boxcolor.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.data.PlayerData;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorRenderer;
import vn.boxpvp.boxcolor.style.ColorStyle;

/**
 * Hai việc với chat: nhận câu trả lời cho ô nhập trong GUI (chặn luôn dòng đó), và tô màu nội dung chat. Màu chat
 * luôn là khung tĩnh: dòng chat đã gửi không đổi màu được nữa. Chạy async: chỉ đọc dữ liệu đã tính sẵn.
 */
public final class ChatListener implements Listener {

    private final BoxColorPlugin plugin;

    public ChatListener(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrompt(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.prompts().waiting(player.getUniqueId())) return;
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (player.isOnline()) plugin.prompts().handle(player, text);
            } catch (Exception e) {
                plugin.report("prompt", e);
            }
        });
    }

    /** LOW: trước plugin định dạng chat (EzChat...), để họ nhận nội dung đã có màu. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.settings().chatEnabled()) return;
        PlayerData data = plugin.cache().get(event.getPlayer().getUniqueId());
        if (data == null) return;
        ColorStyle style = data.shown(Channel.CHAT);
        if (style == null) return;
        Component message = event.message();
        // Người chơi tự gõ mã màu (có quyền dùng '&'): giữ màu của họ
        if (plugin.settings().keepTypedColors() && hasColor(message)) return;
        String text = PlainTextComponentSerializer.plainText().serialize(message);
        event.message(ColorRenderer.component(text, style, 0, plugin.settings().keepPlain()));
    }

    private static boolean hasColor(Component component) {
        if (component.color() != null) return true;
        for (Component child : component.children()) {
            if (hasColor(child)) return true;
        }
        return false;
    }
}
