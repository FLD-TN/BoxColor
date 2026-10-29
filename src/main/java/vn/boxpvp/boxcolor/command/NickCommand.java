package vn.boxpvp.boxcolor.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.data.PlayerData;
import vn.boxpvp.boxcolor.nick.NickRules;
import vn.boxpvp.boxcolor.tier.Feature;
import vn.boxpvp.boxcolor.ui.Icons;
import vn.boxpvp.boxcolor.util.Durations;

/** {@code /nick <tên>} và {@code /nick reset}. Cũng dùng cho ô "Đổi nick" trong GUI. */
public final class NickCommand implements TabExecutor {

    public static final String BYPASS_COOLDOWN = "boxcolor.nick.bypass-cooldown";

    private final BoxColorPlugin plugin;

    public NickCommand(BoxColorPlugin plugin) {
        this.plugin = plugin;
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
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null) {
            plugin.messages().send(player, "command.not-loaded");
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(player, "nick.usage", "label", label);
            return true;
        }
        if (args.length == 1 && List.of("reset", "off").contains(args[0].toLowerCase(Locale.ROOT))) {
            if (data.nick() == null) plugin.messages().send(player, "nick.none");
            else reset(player, data);
            return true;
        }
        apply(player, String.join(" ", args));
        return true;
    }

    /**
     * Kiểm tra đủ luật rồi đặt nick, báo kết quả cho người chơi.
     *
     * @return {@code true} nếu đã đổi được
     */
    public boolean apply(Player player, String input) {
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null) {
            plugin.messages().send(player, "command.not-loaded");
            return false;
        }
        if (!plugin.colors().hasFeature(player, Feature.NICK)) {
            plugin.messages().send(player, "nick.locked", "tier", new Icons(plugin).featureUnlock(Feature.NICK));
            return false;
        }
        NickRules rules = plugin.settings().nickRules();
        String nick = NickRules.clean(input);
        if (nick.equals(data.nick())) {
            plugin.messages().send(player, "nick.same");
            return false;
        }
        NickRules.Result result = rules.validate(nick);
        if (result != NickRules.Result.OK) {
            plugin.messages().send(player, "nick." + result.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                    "min", String.valueOf(rules.minLength()), "max", String.valueOf(rules.maxLength()));
            return false;
        }
        if (plugin.nicks().takenBy(player.getUniqueId(), nick) != null) {
            plugin.messages().send(player, "nick.taken");
            return false;
        }
        long now = System.currentTimeMillis();
        long wait = data.nickChangedAt() + plugin.settings().nickCooldownMillis() - now;
        if (wait > 0 && !player.hasPermission(BYPASS_COOLDOWN)) {
            plugin.messages().send(player, "nick.cooldown", "time", Durations.format(wait, plugin.messages()));
            return false;
        }
        set(player, data, nick, now);
        return true;
    }

    /** Đặt nick không qua luật (admin), chỉ còn chặn trùng. */
    public void set(Player player, PlayerData data, String nick, long changedAt) {
        plugin.nicks().set(data, nick);
        data.nickChangedAt(changedAt);
        plugin.cache().save(data);
        plugin.colors().refresh(player);
        plugin.messages().send(player, "nick.set",
                "nick", Icons.styled(nick, plugin.colors().nameStyle(data), false));
    }

    public void reset(Player player, PlayerData data) {
        plugin.nicks().set(data, null);
        plugin.cache().save(data);
        plugin.colors().refresh(player);
        plugin.messages().send(player, "nick.reset");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1 && "reset".startsWith(args[0].toLowerCase(Locale.ROOT))) return List.of("reset");
        return List.of();
    }
}
