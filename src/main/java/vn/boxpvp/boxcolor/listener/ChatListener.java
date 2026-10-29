package vn.boxpvp.boxcolor.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
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
        String text = PlainTextComponentSerializer.plainText().serialize(message);
        // Người chơi tự gõ mã màu ('&', hex, tag [color=] của InteractiveChat): giữ màu của họ
        if (plugin.settings().keepTypedColors()) {
            if (hasColor(message)) return;
            Pattern typed = plugin.settings().typedColors();
            if (typed != null && typed.matcher(text).find()) return;
        }
        event.message(ColorRenderer.component(text, style, 0, keepPlain()));
    }

    /**
     * Đoạn giữ nguyên không tô: từ khoá trong config, cộng tên thật và nick của người đang online (để InteractiveChat
     * vẫn gắn hover/click vào tên).
     */
    private Pattern keepPlain() {
        Pattern base = plugin.settings().keepPlain();
        if (!plugin.settings().keepPlayerNames()) return base;
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
            PlayerData data = plugin.cache().get(p.getUniqueId());
            if (data != null && data.shownNick() != null) names.add(data.shownNick());
        }
        if (names.isEmpty()) return base;
        // Tên dài trước để "Steve123" không bị khớp thành "Steve"
        names.sort(Comparator.comparingInt(String::length).reversed());
        StringBuilder regex = new StringBuilder(base != null ? "(?:" + base.pattern() + ")|" : "");
        regex.append("(?<![\\p{L}\\p{N}_])(?:");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) regex.append('|');
            regex.append(Pattern.quote(names.get(i)));
        }
        regex.append(")(?![\\p{L}\\p{N}_])");
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static boolean hasColor(Component component) {
        if (component.color() != null) return true;
        for (Component child : component.children()) {
            if (hasColor(child)) return true;
        }
        return false;
    }
}
