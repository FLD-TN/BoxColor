package vn.boxpvp.boxcolor;

import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import vn.boxpvp.boxcolor.command.AdminCommand;
import vn.boxpvp.boxcolor.command.ColorCommand;
import vn.boxpvp.boxcolor.command.NickCommand;
import vn.boxpvp.boxcolor.command.RealNameCommand;
import vn.boxpvp.boxcolor.config.Messages;
import vn.boxpvp.boxcolor.config.Settings;
import vn.boxpvp.boxcolor.data.PlayerCache;
import vn.boxpvp.boxcolor.data.Storage;
import vn.boxpvp.boxcolor.hook.BoxColorExpansion;
import vn.boxpvp.boxcolor.listener.ChatListener;
import vn.boxpvp.boxcolor.listener.MenuClickListener;
import vn.boxpvp.boxcolor.listener.SessionListener;
import vn.boxpvp.boxcolor.listener.VoucherListener;
import vn.boxpvp.boxcolor.nick.NickService;
import vn.boxpvp.boxcolor.service.ColorService;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.StyleRegistry;
import vn.boxpvp.boxcolor.tier.TierRegistry;
import vn.boxpvp.boxcolor.ui.InputPrompts;
import vn.boxpvp.boxcolor.ui.MenuManager;
import vn.boxpvp.boxcolor.voucher.VoucherService;

public final class BoxColorPlugin extends JavaPlugin {

    private Messages messages;
    private Settings settings;
    private StyleRegistry styles;
    private TierRegistry tiers;
    private Storage storage;
    private PlayerCache cache;
    private NickService nicks;
    private ColorService colors;
    private MenuManager menus;
    private InputPrompts prompts;
    private NickCommand nickCommand;
    private VoucherService vouchers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        for (String file : new String[] {"colors.yml", "tiers.yml"}) {
            if (!new File(getDataFolder(), file).exists()) saveResource(file, false);
        }
        messages = new Messages(this);
        messages.load();
        loadConfigs();

        storage = new Storage();
        try {
            storage.open(new File(getDataFolder(), "data.db"));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, messages.log("log.database-open-failed"), e);
            storage = null;
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cache = new PlayerCache(this, storage);
        nicks = new NickService(this);
        try {
            nicks.init(storage.nickKeys());
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, messages.log("log.database-open-failed"), e);
        }
        colors = new ColorService(this);
        menus = new MenuManager(this);
        prompts = new InputPrompts(this);
        nickCommand = new NickCommand(this);
        vouchers = new VoucherService(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new SessionListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new MenuClickListener(this), this);
        pm.registerEvents(new VoucherListener(this), this);

        command("chatcolor", new ColorCommand(this, Channel.CHAT));
        command("namecolor", new ColorCommand(this, Channel.NAME));
        command("nick", nickCommand);
        command("realname", new RealNameCommand(this));
        command("boxcolor", new AdminCommand(this));

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new BoxColorExpansion(this).register();
        } else {
            getLogger().warning(messages.log("log.no-papi"));
        }

        // Người chơi đang online (reload / plugman)
        for (Player player : Bukkit.getOnlinePlayers()) {
            nicks.onJoin(player);
            cache.load(player, data -> colors.refresh(player));
        }
        colors.start();
    }

    @Override
    public void onDisable() {
        if (colors != null) {
            colors.stop();
            colors.resetDisplayNames();
        }
        if (menus != null) menus.closeAll();
        if (prompts != null) prompts.clear();
        if (cache != null) cache.shutdown();
        if (storage != null) storage.close();
    }

    private void command(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) return;
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /** config.yml, colors.yml, tiers.yml. @return số cảnh báo (đã ghi log) */
    private int loadConfigs() {
        AtomicInteger count = new AtomicInteger();
        Consumer<String> warn = line -> {
            count.incrementAndGet();
            getLogger().warning(messages.log("log.config-warning", "message", line));
        };
        settings = Settings.load(getConfig(), warn);
        styles = StyleRegistry.load(yaml("colors.yml"), warn);
        tiers = TierRegistry.load(yaml("tiers.yml"), styles, warn);
        return count.get();
    }

    private YamlConfiguration yaml(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name));
    }

    /**
     * {@code /boxcolor reload}: đóng GUI và ô nhập đang chờ (danh sách màu có thể đã đổi), nạp lại mọi file rồi
     * tính lại màu của người đang online. Dữ liệu người chơi giữ nguyên.
     *
     * @return số cảnh báo cấu hình
     */
    public int reload() {
        menus.closeAll();
        prompts.clear();
        reloadConfig();
        messages.load();
        int warnings = loadConfigs();
        colors.start();
        colors.refreshAll();
        return warnings;
    }

    /** Lỗi không mong đợi trong một tác vụ (menu, sự kiện), ghi log kèm nơi xảy ra. */
    public void report(String where, Throwable error) {
        getLogger().log(Level.SEVERE, messages.log("log.error", "where", where), error);
    }

    public Messages messages() {
        return messages;
    }

    public Settings settings() {
        return settings;
    }

    public StyleRegistry styles() {
        return styles;
    }

    public TierRegistry tiers() {
        return tiers;
    }

    public PlayerCache cache() {
        return cache;
    }

    public NickService nicks() {
        return nicks;
    }

    public ColorService colors() {
        return colors;
    }

    public MenuManager menus() {
        return menus;
    }

    public InputPrompts prompts() {
        return prompts;
    }

    public VoucherService vouchers() {
        return vouchers;
    }

    public NickCommand nickCommand() {
        return nickCommand;
    }
}
