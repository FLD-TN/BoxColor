package vn.boxpvp.boxcolor.listener;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import vn.boxpvp.boxcolor.BoxColorPlugin;

/** Chuột phải cầm phiếu màu: mở khóa màu. Chặn luôn việc mặc giáp da của phiếu lên người. */
public final class VoucherListener implements Listener {

    private final BoxColorPlugin plugin;

    public VoucherListener(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (plugin.vouchers().read(item) == null) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        // Sự kiện bắn cho cả hai tay: chỉ xử lý tay đang cầm phiếu, một lần
        if (event.getHand() == EquipmentSlot.OFF_HAND
                && plugin.vouchers().read(event.getPlayer().getInventory().getItemInMainHand()) != null) {
            return;
        }
        try {
            plugin.vouchers().redeem(event.getPlayer(), item);
        } catch (Exception e) {
            plugin.report("voucher", e);
        }
    }
}
