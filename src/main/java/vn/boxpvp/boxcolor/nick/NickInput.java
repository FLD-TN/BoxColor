package vn.boxpvp.boxcolor.nick;

import java.util.ArrayList;
import java.util.List;
import vn.boxpvp.boxcolor.style.ColorStyle;

/**
 * Đọc tên người chơi gõ cho "Màu tùy chỉnh" / {@code /nick}, theo đúng cách Minecraft đọc mã màu: mỗi mã đổi màu
 * cho các chữ phía sau nó, tới khi gặp mã khác. Nhận {@code &x&R&R&G&G&B&B}, {@code &#RRGGBB}, {@code #RRGGBB},
 * {@code &0}..{@code &f} (cả dạng {@code §}), {@code &r} để bỏ màu. Mã định dạng {@code &k}..{@code &o} bị bỏ qua.
 */
public final class NickInput {

    /**
     * @param text   chữ đã bỏ hết mã
     * @param colors màu của từng chữ nhìn thấy (không tính dấu cách), {@link ColorStyle#NO_COLOR} nếu chữ đó chưa có
     *               mã nào phía trước; rỗng nếu người chơi không gõ mã màu nào
     */
    public record Parsed(String text, List<Integer> colors) {

        public boolean hasColors() {
            return !colors.isEmpty();
        }
    }

    /** Màu của mã cơ bản Minecraft {@code &0}..{@code &f}. */
    private static final int[] LEGACY = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private NickInput() {
    }

    public static Parsed parse(String input) {
        StringBuilder text = new StringBuilder(input.length());
        List<Integer> colors = new ArrayList<>();
        boolean anyCode = false;
        int current = ColorStyle.NO_COLOR;
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            boolean prefix = c == '&' || c == '§';
            // &x&R&R&G&G&B&B
            if (prefix && isBungeeHex(input, i)) {
                current = bungeeHex(input, i);
                anyCode = true;
                i += 14;
                continue;
            }
            // &#RRGGBB
            if (prefix && i + 1 < input.length() && input.charAt(i + 1) == '#' && isHex(input, i + 2, 6)) {
                current = Integer.parseInt(input.substring(i + 2, i + 8), 16);
                anyCode = true;
                i += 8;
                continue;
            }
            // #RRGGBB
            if (c == '#' && isHex(input, i + 1, 6)) {
                current = Integer.parseInt(input.substring(i + 1, i + 7), 16);
                anyCode = true;
                i += 7;
                continue;
            }
            if (prefix && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                int digit = Character.digit(code, 16);
                if (digit >= 0) {
                    current = LEGACY[digit];
                    anyCode = true;
                    i += 2;
                    continue;
                }
                if (code == 'r') {
                    current = ColorStyle.NO_COLOR;
                    anyCode = true;
                    i += 2;
                    continue;
                }
                if (code >= 'k' && code <= 'o') {
                    i += 2;
                    continue;
                }
            }
            int cp = input.codePointAt(i);
            text.appendCodePoint(cp);
            if (!Character.isWhitespace(cp)) colors.add(current);
            i += Character.charCount(cp);
        }
        return new Parsed(text.toString(), anyCode ? List.copyOf(colors) : List.of());
    }

    /** {@code &x&R&R&G&G&B&B} bắt đầu tại {@code i}. */
    private static boolean isBungeeHex(String s, int i) {
        if (i + 14 > s.length()) return false;
        if (Character.toLowerCase(s.charAt(i + 1)) != 'x') return false;
        for (int k = 0; k < 6; k++) {
            char p = s.charAt(i + 2 + k * 2);
            if (p != '&' && p != '§') return false;
            if (Character.digit(s.charAt(i + 3 + k * 2), 16) < 0) return false;
        }
        return true;
    }

    private static int bungeeHex(String s, int i) {
        int rgb = 0;
        for (int k = 0; k < 6; k++) rgb = rgb * 16 + Character.digit(s.charAt(i + 3 + k * 2), 16);
        return rgb;
    }

    private static boolean isHex(String s, int from, int count) {
        if (from + count > s.length()) return false;
        for (int k = 0; k < count; k++) {
            if (Character.digit(s.charAt(from + k), 16) < 0) return false;
        }
        return true;
    }
}
