package vn.boxpvp.boxcolor.shader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.scheduler.BukkitTask;
import vn.boxpvp.boxcolor.BoxColorPlugin;

/**
 * Pack shader riêng của BoxColor: tạo zip, đọc SHA-1 và gửi cho người chơi (sau pack của ItemsAdder, nên file của pack
 * này đè lên khi trùng). Không bắt buộc: người chơi từ chối hoặc tải lỗi chỉ thấy màu tĩnh gần giống.
 */
public final class ShaderPack {

    /** Cố định để gửi lại là thay đúng pack cũ, không chồng thêm pack mới. */
    public static final UUID PACK_ID = UUID.nameUUIDFromBytes("BoxColor-shader".getBytes(StandardCharsets.UTF_8));
    public static final String FILE_NAME = "BoxColor-shader.zip";
    /** Giao thức của 1.20.5: bản đầu tiên ItemsAdder hỗ trợ. Bản cũ hơn có thể bị pack này thay mất pack ItemsAdder. */
    private static final int MIN_PROTOCOL = 766;
    /** Thời điểm cố định cho mọi file trong zip: cùng nội dung luôn ra cùng SHA-1. */
    private static final long FIXED_TIME = 1_700_000_000_000L;

    /** Trạng thái kết thúc của một lần tải pack. */
    private static final Set<PlayerResourcePackStatusEvent.Status> FINAL = EnumSet.of(
            PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED,
            PlayerResourcePackStatusEvent.Status.DECLINED,
            PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD,
            PlayerResourcePackStatusEvent.Status.INVALID_URL,
            PlayerResourcePackStatusEvent.Status.FAILED_RELOAD,
            PlayerResourcePackStatusEvent.Status.DISCARDED);

    private final BoxColorPlugin plugin;
    private byte[] sha1;
    /** Pack của ItemsAdder ({@code resource-pack.uuid}), {@code null} nếu không có hoặc ItemsAdder không tự gửi. */
    private UUID iaPackId;
    private boolean iaBeforeJoin;
    /** Người đang chờ pack ItemsAdder xong, kèm task gửi dự phòng khi hết thời gian chờ. */
    private final Map<UUID, BukkitTask> waiting = new HashMap<>();

    public ShaderPack(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    /** Zip có thứ tự file và thời gian cố định. */
    public static byte[] zip(Map<String, String> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> file : new TreeMap<>(files).entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTime(FIXED_TIME);
                zip.putNextEntry(entry);
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    public static byte[] sha1(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String hex(byte[] hash) {
        return HexFormat.of().formatHex(hash);
    }

    public Path file() {
        return plugin.getDataFolder().toPath().resolve("shader-pack").resolve(FILE_NAME);
    }

    /** Đọc SHA-1 của zip hiện có (lúc bật plugin, reload và sau /boxcolor shader). */
    public void reloadHash() {
        Path file = file();
        sha1 = null;
        if (!Files.isRegularFile(file)) return;
        try {
            sha1 = sha1(Files.readAllBytes(file));
        } catch (IOException e) {
            plugin.report("shader-pack-hash", e);
        }
    }

    public String hashHex() {
        return sha1 == null ? "" : hex(sha1);
    }

    /** Có đủ link và zip để gửi. */
    public boolean ready() {
        return plugin.settings().shaderEnabled() && !plugin.settings().shaderPackUrl().isEmpty() && sha1 != null;
    }

    /**
     * Đọc {@code resource-pack.uuid} và {@code auto_apply} trong config của ItemsAdder, để biết pack nào cần chờ.
     * Gọi lúc bật plugin và khi reload.
     */
    public void loadItemsAdder() {
        iaPackId = null;
        iaBeforeJoin = false;
        File file = new File(plugin.getDataFolder().getParentFile(), "ItemsAdder/config.yml");
        if (!file.isFile()) return;
        YamlConfiguration ia = YamlConfiguration.loadConfiguration(file);
        if (!ia.getBoolean("resource-pack.auto_apply.enabled", true)) return;
        try {
            iaPackId = UUID.fromString(ia.getString("resource-pack.uuid", ""));
        } catch (IllegalArgumentException e) {
            return;
        }
        iaBeforeJoin = ia.getBoolean("resource-pack.auto_apply.before_join", false);
    }

    /**
     * Người chơi vào server. Thứ tự ưu tiên không phụ thuộc pack nào tải xong trước (pack gửi sau luôn đè pack gửi
     * trước); chờ chỉ để người chơi không gặp hai màn hình tải cùng lúc:
     * <ul>
     *   <li>ItemsAdder tải pack trước khi vào game ({@code before_join}) hoặc không có pack ItemsAdder: gửi ngay</li>
     *   <li>còn lại: chờ client báo pack ItemsAdder xong ({@link #onStatus}), tối đa {@code shader.pack.max-wait-seconds}</li>
     * </ul>
     */
    public void onJoin(Player player) {
        if (!ready()) return;
        UUID uuid = player.getUniqueId();
        if (iaPackId == null || iaBeforeJoin) {
            Bukkit.getScheduler().runTask(plugin, () -> send(player));
            return;
        }
        long ticks = plugin.settings().shaderPackMaxWaitSeconds() * 20L;
        BukkitTask previous = waiting.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            waiting.remove(uuid);
            send(player);
        }, ticks));
        if (previous != null) previous.cancel();
    }

    /** Client báo trạng thái một pack: pack ItemsAdder đã xong (thành công hay không) thì gửi pack BoxColor. */
    public void onStatus(Player player, UUID packId, PlayerResourcePackStatusEvent.Status status) {
        if (iaPackId == null || !iaPackId.equals(packId) || !FINAL.contains(status)) return;
        BukkitTask task = waiting.remove(player.getUniqueId());
        if (task == null) return;
        task.cancel();
        send(player);
    }

    public void onQuit(UUID uuid) {
        BukkitTask task = waiting.remove(uuid);
        if (task != null) task.cancel();
    }

    public void send(Player player) {
        if (!player.isOnline() || !ready()) return;
        if (protocol(player) < MIN_PROTOCOL) return;
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(PACK_ID,
                URI.create(plugin.settings().shaderPackUrl()), hex(sha1));
        ResourcePackRequest.Builder request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .replace(false) // giữ pack của ItemsAdder
                .required(false);
        if (!plugin.messages().raw("shader.pack-prompt").isEmpty()) {
            request.prompt(plugin.messages().text("shader.pack-prompt"));
        }
        player.sendResourcePacks(request.build());
    }

    /**
     * Giao thức client qua ViaVersion (đọc bằng reflection, không cần ViaVersion lúc biên dịch). Không có ViaVersion
     * thì mọi người cùng bản server: coi như đủ.
     */
    private int protocol(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) return Integer.MAX_VALUE;
        try {
            Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
            Object api = via.getMethod("getAPI").invoke(null);
            Method version = api.getClass().getMethod("getPlayerVersion", UUID.class);
            version.setAccessible(true);
            return (int) version.invoke(api, player.getUniqueId());
        } catch (ReflectiveOperationException | ClassCastException e) {
            return Integer.MAX_VALUE;
        }
    }
}
