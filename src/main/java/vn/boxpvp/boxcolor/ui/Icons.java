package vn.boxpvp.boxcolor.ui;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.config.Messages;
import vn.boxpvp.boxcolor.hook.PapiText;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorRenderer;
import vn.boxpvp.boxcolor.style.ColorRenderer.HexFormat;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.style.Effect;
import vn.boxpvp.boxcolor.tier.Feature;
import vn.boxpvp.boxcolor.tier.Tier;

/** Dựng item và các đoạn chữ dùng chung cho mọi màn hình. */
public final class Icons {

    private final BoxColorPlugin plugin;
    private final Messages messages;

    public Icons(BoxColorPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
    }

    public static ItemStack item(Material material, Component name, List<Component> lore, boolean glint) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        if (glint) meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    /** Như {@link #item}, giáp da thì nhuộm theo {@code rgb}. */
    public static ItemStack dyed(Material material, int rgb, Component name, List<Component> lore, boolean glint) {
        ItemStack item = item(material, name, lore, glint);
        if (item.getItemMeta() instanceof LeatherArmorMeta leather) {
            leather.setColor(Color.fromRGB(rgb & 0xFFFFFF));
            item.setItemMeta(leather);
        }
        return item;
    }

    /** Ô kính trang trí: không tên, không tooltip khi rê chuột. */
    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack head(OfflinePlayer owner, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(owner);
            skull.displayName(name);
            skull.lore(lore);
            skull.addItemFlags(ItemFlag.values());
            item.setItemMeta(skull);
        }
        return item;
    }

    public ItemStack border() {
        return pane(plugin.settings().border());
    }

    public ItemStack filler() {
        return pane(plugin.settings().filler());
    }

    /** Nút đơn giản: tên và lore lấy từ {@code path.name} / {@code path.lore}. */
    public ItemStack button(Material material, String path, String... placeholders) {
        return item(material, messages.line(path + ".name", placeholders), messages.lines(path + ".lore", placeholders),
                false);
    }

    /** Chữ tô theo kiểu màu (mã {@code &#RRGGBB}, ghép được vào chuỗi messages.yml). */
    public static String styled(String text, ColorStyle style, boolean animated) {
        double phase = animated ? ColorRenderer.phase(style, System.currentTimeMillis()) : 0;
        return ColorRenderer.legacy(text, style, phase, HexFormat.AMPERSAND);
    }

    /** Chữ chạy hiệu ứng tên động theo kiểu chuyển động {@code effect}, như ở TAB và nametag. */
    public String decor(String text, ColorStyle style, Effect effect) {
        if (!plugin.settings().animationEnabled()) return styled(text, style, false);
        return ColorRenderer.legacy(text, style, effect, plugin.colors().frameNow(), HexFormat.AMPERSAND);
    }

    /** Tên kiểu màu tô bằng chính nó; kiểu tự chỉnh dùng {@code menu.custom-display}. */
    public String styleName(ColorStyle style, boolean animated) {
        String text = style.isCustom() ? messages.raw("menu.custom-display") : style.display();
        return styled(text, style, animated);
    }

    public String kind(ColorStyle style) {
        String base = messages.raw("menu.kind." + style.kind().name().toLowerCase(Locale.ROOT));
        return style.animated() ? base + messages.raw("menu.kind.animated-suffix") : base;
    }

    /**
     * "Rank Y" của tier mở màu này. Không tier nào có thì {@code unlock-text} của màu trong colors.yml, không có nữa
     * thì {@code menu.unlock-special} (quay crate hoặc mua).
     */
    public String unlock(Channel channel, ColorStyle style) {
        Tier tier = plugin.tiers().unlockTier(channel, style.id());
        if (tier != null) return PapiText.apply(tier.name());
        String text = plugin.styles().unlockText(channel, style.id());
        return text != null ? text : messages.raw("menu.unlock-special");
    }

    public String featureUnlock(Feature feature) {
        Tier tier = plugin.tiers().featureTier(feature);
        return tier != null ? PapiText.apply(tier.name()) : messages.raw("menu.unlock-special");
    }

    public String channelName(Channel channel) {
        return messages.raw("menu.channel." + channel.key());
    }

    public String yesNo(boolean yes) {
        return messages.raw(yes ? "menu.unlocked" : "menu.locked");
    }

    /** Câu mẫu xem trước: tên người chơi (màu tên) hoặc câu chat mẫu (màu chat). */
    public String previewText(Channel channel, String displayName) {
        return channel == Channel.NAME ? displayName : plugin.settings().chatPreview();
    }
}
