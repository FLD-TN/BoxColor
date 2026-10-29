package vn.boxpvp.boxcolor.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import vn.boxpvp.boxcolor.ui.InputPrompts.Kind;

/**
 * Danh sách màu của một kênh (chat hoặc tên), 28 màu mỗi trang.
 * <pre>
 *  B  B  B  B [i] B  B  B  B      i = đang dùng (4)
 *  B  .  .  .  .  .  .  .  B      . = màu (10-16, 19-25, 28-34, 37-43)
 *  B  .  .  .  .  .  .  .  B
 *  B  .  .  .  .  .  .  .  B
 *  B  .  .  .  .  .  .  .  B
 * [<] B [x][«][#][»][T] B [N]     < quay lại (45), x bỏ màu (47), « » trang (48, 50), # số trang (49),
 *                                 T tự chỉnh (51), N đổi nick (53, chỉ màu tên)
 * </pre>
 */
public final class ColorMenu extends MenuView {

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int SLOT_INFO = 4;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_RESET = 47;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_CUSTOM = 51;
    private static final int SLOT_NICK = 53;

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Channel channel;
    private final Icons icons;
    private final List<ColorStyle> styles;
    private final int page;
    private final int pages;
    /** Ô → màu trên trang này. */
    private final Map<Integer, ColorStyle> slotStyles = new LinkedHashMap<>();

    public ColorMenu(BoxColorPlugin plugin, Player viewer, Channel channel, int page) {
        this(plugin, viewer, channel, page, plugin.styles().list(channel));
    }

    private ColorMenu(BoxColorPlugin plugin, Player viewer, Channel channel, int page, List<ColorStyle> styles) {
        super(54, plugin.messages().text("menu.color.title-" + channel.key(),
                "page", String.valueOf(clampPage(page, styles.size()) + 1),
                "pages", String.valueOf(pageCount(styles.size()))));
        this.plugin = plugin;
        this.viewer = viewer;
        this.channel = channel;
        this.icons = new Icons(plugin);
        this.styles = styles;
        this.pages = pageCount(styles.size());
        this.page = clampPage(page, styles.size());
    }

    private static int pageCount(int size) {
        return Math.max(1, (size + SLOTS.length - 1) / SLOTS.length);
    }

    private static int clampPage(int page, int size) {
        return Math.max(0, Math.min(pageCount(size) - 1, page));
    }

