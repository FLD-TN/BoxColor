package vn.boxpvp.boxcolor.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.service.ColorService;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.tier.Feature;
import vn.boxpvp.boxcolor.ui.ColorMenu;
import vn.boxpvp.boxcolor.ui.CustomConfirmMenu;
import vn.boxpvp.boxcolor.ui.Icons;
import vn.boxpvp.boxcolor.ui.MainMenu;
import vn.boxpvp.boxcolor.ui.NameTypeMenu;

/**
 * {@code /chatcolor} mở trang màu chat, {@code /namecolor} mở trang chọn màu tên (tĩnh/động), {@code /dasac} mở trang
 * chính ({@code channel = null}). {@code reset} bỏ màu, {@code custom <#mã...>} tự chỉnh.
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
            case "custom" -> custom(player, label, Arrays.copyOfRange(args, 1, args.length));
            default -> plugin.messages().send(player, "command.usage-color", "label", label,
                    "max", String.valueOf(plugin.settings().customMaxColors()));
        }
        return true;
    }

    private void custom(Player player, String label, String[] codes) {
        // Màu tên tự chỉnh nằm ở "Màu tùy chỉnh" (/nick), chỉ màu chat còn nhập theo danh sách mã
        if (channel == Channel.NAME) {
            plugin.messages().send(player, "command.name-custom-moved");
            return;
        }
        Feature feature = ColorService.customFeature(channel);
        if (!plugin.colors().hasFeature(player, feature)) {
            Icons icons = new Icons(plugin);
            plugin.messages().send(player, "color.custom-locked",
                    "channel", icons.channelName(channel), "tier", icons.featureUnlock(feature));
            return;
        }
        if (codes.length == 0) {
            plugin.messages().send(player, "command.usage-color", "label", label,
                    "max", String.valueOf(plugin.settings().customMaxColors()));
            return;
        }
        ColorStyle style = plugin.prompts().parseCustom(player, String.join(" ", codes));
        if (style != null) plugin.menus().open(player, new CustomConfirmMenu(plugin, player, channel, style));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (channel == null) return List.of();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            return List.of("reset", "custom").stream().filter(s -> s.startsWith(start)).toList();
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("custom")) return List.of("#");
        return List.of();
    }
}
