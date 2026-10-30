package vn.boxpvp.boxcolor.ui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.config.Messages;
import vn.boxpvp.boxcolor.data.PlayerData;
import vn.boxpvp.boxcolor.service.ColorService;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.tier.Feature;

/**
 * Trang chính, mở bằng {@code /dasac}.
 * <pre>
 *  B  B  B  B  B  B  B  B  B
 *  B  F  F [C] F [N] F  F  B      C = màu chat (12), N = màu tên (14)
 *  B  B  B  B  B  B  B  B  B
 * </pre>
 */
public final class MainMenu extends MenuView {

    private static final int SLOT_CHAT = 12;
    private static final int SLOT_NAME = 14;

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Icons icons;

    public MainMenu(BoxColorPlugin plugin, Player viewer) {
        super(27, plugin.messages().text("menu.main.title"));
        this.plugin = plugin;
        this.viewer = viewer;
        this.icons = new Icons(plugin);
    }

    @Override
    public void render() {
        drawFrame(icons);
        drawItems();
    }

    @Override
    public void refresh() {
        drawItems();
    }

    private void drawItems() {
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        if (data == null) return;
        inventory.setItem(SLOT_CHAT, channelItem(Material.WRITABLE_BOOK, Channel.CHAT, data));
        inventory.setItem(SLOT_NAME, channelItem(Material.NAME_TAG, Channel.NAME, data));
    }

    private ItemStack channelItem(Material material, Channel channel, PlayerData data) {
        Messages m = plugin.messages();
        ColorService.Access access = access(plugin, viewer);
        int unlocked = 0;
        for (ColorStyle style : plugin.styles().list(channel)) {
            if (access.canUse(channel, style)) unlocked++;
        }
        ColorStyle shown = data.shown(channel);
        String current = shown != null ? icons.styleName(shown, true) : m.raw("menu.main.current-none");
        String path = "menu.main." + channel.key();
        String[] ph = {
                "current", current,
                "unlocked", String.valueOf(unlocked),
                "total", String.valueOf(plugin.styles().count(channel)),
                "custom", icons.yesNo(access.hasFeature(ColorService.customFeature(channel))),
                "nick", icons.yesNo(access.hasFeature(Feature.NICK)),
                "decor", data.shownDecor() != null ? icons.styleName(data.shownDecor(), true)
                        : m.raw("menu.main.decor-none")
        };
        return Icons.item(material, m.line(path + ".name", ph), m.lines(path + ".lore", ph), false);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        if (slot == SLOT_CHAT) plugin.menus().open(player, new ColorMenu(plugin, player, ColorMenu.Section.CHAT, 0));
        else if (slot == SLOT_NAME) plugin.menus().open(player, new NameTypeMenu(plugin, player));
    }
}
