package vn.boxpvp.boxcolor.hook;

import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import vn.boxpvp.boxcolor.BoxColorPlugin;
import vn.boxpvp.boxcolor.data.PlayerData;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorRenderer;
import vn.boxpvp.boxcolor.style.ColorRenderer.HexFormat;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.tier.Tier;

/**
 * Placeholder {@code %boxcolor_...%}. Có thể được gọi từ luồng khác (TAB, UnlimitedNameTags): chỉ đọc dữ liệu đã tính
 * sẵn trên main thread, không kiểm tra quyền ở đây.
 * <ul>
 *   <li>{@code name} (cũng là {@code name_static}): nick nếu có, màu tên tĩnh/tùy chỉnh. Cho chat và scoreboard</li>
 *   <li>{@code tab}: luôn là tên thật; hiệu ứng động nếu có, không thì màu rank. Cho TAB</li>
 *   <li>{@code nametag}: luôn là tên thật; hiệu ứng động nếu có, không thì nametag-default-color. Cho nametag</li>
 *   <li>{@code name_plain}: tên/nick không màu; {@code realname}: tên thật</li>
 *   <li>{@code chat}: mã màu đầu tiên của màu chat (đặt trước nội dung nếu tắt tô màu chat)</li>
 *   <li>{@code chatcolor}, {@code namecolor}: tên kiểu màu đang dùng; {@code tier}, {@code tier_level}</li>
 *   <li>{@code has_nick}: true/false</li>
 * </ul>
 * Thêm đuôi {@code _mm} vào {@code name}, {@code tab}, {@code nametag}, {@code chat} để lấy mã MiniMessage.
 */
public final class BoxColorExpansion extends PlaceholderExpansion {

    private final BoxColorPlugin plugin;

    public BoxColorExpansion(BoxColorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "boxcolor";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Dexterityy_";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        String key = params.toLowerCase(Locale.ROOT);
        HexFormat format = plugin.settings().placeholderFormat();
        if (key.endsWith("_mm")) {
            format = HexFormat.MINIMESSAGE;
            key = key.substring(0, key.length() - 3);
        }
        PlayerData data = plugin.cache().get(player.getUniqueId());
        String realName = player.getName() != null ? player.getName() : "";

        switch (key) {
            case "name":
            case "name_static":
                return data == null ? plugin.colors().fallbackName(realName, format)
                        : plugin.colors().name(data, format);
            case "tab":
                return data == null ? plugin.colors().fallbackName(realName, format)
                        : plugin.colors().tabName(data, format);
            case "nametag":
                return data == null
                        ? ColorRenderer.legacy(realName, plugin.settings().nametagDefault(), 0, format)
                        : plugin.colors().nametagName(data, format);
            case "name_plain":
                return data == null ? realName : data.displayText();
            case "realname":
                return realName;
            case "has_nick":
                return String.valueOf(data != null && data.shownNick() != null);
            case "chat": {
                ColorStyle style = data == null ? null : data.shown(Channel.CHAT);
                return style == null ? "" : ColorRenderer.code(style.firstColor(), format);
            }
            case "chatcolor":
            case "namecolor": {
                Channel channel = key.equals("chatcolor") ? Channel.CHAT : Channel.NAME;
                ColorStyle style = data == null ? null : data.shown(channel);
                if (style == null) return plugin.messages().raw("placeholder.none");
                String display = style.isCustom() ? plugin.messages().raw("menu.custom-display") : style.display();
                return ColorRenderer.legacy(display, style, 0, format);
            }
            case "tier": {
                Tier tier = data == null ? null : plugin.tiers().current(data.tierLevel());
                return tier == null ? plugin.messages().raw("placeholder.none") : PapiText.apply(tier.name());
            }
            case "tier_level":
                return data == null ? "0" : String.valueOf(Math.min(data.tierLevel(), 999));
            default:
                return null;
        }
    }
}
