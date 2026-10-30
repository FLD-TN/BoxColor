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
    void classicMotionMatchesOldAnimation() {
        ColorStyle g = gradient(true, 0xFF0000, 0x0000FF);
        for (long now = 0; now < 1000; now += 100) {
            assertEquals(ColorRenderer.legacy("Steve_VN", g, ColorRenderer.phase(g, now), HexFormat.AMPERSAND),
                    ColorRenderer.legacy("Steve_VN", g, Effect.CLASSIC, now, HexFormat.AMPERSAND));
        }
    }

    @Test
    void waveRepeatsEveryWaveChars() {
        ColorStyle g = gradient(true, 0xFF0000, 0x0000FF);
        Effect wave = Motion.WAVE.apply(Effect.CLASSIC);
        assertEquals(ColorRenderer.rgbAt(g, wave, 0, 16, 300), ColorRenderer.rgbAt(g, wave, 5, 16, 300));
        assertNotEquals(ColorRenderer.rgbAt(g, wave, 0, 16, 300), ColorRenderer.rgbAt(g, wave, 2, 16, 300));
    }

    @Test
    void pulseColorsWholeNameAlmostTheSame() {
        ColorStyle g = gradient(true, 0xFF0000, 0x0000FF);
        Effect pulse = Motion.PULSE.apply(Effect.CLASSIC);
        String early = ColorRenderer.legacy("Steve", g, pulse, 0, HexFormat.AMPERSAND);
        String later = ColorRenderer.legacy("Steve", g, pulse, 500, HexFormat.AMPERSAND);
        assertNotEquals(early, later);
        assertTrue(early.startsWith("&#FF0000"), early);
    }

    @Test
    void shineSweepsThenRests() {
        ColorStyle g = gradient(true, 0xFF0000, 0xAA0000);
        Effect shine = Motion.SHINE.apply(Effect.CLASSIC);
        String rest1 = ColorRenderer.legacy("Steve_VN", g, shine, 700, HexFormat.AMPERSAND);
        String rest2 = ColorRenderer.legacy("Steve_VN", g, shine, 900, HexFormat.AMPERSAND);
        // Lúc nghỉ chuỗi đứng yên = gradient tĩnh: TAB/UNT không phải gửi lại
        assertEquals(rest1, rest2);
        assertEquals(ColorRenderer.legacy("Steve_VN", gradient(false, 0xFF0000, 0xAA0000), 0, HexFormat.AMPERSAND),
                rest1);
        String sweeping = ColorRenderer.legacy("Steve_VN", g, shine, 300, HexFormat.AMPERSAND);
        assertNotEquals(rest1, sweeping);
    }

    @Test
    void sparkIsSameForEveryViewerAndOnlyBrightens() {
        ColorStyle g = gradient(true, 0x112233, 0x112233);
        Effect spark = new Effect(Effect.Base.STATIC, 6, false, true, 0xFFFFFF, 0.5);
        boolean lit = false;
        for (long now = 0; now < 3000; now += 100) {
            assertEquals(ColorRenderer.rgbAt(g, spark, 3, 10, now), ColorRenderer.rgbAt(g, spark, 3, 10, now));
            int rgb = ColorRenderer.rgbAt(g, spark, 3, 10, now);
            assertTrue(((rgb >> 16) & 0xFF) >= 0x11, Integer.toHexString(rgb));
            if (rgb != 0x112233) lit = true;
        }
        assertTrue(lit);
    }

    @Test
    void motionKeysRoundTrip() {
        for (Motion m : Motion.values()) assertEquals(m, Motion.parse(m.key()));
        assertEquals(Motion.WAVE_SHINE, Motion.parse("WAVE_SHINE"));
        assertEquals(null, Motion.parse("nope"));
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
