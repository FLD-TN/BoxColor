package vn.boxpvp.boxcolor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
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
import vn.boxpvp.boxcolor.nick.NickService;
import vn.boxpvp.boxcolor.service.ColorService;
import vn.boxpvp.boxcolor.shader.ReservedColors;
import vn.boxpvp.boxcolor.shader.ShaderGenerator;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.style.StyleRegistry;
import vn.boxpvp.boxcolor.tier.TierRegistry;
import vn.boxpvp.boxcolor.ui.InputPrompts;
import vn.boxpvp.boxcolor.ui.MenuManager;

public final class BoxColorPlugin extends JavaPlugin {

    private Messages messages;
    private Settings settings;
    private StyleRegistry styles;
    /** colors.yml đã gán mã tín hiệu shader (dùng khi bật shader và để xuất pack). */
    private StyleRegistry shaderStyles;
    private TierRegistry tiers;
    private Storage storage;
    private PlayerCache cache;
    private NickService nicks;
    private ColorService colors;
    private MenuManager menus;
    private InputPrompts prompts;
    private NickCommand nickCommand;

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

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new SessionListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new MenuClickListener(this), this);

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
        StyleRegistry loaded = StyleRegistry.load(yaml("colors.yml"), warn);
        // Luôn tính sẵn mã tín hiệu để /boxcolor shader xuất được pack trước khi bật shader.enabled
        shaderStyles = loaded.withShaderMarkers(ReservedColors.of(settings), warn);
        styles = settings.shaderEnabled() ? shaderStyles : loaded;
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

    /** Tên thư mục content của ItemsAdder mà {@code /boxcolor shader} tạo ra. */
    public static final String SHADER_CONTENT = "boxcolor";

    /**
     * {@code /boxcolor shader}: ghi một thư mục content ItemsAdder hoàn chỉnh vào
     * {@code plugins/BoxColor/shader-pack/boxcolor/} (xoá bản cũ trước), để chép nguyên vào {@code ItemsAdder/contents/}.
     * Có cả {@code configs/}: ItemsAdder bỏ qua thư mục content không có config.
     *
     * @return số màu chạy bằng shader
     */
    public int exportShader() throws IOException {
        List<ColorStyle> effects = shaderStyles.shaderStyles();
        Path root = getDataFolder().toPath().resolve("shader-pack");
        if (Files.exists(root)) {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
        Path content = root.resolve(SHADER_CONTENT);
        for (Map.Entry<String, String> file
                : ShaderGenerator.generate(effects, settings.shaderSpreadPixels()).entrySet()) {
            Path target = content.resolve("resourcepack").resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
        }
        Path config = content.resolve("configs").resolve(SHADER_CONTENT + ".yml");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "# Tạo bởi /boxcolor shader. Chỉ để ItemsAdder nhận thư mục này, không có item nào\n"
                + "info:\n  namespace: " + SHADER_CONTENT + "\n", StandardCharsets.UTF_8);
        return effects.size();
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

    public NickCommand nickCommand() {
        return nickCommand;
    }
}
