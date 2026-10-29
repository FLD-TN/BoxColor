package vn.boxpvp.boxcolor.nick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class NickInputTest {

    @Test
    void noCodes() {
        NickInput.Parsed p = NickInput.parse("Đức Anh");
        assertEquals("Đức Anh", p.text());
        assertFalse(p.hasColors());
    }

    @Test
    void hexCodesAnywhere() {
        NickInput.Parsed p = NickInput.parse("&#FF0000Đức #ffd166Anh");
        assertEquals("Đức Anh", p.text());
        assertEquals(List.of(0xFF0000, 0xFFD166), p.colors());
    }

    @Test
    void legacyCodes() {
        NickInput.Parsed p = NickInput.parse("&aSteve&b");
        assertEquals("Steve", p.text());
        assertEquals(List.of(0x55FF55, 0x55FFFF), p.colors());
    }

    @Test
    void badCodesStayInTextAndFailValidation() {
        // &l (in đậm) và mã hex thiếu số không phải mã màu: còn lại trong chữ, luật nick sẽ chặn
        assertEquals("&lSteve", NickInput.parse("&lSteve").text());
        assertEquals("&#FFFSteve", NickInput.parse("&#FFFSteve").text());
    }
}
