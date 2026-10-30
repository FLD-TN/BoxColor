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
import vn.boxpvp.boxcolor.style.Motion;
import vn.boxpvp.boxcolor.tier.Feature;
import vn.boxpvp.boxcolor.ui.ColorMenu.Section;

/**
 * Chọn kiểu chuyển động cho hiệu ứng tên động. Mỗi ô xem trước tên thật chạy hiệu ứng đang dùng theo kiểu đó
 * (chưa dùng hiệu ứng nào thì xem bằng hiệu ứng đầu tiên đã sở hữu).
 * <pre>
 *  B  B  B  B [I] B  B  B  B      I = đang dùng (4)
 *  B  F [1][2][3][4][5] F  B      11..15
 *  B  F [6][7] F [8][9] F  B      20, 21, 23, 24
 *  B  B  B  B [<] B  B  B  B      < = quay lại (31)
 * </pre>
 */
public final class MotionMenu extends MenuView {

    private static final int SLOT_INFO = 4;
    private static final int SLOT_BACK = 31;
    private static final int[] SLOTS = {11, 12, 13, 14, 15, 20, 21, 23, 24};
    private static final Map<Motion, Material> ICONS = Map.of(
            Motion.SIGNATURE, Material.NETHER_STAR,
            Motion.CLASSIC, Material.PAPER,
            Motion.WAVE, Material.PRISMARINE_SHARD,
            Motion.SHINE, Material.GLOWSTONE_DUST,
            Motion.PULSE, Material.REDSTONE,
            Motion.SPARK, Material.BLAZE_POWDER,
            Motion.WAVE_SHINE, Material.AMETHYST_SHARD,
            Motion.WAVE_SPARK, Material.FIREWORK_STAR,
            Motion.SHINE_SPARK, Material.GOLD_NUGGET);

    private final BoxColorPlugin plugin;
    private final Player viewer;
    private final Icons icons;
    private final Map<Integer, Motion> slotMotions = new LinkedHashMap<>();

    public MotionMenu(BoxColorPlugin plugin, Player viewer) {
        super(36, plugin.messages().text("menu.motion.title"));
        this.plugin = plugin;
        this.viewer = viewer;
        this.icons = new Icons(plugin);
        Motion[] motions = Motion.values();
        for (int i = 0; i < SLOTS.length && i < motions.length; i++) slotMotions.put(SLOTS[i], motions[i]);
    }

    @Override
    public void render() {
        drawFrame(icons);
        inventory.setItem(SLOT_BACK, icons.button(Material.ARROW, "menu.motion.back"));
        drawItems();
    }

    /** Xem trước chạy liên tục. */
    @Override
    public void refresh() {
        drawItems();
    }

    private void drawItems() {
        PlayerData data = plugin.cache().get(viewer.getUniqueId());
        if (data == null) return;
        ColorStyle sample = sample(data);
        Motion current = plugin.colors().motionOf(data);
        boolean unlocked = data.motionUnlocked();
        for (Map.Entry<Integer, Motion> e : slotMotions.entrySet()) {
            Motion motion = e.getValue();
            String state = motion == current ? "selected" : unlocked ? "unselected" : "locked";
            inventory.setItem(e.getKey(), motionItem(motion, state, sample, data));
        }
        inventory.setItem(SLOT_INFO, infoItem(data, sample, current));
    }

    /** Hiệu ứng đang dùng; chưa có thì hiệu ứng đầu tiên đã sở hữu; không sở hữu cái nào thì cái đầu tiên. */
    private ColorStyle sample(PlayerData data) {
        if (data.shownDecor() != null) return data.shownDecor();
        List<ColorStyle> styles = Section.NAME_ANIMATED.styles(plugin);
        ColorService.Access access = access(plugin, viewer);
        for (ColorStyle style : styles) {
            if (access.canUse(Channel.NAME, style)) return style;
        }
        return styles.isEmpty() ? null : styles.get(0);
    }

    private ItemStack motionItem(Motion motion, String state, ColorStyle sample, PlayerData data) {
        Messages m = plugin.messages();
        String path = "menu.motion." + motion.key();
        String preview = sample == null ? m.raw("menu.motion.no-sample")
                : icons.decor(data.realName(), sample, motion.apply(sample.effect()));
        String[] ph = {"preview", preview};
        Map<String, List<String>> lists = Map.of(
                "description", m.rawList(path + ".description"),
                "state", m.rawList("menu.motion.state-" + state));
        return Icons.item(ICONS.getOrDefault(motion, Material.PAPER), m.line(path + ".name", ph),
                m.lines("menu.motion.item.lore", lists, ph), state.equals("selected"));
    }

    private ItemStack infoItem(PlayerData data, ColorStyle sample, Motion current) {
        Messages m = plugin.messages();
        String[] ph = {
                "motion", m.raw("menu.motion." + current.key() + ".name"),
                "style", sample == null ? m.raw("menu.motion.no-sample")
                        : icons.decor(sample.display(), sample, current.apply(sample.effect()))
        };
        String path = !data.motionUnlocked() ? "menu.motion.info-locked"
                : data.shownDecor() != null ? "menu.motion.info" : "menu.motion.info-no-decor";
        return Icons.item(Material.CLOCK, m.line(path + ".name", ph), m.lines(path + ".lore", ph), false);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        if (slot == SLOT_BACK) {
            plugin.menus().open(player, new ColorMenu(plugin, player, Section.NAME_ANIMATED, 0));
            return;
        }
        Motion motion = slotMotions.get(slot);
        if (motion == null) return;
        PlayerData data = plugin.cache().get(player.getUniqueId());
        if (data == null || plugin.colors().motionOf(data) == motion) return;
        // Kiểm tra quyền thật lúc bấm (có thể vừa mất rank)
        if (!plugin.colors().hasFeature(player, Feature.MOTION)) {
            plugin.messages().send(player, "color.motion-locked");
            return;
        }
        plugin.colors().selectMotion(player, motion);
        drawItems();
    }
}
