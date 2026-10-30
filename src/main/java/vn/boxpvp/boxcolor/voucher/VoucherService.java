package vn.boxpvp.boxcolor.voucher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.config.Messages;
import vn.boxpvp.boxcolor.service.ColorService;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.style.StyleKind;
import vn.boxpvp.boxcolor.ui.Icons;

/**
 * Phiếu màu: vật phẩm bỏ vào crate, chuột phải để nhận permission của một màu. Nhận ra phiếu bằng dữ liệu ẩn
 * ({@code boxcolor:voucher} = {@code chat:cuc_quang}), không đọc tên hay lore, nên sửa messages.yml không hỏng phiếu cũ.
 */
public final class VoucherService {

    /** LuckPerms lưu quyền bất đồng bộ: trong khoảng này phiếu cùng màu coi như đã sở hữu, không trừ thêm. */
    private static final long PENDING_MILLIS = 5000L;

    private final BoxColorPlugin plugin;
    private final NamespacedKey key;
    /** "uuid:permission" → hạn chờ, chỉ dùng trên main thread. */
    private final Map<String, Long> pending = new HashMap<>();

    public VoucherService(BoxColorPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "voucher");
    }

    /** Phiếu của một màu có sẵn trong colors.yml. */
    public ItemStack create(Channel channel, ColorStyle style, int amount) {
        Messages m = plugin.messages();
        Icons icons = new Icons(plugin);
        List<String> codes = new ArrayList<>();
        if (style.kind() == StyleKind.RAINBOW) {
            codes.add(m.raw("voucher.item.rainbow"));
        } else {
            for (int rgb : style.colors()) {
                codes.add(Icons.styled(ColorStyle.hex(rgb), ColorStyle.custom(List.of(rgb)), false));
            }
        }
        String sample = channel == Channel.NAME ? m.raw("voucher.item.sample-name") : plugin.settings().chatPreview();
        // Loại phiếu: chat, name (màu tên tĩnh), decor (hiệu ứng tên động)
        String type = channel == Channel.CHAT ? "chat" : style.animated() ? "decor" : "name";
        String[] ph = {
                "type", m.raw("voucher.type." + type),
                "style", icons.styleName(style, false),
                "channel", icons.channelName(channel),
                "kind", icons.kind(style),
                "preview", Icons.styled(sample, style, false)
        };
        Map<String, List<String>> lists = new LinkedHashMap<>();
        // {code} giữ cho messages.yml cũ còn dòng này; lore mặc định không còn hiện mã màu
        lists.put("code", codes);
        lists.put("where", m.rawList("voucher.where." + type));
        ItemStack item = Icons.dyed(plugin.settings().voucherMaterial(), style.firstColor(),
                m.line("voucher.item.name", ph), m.lines("voucher.item.lore", lists, ph), true);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, channel.key() + ":" + style.id());
        meta.setMaxStackSize(64);
        item.setItemMeta(meta);
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return item;
    }

    /** {@code kênh:mã} của phiếu, {@code null} nếu không phải phiếu màu. */
    public String read(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /**
     * Dùng phiếu đang cầm: cấp permission rồi trừ 1 phiếu. Đã sở hữu màu đó, hoặc màu đã bị xoá khỏi colors.yml,
     * thì không trừ.
     */
    public void redeem(Player player, ItemStack item) {
        String tag = read(item);
        if (tag == null) return;
        String[] parts = tag.split(":", 2);
        Channel channel = parts[0].equals(Channel.NAME.key()) ? Channel.NAME : Channel.CHAT;
        ColorStyle style = parts.length < 2 ? null : plugin.styles().get(channel, parts[1]);
        Icons icons = new Icons(plugin);
        if (style == null) {
            plugin.messages().send(player, "voucher.invalid");
            return;
        }
        String name = icons.styleName(style, false);
        String permission = ColorService.COLOR_PERMISSION + channel.key() + "." + style.id();
        String pendingKey = player.getUniqueId() + ":" + permission;
        if (isPending(pendingKey) || plugin.colors().canUse(player, channel, style)) {
            plugin.messages().send(player, "voucher.already", "style", name, "channel", icons.channelName(channel));
            return;
        }
        long until = System.currentTimeMillis() + PENDING_MILLIS;
        pending.put(pendingKey, until);
        String command = plugin.settings().voucherGrantCommand()
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{permission}", permission);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        item.setAmount(item.getAmount() - 1);
        plugin.messages().send(player, "voucher.redeemed", "style", name, "channel", icons.channelName(channel),
                "command", channel == Channel.CHAT ? "/chatcolor" : "/namecolor");
        // LuckPerms lưu quyền bất đồng bộ: tính lại màu sau một chút để GUI và màu đang hiện cập nhật
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) plugin.colors().refresh(player);
        }, 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> pending.remove(pendingKey, until), PENDING_MILLIS / 50);
    }

    private boolean isPending(String pendingKey) {
        Long until = pending.get(pendingKey);
        return until != null && until > System.currentTimeMillis();
    }
}
