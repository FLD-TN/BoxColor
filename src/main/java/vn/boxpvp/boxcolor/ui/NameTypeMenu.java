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
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.ui.ColorMenu.Section;

/**
 * Chọn loại màu tên: tĩnh hoặc động. Lore mỗi loại có tên người chơi tô bằng một màu mẫu của loại đó.
 * <pre>
 *  B  B  B  B  B  B  B  B  B
 *  B  F [T] F  F  F [Đ] F  B      T = màu tĩnh (11), Đ = màu động (15)
 *  B  B  B  B [<] B  B  B  B      < = quay lại (22)
 * </pre>
 */
public final class NameTypeMenu extends MenuView {

    private static final int SLOT_STATIC = 11;
    private static final int SLOT_ANIMATED = 15;
    private static final int SLOT_BACK = 22;

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Icons icons;

    public NameTypeMenu(BoxColorPlugin plugin, Player viewer) {
        super(27, plugin.messages().text("menu.name-type.title"));
        this.plugin = plugin;
        this.viewer = viewer;
        this.icons = new Icons(plugin);
    }

    @Override
    public void render() {
        drawFrame(icons);
        inventory.setItem(SLOT_BACK, icons.button(Material.ARROW, "menu.color.back"));
        drawItems();
    }

    /** Màu mẫu của loại động chạy trong GUI. */
    @Override
    public void refresh() {
        drawItems();
    }

    private void drawItems() {
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        if (data == null) return;
        inventory.setItem(SLOT_STATIC, typeItem(Section.NAME_STATIC, Material.NAME_TAG, data));
        inventory.setItem(SLOT_ANIMATED, typeItem(Section.NAME_ANIMATED, Material.NETHER_STAR, data));
    }

    private ItemStack typeItem(Section section, Material material, PlayerData data) {
        Messages m = plugin.messages();
        List<ColorStyle> styles = section.styles(plugin);
        ColorStyle sample = sample(styles);
        // Hiệu ứng động chỉ tô lên tên thật (TAB, nametag); màu tĩnh hiện nick (chat, scoreboard)
        String text = section == Section.NAME_ANIMATED ? data.realName() : data.displayText();
        String example = sample == null ? m.raw("menu.name-type.no-sample") : Icons.styled(text, sample, true);
        int unlocked = 0;
        for (ColorStyle style : styles) {
            if (plugin.colors().canUse(viewer, Channel.NAME, style)) unlocked++;
        }
        String path = "menu.name-type." + section.key();
        String[] ph = {
                "example", example,
                "sample", sample == null ? "" : icons.styleName(sample, true),
                "unlocked", String.valueOf(unlocked),
                "total", String.valueOf(styles.size())
        };
        Map<String, List<String>> lists = new LinkedHashMap<>();
        lists.put("gift", section == Section.NAME_ANIMATED ? rankGifts(styles) : List.of());
        lists.put("state", m.rawList(path + ".state-unlocked", ph));
        return Icons.item(material, m.line(path + ".name", ph), m.lines(path + ".lore", lists, ph), false);
    }

    /** Hiệu ứng động rank tặng sẵn: một dòng "ảnh rank - tên hiệu ứng" cho mỗi hiệu ứng nằm trong tiers.yml. */
    private List<String> rankGifts(List<ColorStyle> styles) {
        List<String> out = new ArrayList<>();
        for (ColorStyle style : styles) {
            if (plugin.tiers().unlockTier(Channel.NAME, style.id()) == null) continue;
            out.add(plugin.messages().raw("menu.name-type.gift-line",
                    "tier", icons.unlock(Channel.NAME, style), "style", icons.styleName(style, true)));
        }
        return out;
    }

    /** Màu đầu tiên người chơi dùng được, không có thì màu đầu tiên của loại đó. */
    private ColorStyle sample(List<ColorStyle> styles) {
        for (ColorStyle style : styles) {
            if (plugin.colors().canUse(viewer, Channel.NAME, style)) return style;
        }
        return styles.isEmpty() ? null : styles.get(0);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case SLOT_STATIC -> plugin.menus().open(player, new ColorMenu(plugin, player, Section.NAME_STATIC, 0));
            case SLOT_ANIMATED -> plugin.menus().open(player, new ColorMenu(plugin, player, Section.NAME_ANIMATED, 0));
            case SLOT_BACK -> plugin.menus().open(player, new MainMenu(plugin, player));
            default -> {
            }
        }
    }
}
