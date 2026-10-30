package vn.boxpvp.boxcolor.ui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.config.Messages;
import vn.boxpvp.boxcolor.data.PlayerData;
import vn.boxpvp.boxcolor.hook.PapiText;
import vn.boxpvp.boxcolor.service.ColorService;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.tier.Feature;
import vn.boxpvp.boxcolor.tier.Tier;

/**
 * Trang chính, mở bằng cả {@code /chatcolor} và {@code /namecolor}.
 * <pre>
 *  B  B  B  B  B  B  B  B  B
 *  B  F [C] F [H] F [N] F  B      C = màu chat (11), H = đầu người chơi (13), N = màu tên (15)
 *  B  B  B  B  B  B  B  B  B
 * </pre>
 */
public final class MainMenu extends MenuView {

    private static final int SLOT_CHAT = 11;
    private static final int SLOT_HEAD = 13;
    private static final int SLOT_NAME = 15;

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Icons icons;
    /** Dựng một lần (đầu người chơi tải skin), sau đó chỉ đổi tên và lore. */
    private ItemStack headItem;

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
        inventory.setItem(SLOT_HEAD, head(data));
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

    private ItemStack head(PlayerData data) {
        Messages m = plugin.messages();
        String nick;
        if (data.nick() == null) nick = m.raw("menu.main.nick-none");
        else if (data.shownNick() == null) nick = m.raw("menu.main.nick-hidden", "nick", data.nick());
        else nick = "&f" + data.nick();
        Tier tier = plugin.tiers().current(data.tierLevel());
        String tierName = tier != null ? PapiText.apply(tier.name()) : m.raw("menu.main.tier-none");
        String[] ph = {
                "name", Icons.styled(data.displayText(), plugin.colors().nameStyle(data), true),
                "realname", data.realName(),
                "nick", nick,
                "tier", tierName
        };
        if (headItem == null) {
            headItem = Icons.head(viewer, m.line("menu.main.head.name", ph), m.lines("menu.main.head.lore", ph));
            return headItem;
        }
        ItemMeta meta = headItem.getItemMeta();
        meta.displayName(m.line("menu.main.head.name", ph));
        meta.lore(m.lines("menu.main.head.lore", ph));
        headItem.setItemMeta(meta);
        return headItem;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        if (slot == SLOT_CHAT) plugin.menus().open(player, new ColorMenu(plugin, player, ColorMenu.Section.CHAT, 0));
        else if (slot == SLOT_NAME) plugin.menus().open(player, new NameTypeMenu(plugin, player));
    }
}
