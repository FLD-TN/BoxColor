package vn.boxpvp.boxcolor.shader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.ColorRenderer;
import vn.boxpvp.boxcolor.style.ColorStyle;
import vn.boxpvp.boxcolor.style.StyleRegistry;

/** Gán mã tín hiệu và sinh shader từ colors.yml mặc định. */
class ShaderGeneratorTest {

    private static final Set<Integer> RESERVED = Set.of(0xFFFFFF, 0xFFFFFE, 0xFF5555, 0xAAAAAA);

    private static StyleRegistry withMarkers(List<String> warnings) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                ShaderGeneratorTest.class.getResourceAsStream("/colors.yml"), StandardCharsets.UTF_8));
        return StyleRegistry.load(yml, warnings::add).withShaderMarkers(RESERVED, warnings::add);
    }

    @Test
    void markersAreUniqueAndAvoidEveryConfiguredColor() {
        List<String> warnings = new ArrayList<>();
        StyleRegistry styles = withMarkers(warnings);
        assertEquals(List.of(), warnings);
        List<ColorStyle> effects = styles.shaderStyles();
        assertEquals(14, effects.size());

        Set<Integer> configured = new HashSet<>(RESERVED);
        for (Channel channel : Channel.values()) {
            for (ColorStyle style : styles.list(channel)) configured.addAll(style.colors());
        }
        Set<Integer> markers = new HashSet<>();
        for (ColorStyle effect : effects) {
            assertTrue(markers.add(effect.marker()), "trùng mã " + effect.id());
            assertFalse(configured.contains(effect.marker()), "mã trùng màu có sẵn " + effect.id());
        }
    }

    @Test
    void markerStaysCloseToFirstColor() {
        StyleRegistry styles = withMarkers(new ArrayList<>());
        ColorStyle fire = styles.get(Channel.NAME, "lua_dong");
        int base = 0xFFD166;
        for (int shift : new int[] {16, 8, 0}) {
            int diff = Math.abs((fire.marker() >> shift & 0xFF) - (base >> shift & 0xFF));
            assertTrue(diff <= 15, "lệch quá xa ở kênh " + shift);
        }
    }

    @Test
    void sameConfigGivesSameMarkers() {
        StyleRegistry a = withMarkers(new ArrayList<>());
        StyleRegistry b = withMarkers(new ArrayList<>());
        for (ColorStyle effect : a.shaderStyles()) {
            assertEquals(effect.marker(), b.get(Channel.NAME, effect.id()) != null
                    ? b.get(Channel.NAME, effect.id()).marker() : b.get(Channel.CHAT, effect.id()).marker());
        }
    }

    @Test
    void shaderStyleRendersOneMarkerForEveryChar() {
        ColorStyle fire = withMarkers(new ArrayList<>()).get(Channel.NAME, "lua_dong");
        String legacy = ColorRenderer.legacy("Đức Anh", fire, 0.7, ColorRenderer.HexFormat.AMPERSAND);
        assertEquals("&#" + String.format("%06X", fire.marker()) + "Đức Anh", legacy);
        assertEquals(0, ColorRenderer.phase(fire, 12345L));
    }

    @Test
    void generatesAllVersionFilesAsciiOnly() {
        List<ColorStyle> effects = withMarkers(new ArrayList<>()).shaderStyles();
        Map<String, String> files = ShaderGenerator.generate(effects, 100);
        assertEquals(9, files.size());
        assertTrue(files.containsKey("assets/minecraft/shaders/core/rendertype_text.fsh"));
        assertTrue(files.containsKey("bc_1_21_2_plus/assets/minecraft/shaders/core/rendertype_text_see_through.json"));
        assertTrue(files.containsKey("bc_1_21_6_plus/assets/minecraft/shaders/core/rendertype_text.fsh"));
        String mcmeta = files.get("pack.mcmeta");
        assertTrue(mcmeta.contains("\"directory\":\"bc_1_21_2_plus\"") && mcmeta.contains("\"directory\":\"bc_1_21_6_plus\""));
        for (Map.Entry<String, String> file : files.entrySet()) {
            String content = file.getValue();
            if (file.getKey().equals("pack.mcmeta")) continue;
            assertTrue(content.chars().allMatch(c -> c < 128), "ký tự lạ trong " + file.getKey());
            if (file.getKey().endsWith(".fsh")) {
                assertTrue(content.startsWith("#version 150"), file.getKey());
                for (ColorStyle effect : effects) {
                    int m = effect.marker();
                    assertTrue(content.contains("ivec3(" + (m >> 16 & 0xFF) + ", " + (m >> 8 & 0xFF) + ", "
                            + (m & 0xFF) + ")"), file.getKey() + " thiếu " + effect.id());
                }
                assertEquals(count(content, '{'), count(content, '}'), "ngoặc lệch " + file.getKey());
            }
        }
        String modern = files.get("bc_1_21_6_plus/assets/minecraft/shaders/core/rendertype_text.fsh");
        assertTrue(modern.contains("apply_fog(") && modern.contains("globals.glsl"));
        assertFalse(modern.contains("uniform float GameTime"));
        String legacy = files.get("assets/minecraft/shaders/core/rendertype_text.fsh");
        assertTrue(legacy.contains("linear_fog(") && legacy.contains("uniform float GameTime;"));
        assertTrue(files.get("assets/minecraft/shaders/core/rendertype_text_see_through.json").contains("GameTime"));
    }

    @Test
    void zipIsDeterministic() throws Exception {
        Map<String, String> files = ShaderGenerator.generate(withMarkers(new ArrayList<>()).shaderStyles(), 100);
        assertEquals(ShaderPack.hex(ShaderPack.sha1(ShaderPack.zip(files))),
                ShaderPack.hex(ShaderPack.sha1(ShaderPack.zip(new java.util.TreeMap<>(files)))));
    }

    private static long count(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}
