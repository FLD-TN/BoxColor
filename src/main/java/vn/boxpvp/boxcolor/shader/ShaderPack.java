package vn.boxpvp.boxcolor.shader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    private final BoxColorPlugin plugin;
    private byte[] sha1;

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

    /** Gửi pack sau {@code shader.pack.delay-ticks}, để pack của ItemsAdder tải xong trước. */
    public void sendLater(Player player) {
        if (!ready()) return;
        long delay = plugin.settings().shaderPackDelayTicks();
        Bukkit.getScheduler().runTaskLater(plugin, () -> send(player), delay);
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
