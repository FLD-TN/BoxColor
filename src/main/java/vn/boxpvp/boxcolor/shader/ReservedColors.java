package vn.boxpvp.boxcolor.shader;

import java.util.HashSet;
import java.util.Set;
import vn.boxpvp.boxcolor.config.Settings;

/** Các màu không được dùng làm mã tín hiệu shader, để chữ thường không bị chạy màu nhầm. */
public final class ReservedColors {

    /** 16 màu cơ bản của Minecraft ({@code &0}..{@code &f}). */
    private static final int[] LEGACY = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    /** Mã hiệu ứng chữ mà shader của ItemsAdder 4.0 đang dùng (cầu vồng, gợn sóng, nhảy, nhấp nháy, ẩn chữ...). */
    private static final int[] ITEMSADDER = {
            0x131709, 0x393F3F, 0x393F3E, 0x393E3F, 0x393E3E, 0x393D3F, 0x393D3E, 0x4E5C24,
            0xE6FFFE, 0xE6FFFA, 0xE6FBFE, 0xE6FBFA, 0xE6F7FE, 0xE6F7FA,
            0xFFFFFE, 0xFFFFFD, 0xFFFF19, 0xFFFFFB, 0xFFFEFE
    };

    private ReservedColors() {
    }

    public static Set<Integer> of(Settings settings) {
        Set<Integer> out = new HashSet<>();
        for (int rgb : LEGACY) out.add(rgb);
        for (int rgb : ITEMSADDER) out.add(rgb);
        out.addAll(settings.defaultName().colors());
        for (Settings.RankColor rank : settings.rankNameColors()) out.addAll(rank.style().colors());
        return out;
    }
}
