package vn.boxpvp.boxcolor.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Lớp nền của mọi màn hình. Kho được tạo một lần; mọi cập nhật dùng {@code inventory.setItem()},
 * không mở lại kho (mở lại làm con trỏ chuột nhảy về giữa).
 */
public abstract class MenuView implements InventoryHolder {

    protected final Inventory inventory;

    protected MenuView(int size, Component title) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /** Viền quanh kho, kính tối ở giữa. */
    protected void drawFrame(Icons icons) {
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            int col = slot % 9;
            boolean edge = slot < 9 || slot >= size - 9 || col == 0 || col == 8;
            inventory.setItem(slot, edge ? icons.border() : icons.filler());
        }
    }

    /** Vẽ toàn bộ kho. */
    public abstract void render();

    /** Gọi định kỳ khi đang có người xem (màu chuyển động); mặc định không làm gì. */
    public void refresh() {
    }

    /** Bấm vào một ô của kho trên (sự kiện đã bị huỷ sẵn). */
    public abstract void onClick(Player player, int slot, ClickType click);
}
