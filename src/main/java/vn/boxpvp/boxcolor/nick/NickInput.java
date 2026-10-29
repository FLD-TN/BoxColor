package vn.boxpvp.boxcolor.nick;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tách mã màu ra khỏi chữ người chơi gõ sau {@code /nick}: {@code &#RRGGBB}, {@code #RRGGBB} và mã cơ bản
 * {@code &0}..{@code &f}, đặt ở đâu cũng được. Màu áp cho cả nick (1 mã = màu đơn, nhiều mã = chuyển màu theo thứ tự).
 */
public final class NickInput {

    /** @param colors các màu theo thứ tự gõ, rỗng nếu không có mã nào */
    public record Parsed(String text, List<Integer> colors) {

        public boolean hasColors() {
            return !colors.isEmpty();
        }
    }

    private static final Pattern CODE = Pattern.compile("&?#([0-9A-Fa-f]{6})|&([0-9A-Fa-f])");

    /** Màu của mã cơ bản Minecraft {@code &0}..{@code &f}. */
    private static final int[] LEGACY = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private NickInput() {
    }

    public static Parsed parse(String input) {
        List<Integer> colors = new ArrayList<>();
        Matcher m = CODE.matcher(input);
        StringBuilder text = new StringBuilder(input.length());
        while (m.find()) {
            colors.add(m.group(1) != null
                    ? Integer.parseInt(m.group(1), 16)
                    : LEGACY[Character.digit(m.group(2).charAt(0), 16)]);
            m.appendReplacement(text, "");
        }
        m.appendTail(text);
        return new Parsed(text.toString(), List.copyOf(colors));
    }
}
