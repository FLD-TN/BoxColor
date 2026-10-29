package vn.boxpvp.boxcolor.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.data.PlayerData;
import vn.boxpvp.boxcolor.nick.NickInput;
import vn.boxpvp.boxcolor.nick.NickRules;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;

/**
 * {@code /boxcolor reload}, {@code /boxcolor shader} (xuất shader cho resource pack), {@code /boxcolor reset <người chơi> <chat|name|nick|all>},
 * {@code /boxcolor nick <người chơi> <tên|reset>}.
 */
public final class AdminCommand implements TabExecutor {

    private static final List<String> RESET_WHAT = List.of("chat", "name", "nick", "all");

    private final BoxColorPlugin plugin;

    public AdminCommand(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("boxcolor.admin")) {
            plugin.messages().send(sender, "command.no-permission");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                int warnings = plugin.reload();
                plugin.messages().send(sender, "command.reloaded",
                        "chat", String.valueOf(plugin.styles().count(Channel.CHAT)),
                        "name", String.valueOf(plugin.styles().count(Channel.NAME)),
                        "tiers", String.valueOf(plugin.tiers().all().size()),
                        "warnings", String.valueOf(warnings));
            }
            case "shader" -> {
                try {
                    int count = plugin.exportShader();
                    plugin.messages().send(sender, "admin.shader-done", "count", String.valueOf(count),
                            "enabled", plugin.messages().raw(plugin.settings().shaderEnabled()
                                    ? "admin.shader-enabled" : "admin.shader-disabled"));
                } catch (IOException e) {
                    plugin.report("shader-export", e);
                    plugin.messages().send(sender, "admin.shader-failed");
                }
            }
            case "reset" -> reset(sender, args);
            case "nick" -> nick(sender, args);
            default -> plugin.messages().send(sender, "admin.usage", "label", label);
        }
        return true;
    }

    private void reset(CommandSender sender, String[] args) {
        if (args.length < 3 || !RESET_WHAT.contains(args[2].toLowerCase(Locale.ROOT))) {
            plugin.messages().send(sender, "admin.usage", "label", "boxcolor");
            return;
        }
        String what = args[2].toLowerCase(Locale.ROOT);
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            resetOffline(sender, args[1], what);
            return;
        }
        PlayerData data = plugin.cache().get(target.getUniqueId());
        if (data == null) {
            plugin.messages().send(sender, "admin.not-loaded", "player", target.getName());
            return;
        }
        if (what.equals("chat") || what.equals("all")) data.selection(Channel.CHAT, null);
        if (what.equals("name") || what.equals("all")) data.selection(Channel.NAME, null);
        if (what.equals("nick") || what.equals("all")) plugin.nicks().set(data, null);
        plugin.cache().save(data);
        plugin.colors().refresh(target);
        plugin.messages().send(sender, "admin.reset-done", "what", plugin.messages().raw("admin.what-" + what),
                "player", target.getName());
    }

    /** Người offline: chỉ xoá được nick (đủ để gỡ nick phản cảm). */
    private void resetOffline(CommandSender sender, String name, String what) {
        if (!what.equals("nick")) {
            plugin.messages().send(sender, "admin.offline-nick-only", "player", name);
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline == null) {
            plugin.messages().send(sender, "command.player-not-found", "player", name);
            return;
        }
        UUID uuid = offline.getUniqueId();
        plugin.nicks().forgetOffline(uuid);
        plugin.cache().clearNickOffline(uuid);
        plugin.messages().send(sender, "admin.reset-done", "what", plugin.messages().raw("admin.what-nick"),
                "player", name);
    }

    private void nick(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "admin.usage", "label", "boxcolor");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "command.player-not-found", "player", args[1]);
            return;
        }
        PlayerData data = plugin.cache().get(target.getUniqueId());
        if (data == null) {
            plugin.messages().send(sender, "admin.not-loaded", "player", target.getName());
            return;
        }
        String input = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (args.length == 3 && List.of("reset", "off").contains(input.toLowerCase(Locale.ROOT))) {
            plugin.nickCommand().reset(target, data);
            plugin.messages().send(sender, "admin.reset-done", "what", plugin.messages().raw("admin.what-nick"),
                    "player", target.getName());
            return;
        }
        NickInput.Parsed parsed = NickInput.parse(input);
        String nick = NickRules.clean(parsed.text());
        // Admin bỏ qua độ dài, từ cấm và thời gian chờ; vẫn chặn ký tự lạ và trùng tên
        NickRules.Result result = plugin.settings().nickRules().validate(nick);
        if (result == NickRules.Result.BAD_CHARS || result == NickRules.Result.NO_LETTERS) {
            plugin.messages().send(sender, "nick." + result.name().toLowerCase(Locale.ROOT).replace('_', '-'));
            return;
        }
        if (plugin.nicks().takenBy(target.getUniqueId(), nick) != null) {
            plugin.messages().send(sender, "nick.taken");
            return;
        }
        if (parsed.hasColors()) {
            data.selection(Channel.NAME, ColorStyle.perChar(parsed.colors()).encodeCustom());
        }
        plugin.nickCommand().set(target, data, nick, data.nickChangedAt());
        plugin.messages().send(sender, "admin.nick-done", "player", target.getName(), "nick", nick);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (!sender.hasPermission("boxcolor.admin")) return List.of();
        if (args.length == 1) return filter(List.of("reload", "shader", "reset", "nick"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("nick"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filter(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("reset")) return filter(RESET_WHAT, args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("nick")) return filter(List.of("reset"), args[2]);
        return List.of();
    }

    private static List<String> filter(List<String> options, String start) {
        String s = start.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(s)).toList();
    }
}
