package vn.boxpvp.boxcolor.ui;

import java.util.List;
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
import vn.boxpvp.boxcolor.ui.ColorMenu.Section;

/**
 * Chọn loại màu (tĩnh hoặc động) của màu chat hoặc màu tên. Lore mỗi loại có chữ mẫu tô bằng một màu của loại đó.
 * <pre>
 *  B  B  B  B  B  B  B  B  B
 *  B  F [T] F  F  F [Đ] F  B      T = màu tĩnh (11), Đ = màu động (15)
 *  B  B  B  B [<] B  B  B  B      < = về trang chính (22)
 * </pre>
 */
public final class TypeMenu extends MenuView {

    private static final int SLOT_STATIC = 11;
    private static final int SLOT_ANIMATED = 15;
    private static final int SLOT_BACK = 22;

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Channel channel;
    private final Icons icons;

    public TypeMenu(BoxColorPlugin plugin, Player viewer, Channel channel) {
        super(27, plugin.messages().text("menu.type.title-" + channel.key()));
        this.plugin = plugin;
        this.viewer = viewer;
        this.channel = channel;
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
        Material staticIcon = channel == Channel.CHAT ? Material.WRITABLE_BOOK : Material.NAME_TAG;
        inventory.setItem(SLOT_STATIC, typeItem(Section.of(channel, false), staticIcon, data));
        inventory.setItem(SLOT_ANIMATED, typeItem(Section.of(channel, true), Material.NETHER_STAR, data));
    }

    private ItemStack typeItem(Section section, Material material, PlayerData data) {
        Messages m = plugin.messages();
        List<ColorStyle> styles = section.styles(plugin);
        ColorStyle sample = sample(styles);
        String text = icons.previewText(channel, data.displayText());
        String example = sample == null ? m.raw("menu.type.no-sample") : Icons.styled(text, sample, true);
        Feature feature = ColorService.animatedFeature(channel);
        boolean locked = section.animated() && !plugin.colors().hasFeature(viewer, feature);
        int unlocked = 0;
        for (ColorStyle style : styles) {
            if (plugin.colors().canUse(viewer, channel, style)) unlocked++;
        }
        String path = "menu.type." + section.key();
        String[] ph = {
                "example", example,
                "unlocked", String.valueOf(unlocked),
                "total", String.valueOf(styles.size()),
                "tier", icons.featureUnlock(feature)
        };
        return Icons.item(material, m.line(path + ".name", ph),
                m.lines(path + ".lore", "state", m.rawList("menu.type.state-" + (locked ? "locked" : "unlocked"), ph),
                        ph),
                false);
    }

    /** Màu đầu tiên người chơi dùng được, không có thì màu đầu tiên của loại đó. */
    private ColorStyle sample(List<ColorStyle> styles) {
        for (ColorStyle style : styles) {
            if (plugin.colors().canUse(viewer, channel, style)) return style;
        }
        return styles.isEmpty() ? null : styles.get(0);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        switch (slot) {
            case SLOT_STATIC -> plugin.menus().open(player, new ColorMenu(plugin, player, Section.of(channel, false), 0));
            case SLOT_ANIMATED -> {
                Feature feature = ColorService.animatedFeature(channel);
                if (!plugin.colors().hasFeature(player, feature)) {
                    plugin.messages().send(player, "color.animated-locked",
                            "channel", icons.channelName(channel), "tier", icons.featureUnlock(feature));
                    return;
                }
                plugin.menus().open(player, new ColorMenu(plugin, player, Section.of(channel, true), 0));
            }
            case SLOT_BACK -> plugin.menus().open(player, new MainMenu(plugin, player));
            default -> {
            }
        }
    }
}
