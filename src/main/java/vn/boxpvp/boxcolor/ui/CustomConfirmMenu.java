package vn.boxpvp.boxcolor.ui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.config.Messages;
import vn.boxpvp.boxcolor.data.PlayerData;
import vn.boxpvp.boxcolor.service.ColorService;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;

/**
 * Xem trước màu tự chỉnh trước khi áp dụng.
 * <pre>
 *  B  B  B  B  B  B  B  B  B
 *  B  F [✔] F [i] F [✘] F  B      ✔ = áp dụng (11), i = xem trước (13), ✘ = quay lại (15)
 *  B  B  B  B  B  B  B  B  B
 * </pre>
 */
public final class CustomConfirmMenu extends MenuView {

    private static final int SLOT_ACCEPT = 11;
    private static final int SLOT_PREVIEW = 13;
    private static final int SLOT_CANCEL = 15;

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Channel channel;
    private final ColorStyle style;
    private final Icons icons;

    public CustomConfirmMenu(BoxColorPlugin plugin, Player viewer, Channel channel, ColorStyle style) {
        super(27, plugin.messages().text("menu.confirm.title"));
        this.plugin = plugin;
        this.viewer = viewer;
        this.channel = channel;
        this.style = style;
        this.icons = new Icons(plugin);
    }

    @Override
    public void render() {
        drawFrame(icons);
        Messages m = plugin.messages();
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        String text = icons.previewText(channel, data != null ? data.displayText() : viewer.getName());
        List<String> codes = new ArrayList<>();
        for (int rgb : style.colors()) codes.add(Icons.styled(ColorStyle.hex(rgb), ColorStyle.custom(List.of(rgb)), false));
        String[] ph = {
                "preview", Icons.styled(text, style, false),
                "channel", icons.channelName(channel),
                "kind", icons.kind(style),
                "codes", String.join("&7, ", codes)
        };
        inventory.setItem(SLOT_PREVIEW, Icons.dyed(plugin.settings().defaultIcon(), style.firstColor(),
                m.line("menu.confirm.preview.name", ph), m.lines("menu.confirm.preview.lore", ph), false));
        inventory.setItem(SLOT_ACCEPT, icons.button(Material.LIME_DYE, "menu.confirm.accept", ph));
        inventory.setItem(SLOT_CANCEL, icons.button(Material.RED_DYE, "menu.confirm.cancel", ph));
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        if (slot == SLOT_ACCEPT) {
            // Có thể đã mất rank trong lúc xem
            if (!plugin.colors().hasFeature(player, ColorService.customFeature(channel))) {
                plugin.messages().send(player, "color.custom-locked", "channel", icons.channelName(channel),
                        "tier", icons.featureUnlock(ColorService.customFeature(channel)));
                plugin.menus().close(player, this);
                return;
            }
            plugin.colors().select(player, channel, style);
            plugin.menus().open(player, new ColorMenu(plugin, player, channel, 0));
        } else if (slot == SLOT_CANCEL) {
            plugin.menus().open(player, new ColorMenu(plugin, player, channel, 0));
        }
    }
}
