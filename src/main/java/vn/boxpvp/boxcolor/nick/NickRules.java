package vn.boxpvp.boxcolor.nick;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Luật đặt nick (không cần server, test được). Nick chỉ là chữ, không chứa mã màu: màu do màu tên quyết định.
 */
public final class NickRules {

    public enum Result {
        OK,
        TOO_SHORT,
        TOO_LONG,
        BAD_CHARS,
        NO_LETTERS,
        BLOCKED
    }

    private static final Pattern VIETNAMESE = Pattern.compile("[\\p{L}\\p{M}0-9_ ]+");
    private static final Pattern ASCII = Pattern.compile("[A-Za-z0-9_ ]+");
    private static final Pattern NO_SPACES_VIETNAMESE = Pattern.compile("[\\p{L}\\p{M}0-9_]+");
    private static final Pattern NO_SPACES_ASCII = Pattern.compile("[A-Za-z0-9_]+");

    private final int minLength;
    private final int maxLength;
    private final Pattern allowed;
    /** Đã chuẩn hoá bằng {@link #key}. */
    private final List<String> blocked;

    public NickRules(int minLength, int maxLength, boolean allowSpaces, boolean allowVietnamese,
            List<String> blockedWords) {
        this.minLength = Math.max(1, minLength);
        this.maxLength = Math.max(this.minLength, maxLength);
        this.allowed = allowSpaces
                ? (allowVietnamese ? VIETNAMESE : ASCII)
                : (allowVietnamese ? NO_SPACES_VIETNAMESE : NO_SPACES_ASCII);
        List<String> keys = new ArrayList<>();
        for (String word : blockedWords) {
            String k = key(word);
            if (!k.isEmpty()) keys.add(k);
        }
        this.blocked = List.copyOf(keys);
    }

    public int minLength() {
        return minLength;
    }

    public int maxLength() {
        return maxLength;
    }

    /** Bỏ khoảng trắng hai đầu và gộp nhiều dấu cách liền nhau thành một. */
    public static String clean(String input) {
        return Normalizer.normalize(input.trim().replaceAll("\\s+", " "), Normalizer.Form.NFC);
    }

    /** Kiểm tra nick đã qua {@link #clean}. */
    public Result validate(String nick) {
        if (!allowed.matcher(nick).matches()) return Result.BAD_CHARS;
        int length = nick.codePointCount(0, nick.length());
        if (length < minLength) return Result.TOO_SHORT;
        if (length > maxLength) return Result.TOO_LONG;
        String k = key(nick);
        if (k.isEmpty()) return Result.NO_LETTERS;
        for (String word : blocked) {
            if (k.contains(word)) return Result.BLOCKED;
        }
        return Result.OK;
    }

    /**
     * Khoá so trùng, chống giả mạo: bỏ dấu tiếng Việt, chữ thường, đổi số/chữ dễ nhầm về một dạng
     * ({@code 0→o, 1/i→l, 3→e, 4→a, 5→s, 7→t}), bỏ dấu cách và {@code _}. "Ádm1n_" và "admin" cùng khoá.
     */
    public static String key(String s) {
        String lower = s.toLowerCase(Locale.ROOT).replace('đ', 'd');
        String plain = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        StringBuilder sb = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            char mapped = switch (c) {
                case '0' -> 'o';
                case '1', 'i' -> 'l';
                case '3' -> 'e';
                case '4' -> 'a';
                case '5' -> 's';
                case '7' -> 't';
                default -> c;
            };
            if ((mapped >= 'a' && mapped <= 'z') || (mapped >= '0' && mapped <= '9')) sb.append(mapped);
        }
        return sb.toString();
    }
}
