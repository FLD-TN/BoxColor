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
 * [<] B [x][«][#][»][T] B [L]     < quay lại (45), x bỏ màu (47), « » trang (48, 50), # số trang (49),
 *                                 T (51): chat = tự chỉnh mã màu (chỉ khi có quyền lẻ), tên tĩnh = Màu tùy chỉnh,
 *                                 tên động = đổi chữ của tên; L (53) = bộ lọc tất cả / đã sở hữu / chưa sở hữu
 * </pre>
 */
public final class ColorMenu extends MenuView {

    /** Trang màu: chat, tên tĩnh hoặc tên động. {@link #key()} dùng trong messages.yml. */
    public enum Section {
        CHAT(Channel.CHAT, "chat"),
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

        /** Các màu thuộc trang này, theo thứ tự colors.yml. */
        public List<ColorStyle> styles(BoxColorPlugin plugin) {
            List<ColorStyle> all = plugin.styles().list(channel);
            if (this == CHAT) return all;
            List<ColorStyle> out = new ArrayList<>();
            for (ColorStyle style : all) {
                if (style.animated() == (this == NAME_ANIMATED)) out.add(style);
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
    private static final int SLOT_FILTER = 53;

    /** Bộ lọc danh sách màu. {@link #key()} dùng trong messages.yml. */
    public enum Filter {
        ALL("all"),
        OWNED("owned"),
        NOT_OWNED("not-owned");

        private final String key;

        Filter(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Section section;
    private final Channel channel;
    private final Filter filter;
    /** Số màu cả trang (chưa lọc) và số màu đã sở hữu, cho nút bộ lọc. */
    private final int totalCount;
    private final int ownedCount;
    private final Icons icons;
    private final List<ColorStyle> styles;
    private final int page;
    private final int pages;
    /** Ô → màu trên trang này. */
    private final Map<Integer, ColorStyle> slotStyles = new LinkedHashMap<>();

    /** Mở trang với bộ lọc người chơi dùng lần gần nhất. */
    public ColorMenu(BoxColorPlugin plugin, Player viewer, Section section, int page) {
        this(plugin, viewer, section, page, plugin.menus().filter(viewer.getUniqueId()));
    }

    public ColorMenu(BoxColorPlugin plugin, Player viewer, Section section, int page, Filter filter) {
        this(plugin, viewer, section, page, filter, section.styles(plugin), plugin.colors().access(viewer));
    }

    private ColorMenu(BoxColorPlugin plugin, Player viewer, Section section, int page, Filter filter,
            List<ColorStyle> all, ColorService.Access access) {
        this(plugin, viewer, section, page, filter, all, access, filtered(access, section, all, filter));
    }

    private ColorMenu(BoxColorPlugin plugin, Player viewer, Section section, int page, Filter filter,
            List<ColorStyle> all, ColorService.Access access, List<ColorStyle> styles) {
        super(54, plugin.messages().text("menu.color.title-" + section.key(),
                "page", String.valueOf(clampPage(page, styles.size()) + 1),
                "pages", String.valueOf(pageCount(styles.size()))));
        this.plugin = plugin;
        this.viewer = viewer;
        this.section = section;
        this.channel = section.channel();
        this.filter = filter;
        this.icons = new Icons(plugin);
        this.styles = styles;
        this.pages = pageCount(styles.size());
        this.page = clampPage(page, styles.size());
        this.totalCount = all.size();
        this.ownedCount = filtered(access, section, all, Filter.OWNED).size();
        useAccess(access);
    }

    private static List<ColorStyle> filtered(ColorService.Access access, Section section,
            List<ColorStyle> all, Filter filter) {
        if (filter == Filter.ALL) return all;
        List<ColorStyle> out = new ArrayList<>();
        for (ColorStyle style : all) {
            boolean owned = access.canUse(section.channel(), style);
            if (owned == (filter == Filter.OWNED)) out.add(style);
        }
        return out;
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
        inventory.setItem(SLOT_BACK, icons.button(Material.ARROW,
                section == Section.CHAT ? "menu.color.back" : "menu.color.back-name"));
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
        // Tự chỉnh màu chat không mở theo rank; chỉ hiện với người có quyền lẻ (boxcolor.feature.custom-chat)
        if (section == Section.NAME_STATIC) inventory.setItem(SLOT_CUSTOM, nameCustomItem());
        else if (access(plugin, viewer).hasFeature(ColorService.customFeature(Channel.CHAT))) {
            inventory.setItem(SLOT_CUSTOM, chatCustomItem());
        }
        inventory.setItem(SLOT_FILTER, filterItem());
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
        String selected = selectedId(data);
        for (Map.Entry<Integer, ColorStyle> e : slotStyles.entrySet()) {
            ColorStyle style = e.getValue();
            if (animatedOnly && !style.animated()) continue;
            inventory.setItem(e.getKey(), styleItem(style, data, style.id().equals(selected)));
        }
    }

    private ItemStack styleItem(ColorStyle style, PlayerData data, boolean selected) {
        Messages m = plugin.messages();
        boolean unlocked = access(plugin, viewer).canUse(channel, style);
        String state = selected && unlocked ? "selected" : unlocked ? "unlocked" : "locked";
        Map<String, List<String>> lists = new LinkedHashMap<>();
        lists.put("note", style.animated() && channel == Channel.CHAT
                ? List.of(m.raw("menu.color.note-animated-chat")) : List.of());
        lists.put("state", m.rawList("menu.color.state-" + state));
        String[] ph = {
                "style", icons.styleName(style, true),
                "preview", Icons.styled(previewText(data), style, true),
                "kind", icons.kind(style),
                "unlock", icons.unlock(channel, style)
        };
        Material material = style.icon() != null ? style.icon() : plugin.settings().defaultIcon();
        return Icons.dyed(material, style.firstColor(), m.line("menu.color.item.name", ph),
                m.lines("menu.color.item.lore", lists, ph), selected && unlocked);
    }

    /** Trang hiệu ứng động lưu riêng ({@code decor}), các trang khác lưu màu của kênh. */
    private String selectedId(PlayerData data) {
        return section == Section.NAME_ANIMATED ? data.decor() : data.selection(channel);
    }

    /** Hiệu ứng động chỉ tô lên tên thật (TAB, nametag); các trang khác xem trước bằng nick/câu chat mẫu. */
    private String previewText(PlayerData data) {
        return section == Section.NAME_ANIMATED ? data.realName() : icons.previewText(channel, data.displayText());
    }

    private void drawInfo() {
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        if (data == null) return;
        if (section == Section.NAME_ANIMATED) {
            drawDecorInfo(data);
            return;
        }
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

    /** Ô trên cùng của trang hiệu ứng động: hiệu ứng đang dùng, xem trước trên tên thật. */
    private void drawDecorInfo(PlayerData data) {
        Messages m = plugin.messages();
        ColorStyle decor = data.shownDecor();
        String current = decor != null ? icons.styleName(decor, true) : m.raw("menu.main.decor-none");
        String preview = Icons.styled(data.realName(), decor != null ? decor : plugin.colors().rankOrDefault(data), true);
        String[] ph = {"current", current, "preview", preview};
        inventory.setItem(SLOT_INFO, Icons.item(Material.NETHER_STAR,
                m.line("menu.color.decor-info.name", ph), m.lines("menu.color.decor-info.lore", ph), false));
    }

    /** Nút bộ lọc: dòng của bộ lọc đang chọn được tô sáng. */
    private ItemStack filterItem() {
        Messages m = plugin.messages();
        String[] ph = {
                "all", String.valueOf(totalCount),
                "owned", String.valueOf(ownedCount),
                "not-owned", String.valueOf(totalCount - ownedCount)
        };
        List<String> options = new ArrayList<>();
        for (Filter f : Filter.values()) {
            options.add(m.raw("menu.color.filter.option-" + (f == filter ? "active" : "inactive"),
                    "option", m.raw("menu.color.filter." + f.key(), ph)));
        }
        return Icons.item(Material.HOPPER, m.line("menu.color.filter.name", ph),
                m.lines("menu.color.filter.lore", "options", options, ph), false);
    }

    /** Tự chỉnh màu chat bằng danh sách mã hex. */
    private ItemStack chatCustomItem() {
        Messages m = plugin.messages();
        Feature feature = ColorService.customFeature(Channel.CHAT);
        boolean unlocked = access(plugin, viewer).hasFeature(feature);
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
        boolean unlocked = access(plugin, viewer).hasFeature(Feature.NICK);
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
            case SLOT_BACK -> plugin.menus().open(player, section == Section.CHAT
                    ? new MainMenu(plugin, player) : new NameTypeMenu(plugin, player));
            case SLOT_RESET -> {
                if (section == Section.NAME_ANIMATED) plugin.colors().selectDecor(player, null);
                else plugin.colors().select(player, channel, null);
                render();
            }
            case SLOT_PREVIOUS -> {
                if (page > 0) plugin.menus().open(player, new ColorMenu(plugin, player, section, page - 1, filter));
            }
            case SLOT_NEXT -> {
                if (page < pages - 1) {
                    plugin.menus().open(player, new ColorMenu(plugin, player, section, page + 1, filter));
                }
            }
            case SLOT_CUSTOM -> {
                if (section != Section.CHAT) clickNameCustom(player, click);
                else if (plugin.colors().hasFeature(player, ColorService.customFeature(Channel.CHAT))) {
                    clickChatCustom(player);
                }
            }
            case SLOT_FILTER -> {
                Filter next = filter.next();
                plugin.menus().filter(player.getUniqueId(), next);
                plugin.menus().open(player, new ColorMenu(plugin, player, section, 0, next));
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
        boolean again = style.id().equals(selectedId(data));
        if (section == Section.NAME_ANIMATED) plugin.colors().selectDecor(player, again ? null : style);
        else plugin.colors().select(player, channel, again ? null : style);
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
