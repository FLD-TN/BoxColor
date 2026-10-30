package vn.boxpvp.boxcolor.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.ui.ColorMenu;
import vn.boxpvp.boxcolor.ui.MainMenu;
import vn.boxpvp.boxcolor.ui.NameTypeMenu;

/**
 * {@code /chatcolor} mở trang màu chat, {@code /namecolor} mở trang chọn màu tên (tĩnh/động), {@code /dasac} mở trang
 * chính ({@code channel = null}). {@code reset} bỏ màu. Màu chat chỉ chọn trong danh sách (rank tặng, phiếu); tên
 * tự chỉnh bằng {@code /nick}.
 */
public final class ColorCommand implements TabExecutor {

    private final BoxColorPlugin plugin;
    /** {@code null}: lệnh {@code /dasac}, chỉ mở trang chính. */
    private final Channel channel;

    public ColorCommand(BoxColorPlugin plugin, Channel channel) {
        this.plugin = plugin;
        this.channel = channel;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "command.player-only");
            return true;
        }
        if (!player.hasPermission("boxcolor.use")) {
            plugin.messages().send(player, "command.no-permission");
            return true;
        }
        if (plugin.cache().get(player.getUniqueId()) == null) {
            plugin.messages().send(player, "command.not-loaded");
            return true;
        }
        if (channel == null) {
            plugin.menus().open(player, new MainMenu(plugin, player));
            return true;
        }
        if (args.length == 0) {
            plugin.menus().open(player, channel == Channel.CHAT
                    ? new ColorMenu(plugin, player, ColorMenu.Section.CHAT, 0) : new NameTypeMenu(plugin, player));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reset", "off" -> plugin.colors().select(player, channel, null);
            default -> plugin.messages().send(player, "command.usage-color", "label", label);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (channel == null) return List.of();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            return "reset".startsWith(start) ? List.of("reset") : List.of();
        }
        return List.of();
    }
}
