package vn.boxpvp.boxcolor.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import vn.boxpvp.boxcolor.style.ColorRenderer.HexFormat;

class ColorRendererTest {

    private static ColorStyle gradient(boolean animated, int... rgb) {
        List<Integer> colors = java.util.Arrays.stream(rgb).boxed().toList();
        return new ColorStyle("g", "G", StyleKind.GRADIENT, colors, animated, 1000L, 1f, 1f, null);
    }

    @Test
    void solidWritesOneCode() {
        ColorStyle red = ColorStyle.custom(List.of(0xFF0000));
        assertEquals("&#FF0000Steve", ColorRenderer.legacy("Steve", red, 0, HexFormat.AMPERSAND));
    }

    @Test
    void gradientStartsAndEndsOnStops() {
        ColorStyle g = gradient(false, 0xFF0000, 0x0000FF);
        assertEquals(0xFF0000, ColorRenderer.rgbAt(g, 0, 5, 0));
        assertEquals(0x0000FF, ColorRenderer.rgbAt(g, 4, 5, 0));
        assertEquals(0x800080, ColorRenderer.rgbAt(g, 2, 5, 0));
    }

    @Test
    void spacesKeepNoCodeAndDoNotCountInGradient() {
        ColorStyle g = gradient(false, 0xFF0000, 0x0000FF);
        String out = ColorRenderer.legacy("a b", g, 0, HexFormat.AMPERSAND);
        assertEquals("&#FF0000a &#0000FFb", out);
    }

    @Test
    void sectionAndMiniMessageFormats() {
        ColorStyle c = ColorStyle.custom(List.of(0x12AB34));
        assertEquals("§x§1§2§A§B§3§4x", ColorRenderer.legacy("x", c, 0, HexFormat.SECTION));
        assertEquals("<#12AB34>x", ColorRenderer.legacy("x", c, 0, HexFormat.MINIMESSAGE));
        assertEquals("<#12AB34>\\<", ColorRenderer.legacy("<", c, 0, HexFormat.MINIMESSAGE));
    }

    @Test
    void vietnameseCharactersStayIntact() {
        ColorStyle g = gradient(false, 0xFF0000, 0x00FF00);
        String out = ColorRenderer.legacy("Đức Anh", g, 0, HexFormat.AMPERSAND);
        assertEquals("Đức Anh", out.replaceAll("&#[0-9A-F]{6}", ""));
    }

    @Test
    void animatedPhaseShiftsColors() {
        ColorStyle g = gradient(true, 0xFF0000, 0x0000FF);
        assertEquals(0.5, ColorRenderer.phase(g, 1500L), 1e-9);
        assertNotEquals(ColorRenderer.rgbAt(g, 0, 4, 0), ColorRenderer.rgbAt(g, 0, 4, 0.25));
        // Tĩnh: pha luôn 0
        assertEquals(0, ColorRenderer.phase(gradient(false, 1, 2), 1500L));
    }

    @Test
    void rainbowIsSaturated() {
        ColorStyle rainbow = new ColorStyle("r", "R", StyleKind.RAINBOW, List.of(), false, 3000L, 1f, 1f, null);
        assertEquals(0xFF0000, ColorRenderer.rgbAt(rainbow, 0, 6, 0));
    }

    @Test
    void componentKeepsPlainTokens() {
        ColorStyle g = gradient(false, 0xFF0000, 0x0000FF);
        Component c = ColorRenderer.component("xem [item] nè", g, 0, Pattern.compile(Pattern.quote("[item]")));
        assertEquals("xem [item] nè", PlainTextComponentSerializer.plainText().serialize(c));
        assertTrue(c.children().stream().anyMatch(child -> child.color() == null
                && PlainTextComponentSerializer.plainText().serialize(child).equals("[item]")));
    }
}
