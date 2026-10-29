package vn.boxpvp.boxcolor.nick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.boxpvp.boxcolor.style.ColorStyle;

class NickInputTest {

    private static final int NONE = ColorStyle.NO_COLOR;

    @Test
    void noCodes() {
        NickInput.Parsed p = NickInput.parse("Đức Anh");
        assertEquals("Đức Anh", p.text());
        assertFalse(p.hasColors());
    }

    @Test
    void bungeeHexPerCharacter() {
        // Ví dụ của chủ server
        NickInput.Parsed p = NickInput.parse("&x&6&7&D&A&F&8D&x&6&A&D&9&F&5e&x&6&D&D&8&F&1p&x&7&0&D&7&E&ET"
                + "&x&7&3&D&6&E&Ar&x&7&6&D&6&E&7a&x&7&9&D&5&E&3i&x&7&C&D&4&E&01&x&7&F&D&3&D&C0&x&8&2&D&2&D&92");
        assertEquals("DepTrai102", p.text());
        assertEquals(List.of(0x67DAF8, 0x6AD9F5, 0x6DD8F1, 0x70D7EE, 0x73D6EA, 0x76D6E7, 0x79D5E3, 0x7CD4E0,
                0x7FD3DC, 0x82D2D9), p.colors());
    }

    @Test
    void codeColorsFollowingCharsUntilNextCode() {
        NickInput.Parsed p = NickInput.parse("#FFD166Đức #FF8FABAnh");
        assertEquals("Đức Anh", p.text());
        assertEquals(List.of(0xFFD166, 0xFFD166, 0xFFD166, 0xFF8FAB, 0xFF8FAB, 0xFF8FAB), p.colors());
    }

    @Test
    void legacyResetAndFormats() {
        NickInput.Parsed p = NickInput.parse("ab&a&lcd&rE§#00FF00f");
        assertEquals("abcdEf", p.text());
        assertEquals(List.of(NONE, NONE, 0x55FF55, 0x55FF55, NONE, 0x00FF00), p.colors());
    }

    @Test
    void incompleteCodesStayInText() {
        assertEquals("&#FFFSteve", NickInput.parse("&#FFFSteve").text());
        // &1 là mã màu hợp lệ nên bị tách, "&x" thừa còn lại và luật nick sẽ chặn
        assertEquals("&xSteve", NickInput.parse("&x&1Steve").text());
    }

    @Test
    void perCharSelectionRoundTrips() {
        ColorStyle style = ColorStyle.perChar(List.of(NONE, 0xFF0000, 0x00FF00));
        assertEquals("~-,#FF0000,#00FF00", style.encodeCustom());
        assertEquals(style, vn.boxpvp.boxcolor.data.PlayerData.resolve(style.encodeCustom(),
                vn.boxpvp.boxcolor.style.Channel.NAME, null));
        assertEquals(0xFF0000, style.firstColor());
    }
}
