package vn.boxpvp.boxcolor.style;

import java.util.ArrayList;
import java.util.List;

/** Đọc mã màu người chơi/admin nhập: {@code #FFB3C6}, {@code FFB3C6}, {@code &#FFB3C6}. */
public final class Hex {

    private Hex() {
    }

    /** @return RGB, hoặc -1 nếu không hợp lệ */
    public static int parse(String s) {
        if (s == null) return -1;
        String t = s.trim();
        if (t.startsWith("&")) t = t.substring(1);
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() != 6) return -1;
        try {
            return Integer.parseInt(t, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Danh sách mã cách nhau bởi dấu cách hoặc dấu phẩy.
     *
     * @return các mã đọc được, hoặc {@code null} nếu có mã sai hoặc danh sách rỗng
     */
    public static List<Integer> parseList(String input) {
        if (input == null) return null;
        List<Integer> out = new ArrayList<>();
        for (String part : input.trim().split("[\\s,]+")) {
            if (part.isEmpty()) continue;
            int rgb = parse(part);
            if (rgb < 0) return null;
            out.add(rgb);
        }
        return out.isEmpty() ? null : out;
    }
}
