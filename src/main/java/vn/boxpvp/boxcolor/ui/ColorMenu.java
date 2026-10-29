package vn.boxpvp.boxcolor.ui;

import java.util.ArrayList;
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
 * Danh sách màu của một trang ({@link Section}), 28 màu mỗi trang.
 * <pre>
 *  B  B  B  B [i] B  B  B  B      i = đang dùng (4)
 *  B  .  .  .  .  .  .  .  B      . = màu (10-16, 19-25, 28-34, 37-43)
 *  B  .  .  .  .  .  .  .  B
 *  B  .  .  .  .  .  .  .  B
 *  B  .  .  .  .  .  .  .  B
 * [<] B [x][«][#][»][T] B  B      < quay lại (45), x bỏ màu (47), « » trang (48, 50), # số trang (49),
 *                                 T (51): chat tĩnh = tự chỉnh mã màu, chat động = không có,
 *                                 tên tĩnh = Màu tùy chỉnh, tên động = đổi chữ của tên
 * </pre>
 */
public final class ColorMenu extends MenuView {

    /** Trang màu: chat, tên tĩnh hoặc tên động. {@link #key()} dùng trong messages.yml. */
    public enum Section {
        CHAT_STATIC(Channel.CHAT, "chat-static"),
        CHAT_ANIMATED(Channel.CHAT, "chat-animated"),
        NAME_STATIC(Channel.NAME, "name-static"),
        NAME_ANIMATED(Channel.NAME, "name-animated");

        private final Channel channel;
        private final String key;

        Section(Channel channel, String key) {
            this.channel = channel;
            this.key = key;
        }

        public Channel channel() {
            return channel;
        }

        public String key() {
            return key;
        }

        public boolean animated() {
            return this == CHAT_ANIMATED || this == NAME_ANIMATED;
        }

        public static Section of(Channel channel, boolean animated) {
            if (channel == Channel.CHAT) return animated ? CHAT_ANIMATED : CHAT_STATIC;
            return animated ? NAME_ANIMATED : NAME_STATIC;
        }

        /** Các màu thuộc trang này, theo thứ tự colors.yml. */
        public List<ColorStyle> styles(BoxColorPlugin plugin) {
            List<ColorStyle> out = new ArrayList<>();
            for (ColorStyle style : plugin.styles().list(channel)) {
                if (style.animated() == animated()) out.add(style);
            }
            return out;
        }
    }

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

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Section section;
    private final Channel channel;
    private final Icons icons;
    private final List<ColorStyle> styles;
    private final int page;
    private final int pages;
    /** Ô → màu trên trang này. */
    private final Map<Integer, ColorStyle> slotStyles = new LinkedHashMap<>();

    public ColorMenu(BoxColorPlugin plugin, Player viewer, Section section, int page) {
        this(plugin, viewer, section, page, section.styles(plugin));
    }

    private ColorMenu(BoxColorPlugin plugin, Player viewer, Section section, int page, List<ColorStyle> styles) {
        super(54, plugin.messages().text("menu.color.title-" + section.key(),
                "page", String.valueOf(clampPage(page, styles.size()) + 1),
                "pages", String.valueOf(pageCount(styles.size()))));
        this.plugin = plugin;
        this.viewer = viewer;
        this.section = section;
        this.channel = section.channel();
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
        inventory.setItem(SLOT_BACK, icons.button(Material.ARROW, "menu.color.back-type"));
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
        if (section == Section.CHAT_STATIC) inventory.setItem(SLOT_CUSTOM, chatCustomItem());
        else if (channel == Channel.NAME) inventory.setItem(SLOT_CUSTOM, nameCustomItem());
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
        List<String> note = List.of();
        if (style.usesShader()) note = List.of(m.raw("menu.color.note-shader"));
        else if (style.animated()) note = List.of(m.raw("menu.color.note-animated-chat"));
        lists.put("note", note);
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

    /** Tự chỉnh màu chat bằng danh sách mã hex. */
    private ItemStack chatCustomItem() {
        Messages m = plugin.messages();
        Feature feature = ColorService.customFeature(Channel.CHAT);
        boolean unlocked = plugin.colors().hasFeature(viewer, feature);
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        ColorStyle shown = data == null ? null : data.shown(Channel.CHAT);
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

    /**
     * Tên tĩnh: "Màu tùy chỉnh" (đặt tên kèm mã màu từng chữ). Tên động: chỉ đổi chữ, màu động giữ nguyên.
     * Cả hai đều là tính năng {@code nick}.
     */
    private ItemStack nameCustomItem() {
        Messages m = plugin.messages();
        boolean unlocked = plugin.colors().hasFeature(viewer, Feature.NICK);
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        boolean hasNick = data != null && data.nick() != null;
        String nick = hasNick ? Icons.styled(data.nick(), plugin.colors().nameStyle(data), false)
                : m.raw("menu.main.nick-none");
        String state = !unlocked ? "locked" : hasNick ? "has-nick" : "unlocked";
        String path = "menu.color." + (section == Section.NAME_ANIMATED ? "rename" : "nick");
        String[] ph = {
                "nick", nick,
                "min", String.valueOf(plugin.settings().nickRules().minLength()),
                "max", String.valueOf(plugin.settings().nickRules().maxLength()),
                "tier", icons.featureUnlock(Feature.NICK)
        };
        Map<String, List<String>> lists = Map.of("state", m.rawList(path + ".state-" + state, ph));
        return Icons.item(Material.NAME_TAG, m.line(path + ".name", ph), m.lines(path + ".lore", lists, ph), false);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        ColorStyle style = slotStyles.get(slot);
        if (style != null) {
            clickStyle(player, style);
            return;
        }
        switch (slot) {
            case SLOT_BACK -> plugin.menus().open(player, new TypeMenu(plugin, player, channel));
            case SLOT_RESET -> {
                plugin.colors().select(player, channel, null);
                render();
            }
            case SLOT_PREVIOUS -> {
                if (page > 0) plugin.menus().open(player, new ColorMenu(plugin, player, section, page - 1, styles));
            }
            case SLOT_NEXT -> {
                if (page < pages - 1) {
                    plugin.menus().open(player, new ColorMenu(plugin, player, section, page + 1, styles));
                }
            }
            case SLOT_CUSTOM -> {
                if (section == Section.CHAT_STATIC) clickChatCustom(player);
                else if (channel == Channel.NAME) clickNameCustom(player, click);
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

    private void clickChatCustom(Player player) {
        Feature feature = ColorService.customFeature(Channel.CHAT);
        if (!plugin.colors().hasFeature(player, feature)) {
            plugin.messages().send(player, "color.custom-locked",
                    "channel", icons.channelName(Channel.CHAT), "tier", icons.featureUnlock(feature));
            return;
        }
        plugin.menus().close(player, this);
        plugin.prompts().start(player, Kind.CHAT_COLOR);
    }

    private void clickNameCustom(Player player, ClickType click) {
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
        plugin.prompts().start(player, section == Section.NAME_ANIMATED ? Kind.RENAME : Kind.NICK);
    }
}
