package vn.boxpvp.boxcolor.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.boxpvp.boxcolor.data.PlayerData;

class HexTest {

    @Test
    void parsesCommonForms() {
        assertEquals(0xFFB3C6, Hex.parse("#FFB3C6"));
        assertEquals(0xFFB3C6, Hex.parse("ffb3c6"));
        assertEquals(0xFFB3C6, Hex.parse("&#FFB3C6"));
        assertEquals(-1, Hex.parse("#FFF"));
        assertEquals(-1, Hex.parse("#GGGGGG"));
    }

    @Test
    void parsesLists() {
        assertEquals(List.of(0xFF0000, 0x00FF00), Hex.parseList("#FF0000, #00FF00"));
        assertEquals(List.of(0xFF0000, 0x00FF00), Hex.parseList("  #FF0000   00FF00 "));
        assertNull(Hex.parseList("#FF0000 nope"));
        assertNull(Hex.parseList("   "));
    }

    @Test
    void customSelectionRoundTrips() {
        ColorStyle custom = ColorStyle.custom(List.of(0xFFB3C6, 0xA0E7E5));
        assertEquals("#FFB3C6,#A0E7E5", custom.encodeCustom());
        ColorStyle back = PlayerData.resolve(custom.encodeCustom(), Channel.CHAT, null);
        assertEquals(custom, back);
        assertEquals(StyleKind.GRADIENT, back.kind());
        assertEquals(StyleKind.SOLID, PlayerData.resolve("#FFFFFF", Channel.NAME, null).kind());
    }
}
