package vn.boxpvp.boxcolor.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.ui.MenuView;

/**
 * Huỷ MỌI thao tác khi kho BoxColor đang mở: click thường, shift-click, click túi đồ dưới, phím số hotbar,
 * kéo thả. Chỉ chuyển cho màn hình số ô người chơi bấm — không bao giờ đọc tên, lore hay NBT của item.
 */
public final class MenuClickListener implements Listener {

    private final BoxColorPlugin plugin;

    public MenuClickListener(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof MenuView view)) return;
        event.setCancelled(true); // trước mọi thứ khác, kể cả khi phần sau ném ngoại lệ

        try {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            // Đúng đối tượng kho đã mở cho người này, không chỉ trùng tiêu đề
            if (!plugin.menus().isViewing(player, view)) return;
            if (event.getClickedInventory() != top) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= top.getSize()) return;
            ClickType click = event.getClick();
            // DOUBLE_CLICK đi kèm sau một cú LEFT — bỏ qua để không bật/tắt hai lần
            if (click == ClickType.DOUBLE_CLICK || !(click.isLeftClick() || click.isRightClick())) return;
            view.onClick(player, slot, click);
        } catch (Exception e) {
            plugin.report("menu-click", e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof MenuView) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        try {
            if (event.getInventory().getHolder(false) instanceof MenuView view
                    && event.getPlayer() instanceof Player player) {
                plugin.menus().closed(player, view);
            }
        } catch (Exception e) {
            plugin.report("menu-close", e);
        }
    }
}