    @Override
    public void render() {
        drawFrame(icons);
        slotStyles.clear();
        int start = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length && start + i < styles.size(); i++) {
            slotStyles.put(SLOTS[i], styles.get(start + i));
        }
        drawStyles(false);
        drawInfo();
        Messages m = plugin.messages();
        inventory.setItem(SLOT_BACK, icons.button(Material.ARROW, "menu.color.back"));
        inventory.setItem(SLOT_RESET, icons.button(Material.BARRIER, "menu.color.reset",
                "channel", icons.channelName(channel)));
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, icons.button(Material.SPECTRAL_ARROW, "menu.color.previous-page",
                    "page", String.valueOf(page)));
        }
        if (page < pages - 1) {
            inventory.setItem(SLOT_NEXT, icons.button(Material.SPECTRAL_ARROW, "menu.color.next-page",
                    "page", String.valueOf(page + 2)));
        }
        inventory.setItem(SLOT_PAGE, Icons.item(Material.PAPER,
                m.line("menu.color.page-info.name", "page", String.valueOf(page + 1), "pages", String.valueOf(pages)),
                m.lines("menu.color.page-info.lore", "total", String.valueOf(styles.size())), false));
        inventory.setItem(SLOT_CUSTOM, customItem());
        if (channel == Channel.NAME) inventory.setItem(SLOT_NICK, nickItem());
    }

    /** Màu chuyển động chạy cả trong GUI. */
    @Override
    public void refresh() {
        drawStyles(true);
        drawInfo();
    }

    private void drawStyles(boolean animatedOnly) {
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        if (data == null) return;
        String selected = data.selection(channel);
        for (Map.Entry<Integer, ColorStyle> e : slotStyles.entrySet()) {
            ColorStyle style = e.getValue();
            if (animatedOnly && !style.animated()) continue;
            inventory.setItem(e.getKey(), styleItem(style, data, style.id().equals(selected)));
        }
    }

    private ItemStack styleItem(ColorStyle style, PlayerData data, boolean selected) {
        Messages m = plugin.messages();
        boolean unlocked = plugin.colors().canUse(viewer, channel, style);
        String state = selected && unlocked ? "selected" : unlocked ? "unlocked" : "locked";
        Map<String, List<String>> lists = new LinkedHashMap<>();
        lists.put("note", style.animated() && channel == Channel.CHAT
                ? List.of(m.raw("menu.color.note-animated-chat")) : List.of());
        lists.put("state", m.rawList("menu.color.state-" + state));
        String[] ph = {
                "style", icons.styleName(style, true),
                "preview", Icons.styled(icons.previewText(channel, data.displayText()), style, true),
                "kind", icons.kind(style),
                "unlock", icons.unlock(channel, style)
        };
        Material material = style.icon() != null ? style.icon() : plugin.settings().defaultIcon();
        return Icons.dyed(material, style.firstColor(), m.line("menu.color.item.name", ph),
                m.lines("menu.color.item.lore", lists, ph), selected && unlocked);
    }

    private void drawInfo() {
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        if (data == null) return;
        ColorStyle shown = data.shown(channel);
        Messages m = plugin.messages();
        String current = shown != null ? icons.styleName(shown, true) : m.raw("menu.main.current-none");
        ColorStyle previewStyle = shown != null ? shown
                : channel == Channel.NAME ? plugin.colors().nameStyle(data) : null;
        String text = icons.previewText(channel, data.displayText());
        String preview = previewStyle != null ? Icons.styled(text, previewStyle, true) : "&f" + text;
        String[] ph = {"channel", icons.channelName(channel), "current", current, "preview", preview};
        inventory.setItem(SLOT_INFO, Icons.item(channel == Channel.CHAT ? Material.WRITABLE_BOOK : Material.NAME_TAG,
                m.line("menu.color.info.name", ph), m.lines("menu.color.info.lore", ph), false));
    }

    private ItemStack customItem() {
        Messages m = plugin.messages();
        Feature feature = ColorService.customFeature(channel);
        boolean unlocked = plugin.colors().hasFeature(viewer, feature);
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        ColorStyle shown = data == null ? null : data.shown(channel);
        String state = !unlocked ? "locked" : shown != null && shown.isCustom() ? "active" : "unlocked";
        String[] ph = {
                "max", String.valueOf(plugin.settings().customMaxColors()),
                "tier", icons.featureUnlock(feature),
                "current", shown != null && shown.isCustom() ? icons.styleName(shown, false) : ""
        };
        Map<String, List<String>> lists = Map.of("state", m.rawList("menu.color.custom.state-" + state, ph));
        return Icons.item(Material.BRUSH, m.line("menu.color.custom.name", ph),
                m.lines("menu.color.custom.lore", lists, ph), false);
    }

    private ItemStack nickItem() {
        Messages m = plugin.messages();
        boolean unlocked = plugin.colors().hasFeature(viewer, Feature.NICK);
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        String nick = data == null || data.nick() == null ? m.raw("menu.main.nick-none") : "&f" + data.nick();
        String state = unlocked ? (data != null && data.nick() != null ? "has-nick" : "unlocked") : "locked";
        String[] ph = {
                "nick", nick,
                "min", String.valueOf(plugin.settings().nickRules().minLength()),
                "max", String.valueOf(plugin.settings().nickRules().maxLength()),
                "tier", icons.featureUnlock(Feature.NICK)
        };
        Map<String, List<String>> lists = Map.of("state", m.rawList("menu.color.nick.state-" + state, ph));
        return Icons.item(Material.NAME_TAG, m.line("menu.color.nick.name", ph),
                m.lines("menu.color.nick.lore", lists, ph), false);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        ColorStyle style = slotStyles.get(slot);
        if (style != null) {
            clickStyle(player, style);
            return;
        }
        switch (slot) {
            case SLOT_BACK -> plugin.menus().open(player, new MainMenu(plugin, player));
            case SLOT_RESET -> {
                plugin.colors().select(player, channel, null);
                render();
            }
            case SLOT_PREVIOUS -> {
                if (page > 0) plugin.menus().open(player, new ColorMenu(plugin, player, channel, page - 1, styles));
            }
            case SLOT_NEXT -> {
                if (page < pages - 1) {
                    plugin.menus().open(player, new ColorMenu(plugin, player, channel, page + 1, styles));
                }
            }
            case SLOT_CUSTOM -> clickCustom(player);
            case SLOT_NICK -> {
                if (channel == Channel.NAME) clickNick(player, click);
            }
            default -> {
            }
        }
    }

    private void clickStyle(Player player, ColorStyle style) {
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null) return;
        if (!plugin.colors().canUse(player, channel, style)) {
            plugin.messages().send(player, "color.locked", "tier", icons.unlock(channel, style));
            return;
        }
        // Bấm lại màu đang dùng: bỏ chọn
        plugin.colors().select(player, channel, style.id().equals(data.selection(channel)) ? null : style);
        render();
    }

    private void clickCustom(Player player) {
        Feature feature = ColorService.customFeature(channel);
        if (!plugin.colors().hasFeature(player, feature)) {
            plugin.messages().send(player, "color.custom-locked",
                    "channel", icons.channelName(channel), "tier", icons.featureUnlock(feature));
            return;
        }
        plugin.menus().close(player, this);
        plugin.prompts().start(player, channel == Channel.CHAT ? Kind.CHAT_COLOR : Kind.NAME_COLOR);
    }

    private void clickNick(Player player, ClickType click) {
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null) return;
        if (click.isRightClick() && data.nick() != null) {
            plugin.nickCommand().reset(player, data);
            render();
            return;
        }
        if (!plugin.colors().hasFeature(player, Feature.NICK)) {
            plugin.messages().send(player, "nick.locked", "tier", icons.featureUnlock(Feature.NICK));
            return;
        }
        plugin.menus().close(player, this);
        plugin.prompts().start(player, Kind.NICK);
    }
}
