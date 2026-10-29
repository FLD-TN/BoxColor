package vn.boxpvp.boxcolor.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import vn.boxpvp.boxcolor.BoxColorPlugin;

/**
 * Danh sách người đang mở menu, kèm đúng đối tượng kho BoxColor đã mở cho họ.
 * Task cập nhật (cho màu chuyển động) chỉ tồn tại khi danh sách không rỗng.
 */
public final class MenuManager {

    private static final long REFRESH_TICKS = 4L;

    private final BoxColorPlugin plugin;
    private final Map<UUID, MenuView> viewers = new HashMap<>();
    private BukkitTask refreshTask;

    public MenuManager(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    /** Mở màn hình ở tick kế tiếp (mở kho ngay trong InventoryClickEvent là không an toàn). */
    public void open(Player player, MenuView view) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!player.isOnline()) return;
                view.render();
                player.openInventory(view.getInventory());
                // Plugin khác có thể huỷ InventoryOpenEvent
                if (player.getOpenInventory().getTopInventory() != view.getInventory()) return;
                viewers.put(player.getUniqueId(), view);
                startTask();
            } catch (Exception e) {
                plugin.report("menu-open", e);
            }
        });
    }

    /** Đóng {@code view} ở tick kế tiếp, nếu người chơi vẫn đang mở đúng nó. */
    public void close(Player player, MenuView view) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory() == view.getInventory()) {
                player.closeInventory();
            }
        });
    }

    /** Người chơi đang mở ĐÚNG màn hình này (so sánh đối tượng, không so tiêu đề). */
    public boolean isViewing(Player player, MenuView view) {
        return viewers.get(player.getUniqueId()) == view
                && player.getOpenInventory().getTopInventory() == view.getInventory();
    }

    /** InventoryCloseEvent: chỉ xoá nếu đúng màn hình đang đăng ký (đổi màn hình cũng sinh sự kiện đóng). */
    public void closed(Player player, MenuView view) {
        viewers.remove(player.getUniqueId(), view);
        stopIfIdle();
    }

    public void forget(UUID uuid) {
        viewers.remove(uuid);
        stopIfIdle();
    }

    public void closeAll() {
        for (UUID uuid : new ArrayList<>(viewers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.closeInventory();
        }
        viewers.clear();
        stopIfIdle();
    }

    private void startTask() {
        if (refreshTask != null) return;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, REFRESH_TICKS, REFRESH_TICKS);
    }

    private void tick() {
        // Dọn phòng hờ trường hợp sự kiện đóng kho không đến
        viewers.entrySet().removeIf(e -> {
            Player player = Bukkit.getPlayer(e.getKey());
            return player == null || player.getOpenInventory().getTopInventory() != e.getValue().getInventory();
        });
        if (viewers.isEmpty()) {
            stopIfIdle();
            return;
        }
        for (MenuView view : new ArrayList<>(viewers.values())) {
            try {
                view.refresh();
            } catch (Exception e) {
                plugin.report("menu-refresh", e);
            }
        }
    }

    private void stopIfIdle() {
        if (viewers.isEmpty() && refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }
}
