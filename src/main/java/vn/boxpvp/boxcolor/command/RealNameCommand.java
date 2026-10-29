package vn.boxpvp.boxcolor.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.data.PlayerData;

/** {@code /realname <nick>}: nick này là của ai (so theo khoá, không cần gõ đúng dấu). */
public final class RealNameCommand implements TabExecutor {

    private final BoxColorPlugin plugin;

    public RealNameCommand(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("boxcolor.realname")) {
            plugin.messages().send(sender, "command.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(sender, "nick.realname-usage", "label", label);
            return true;
        }
        String input = String.join(" ", args);
        UUID owner = plugin.nicks().ownerOf(input);
        if (owner == null) {
            plugin.messages().send(sender, "nick.realname-none", "nick", input);
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
        String name = player.getName() != null ? player.getName() : owner.toString();
        PlayerData data = plugin.cache().get(owner);
        if (data != null) {
            plugin.messages().send(sender, "nick.realname", "nick", data.nick() != null ? data.nick() : input,
                    "player", name);
            return true;
        }
        plugin.cache().nickOfOffline(owner, nick -> plugin.messages().send(sender, "nick.realname",
                "nick", nick != null ? nick : input, "player", name));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length != 1) return List.of();
        List<String> out = new ArrayList<>();
        for (PlayerData data : plugin.cache().all()) {
            String nick = data.shownNick();
            if (nick != null && !nick.contains(" ")
                    && nick.toLowerCase().startsWith(args[0].toLowerCase())) {
                out.add(nick);
            }
        }
        return out;
    }
}
