package vn.boxpvp.boxcolor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import vn.boxpvp.boxcolor.style.StyleKind;

/** Chỉ test phần rank-name-colors: Settings.load cần server thật (Material.isItem). */
class SettingsTest {

    private static YamlConfiguration jarConfig() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                SettingsTest.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
    }

    @Test
    void rankColorsInFileOrder() {
        List<String> warnings = new ArrayList<>();
        List<Settings.RankColor> ranks = Settings.rankColors(jarConfig(), warnings::add);
        assertEquals(List.of(), warnings);
        assertEquals(14, ranks.size());
        assertEquals("group.admin", ranks.get(0).permission());
        assertEquals(StyleKind.GRADIENT, ranks.get(0).style().kind());
        assertEquals("group.default", ranks.get(13).permission());
        assertEquals(List.of(0xFF7C12, 0xF3C890), ranks.get(8).style().colors());
    }

    @Test
    void oldFileWithoutKeyUsesJarTable() {
        YamlConfiguration old = new YamlConfiguration();
        old.set("default-name-color", "#FFFFFF");
        old.setDefaults(jarConfig());
        assertEquals(14, Settings.rankColors(old, w -> { }).size());
    }

    @Test
    void emptyTableDisablesRankColors() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.createSection("rank-name-colors");
        cfg.setDefaults(jarConfig());
        assertEquals(0, Settings.rankColors(cfg, w -> { }).size());
    }

    @Test
    void badCodeIsWarned() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("rank-name-colors.vip", List.of("#ZZZZZZ"));
        cfg.set("rank-name-colors.elite", List.of("#82F1D9"));
        List<String> warnings = new ArrayList<>();
        assertEquals(1, Settings.rankColors(cfg, warnings::add).size());
        assertEquals(1, warnings.size());
    }
}
