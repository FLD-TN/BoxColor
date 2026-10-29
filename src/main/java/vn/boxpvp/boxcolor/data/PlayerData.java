package vn.boxpvp.boxcolor.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.style.Hex;
import vn.boxpvp.boxcolor.style.StyleRegistry;

/**
 * Lựa chọn của một người chơi. Phần "đã lưu" chỉ đổi trên main thread. Phần "đang hiện" ({@code shown*}) là kết quả
 * sau khi kiểm tra quyền: mất rank thì màu/nick tạm ẩn nhưng vẫn giữ trong dữ liệu, lên lại rank là hiện lại.
 * Các trường {@code volatile} vì placeholder có thể được đọc từ luồng khác (TAB, UnlimitedNameTags).
 */
public final class PlayerData {

    private final UUID uuid;
    private volatile String realName;

    /** Mã màu có sẵn, hoặc {@code #RRGGBB,#RRGGBB} khi tự chỉnh; {@code null} là chưa chọn. */
    private volatile String chatSelection;
    private volatile String nameSelection;
    private volatile String nick;
    private volatile long nickChangedAt;

    private volatile ColorStyle shownChat;
    private volatile ColorStyle shownName;
    private volatile String shownNick;
    private volatile int tierLevel;
    /** Màu tên theo rank (config.yml rank-name-colors), dùng khi chưa chọn màu tên; {@code null} nếu không có. */
    private volatile ColorStyle rankName;

    public PlayerData(UUID uuid, String realName) {
        this.uuid = uuid;
        this.realName = realName;
    }

    public UUID uuid() {
        return uuid;
    }

    public String realName() {
        return realName;
    }

    public void realName(String name) {
        this.realName = name;
    }

    public String selection(Channel channel) {
        return channel == Channel.CHAT ? chatSelection : nameSelection;
    }

    public void selection(Channel channel, String value) {
        if (channel == Channel.CHAT) chatSelection = value;
        else nameSelection = value;
    }

    public String nick() {
        return nick;
    }

    public void nick(String nick) {
        this.nick = nick;
    }

    public long nickChangedAt() {
        return nickChangedAt;
    }

    public void nickChangedAt(long millis) {
        this.nickChangedAt = millis;
    }

    public ColorStyle shown(Channel channel) {
        return channel == Channel.CHAT ? shownChat : shownName;
    }

    public void shown(Channel channel, ColorStyle style) {
        if (channel == Channel.CHAT) shownChat = style;
        else shownName = style;
    }

    public String shownNick() {
        return shownNick;
    }

    public void shownNick(String nick) {
        this.shownNick = nick;
    }

    /** Tên đang hiện (chưa tô màu): nick nếu được dùng, không thì tên thật. */
    public String displayText() {
        String n = shownNick;
        return n != null ? n : realName;
    }

    public int tierLevel() {
        return tierLevel;
    }

    public void tierLevel(int level) {
        this.tierLevel = level;
    }

    public ColorStyle rankName() {
        return rankName;
    }

    public void rankName(ColorStyle style) {
        this.rankName = style;
    }

    /** Kiểu màu của một lựa chọn đã lưu; {@code null} nếu chưa chọn hoặc màu đã bị xoá khỏi colors.yml. */
    public static ColorStyle resolve(String selection, Channel channel, StyleRegistry styles) {
        if (selection == null || selection.isEmpty()) return null;
        if (selection.startsWith(ColorStyle.PER_CHAR_PREFIX)) {
            String[] parts = selection.substring(ColorStyle.PER_CHAR_PREFIX.length()).split(",");
            List<Integer> rgb = new ArrayList<>(parts.length);
            for (String part : parts) {
                if (part.equals("-")) {
                    rgb.add(ColorStyle.NO_COLOR);
                    continue;
                }
                int c = Hex.parse(part);
                if (c < 0) return null;
                rgb.add(c);
            }
            return rgb.isEmpty() ? null : ColorStyle.perChar(rgb);
        }
        if (selection.startsWith("#")) {
            List<Integer> rgb = Hex.parseList(selection);
            return rgb == null ? null : ColorStyle.custom(rgb);
        }
        return styles.get(channel, selection);
    }
}
