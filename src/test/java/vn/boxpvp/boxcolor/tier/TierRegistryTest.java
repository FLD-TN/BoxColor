package vn.boxpvp.boxcolor.tier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.boxpvp.boxcolor.style.Channel;
import vn.boxpvp.boxcolor.style.StyleKind;
import vn.boxpvp.boxcolor.style.StyleRegistry;

/** Đọc đúng colors.yml và tiers.yml mặc định trong jar. */
class TierRegistryTest {

    private static StyleRegistry styles;
    private static TierRegistry tiers;
    private static final List<String> warnings = new ArrayList<>();

    private static YamlConfiguration resource(String name) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                TierRegistryTest.class.getResourceAsStream("/" + name), StandardCharsets.UTF_8));
    }

    @BeforeAll
    static void load() {
        styles = StyleRegistry.load(resource("colors.yml"), warnings::add);
        tiers = TierRegistry.load(resource("tiers.yml"), styles, warnings::add);
    }

    @Test
    void defaultsLoadWithoutWarnings() {
        assertEquals(List.of(), warnings);
        assertEquals(32, styles.count(Channel.CHAT));
        assertEquals(36, styles.count(Channel.NAME));
        assertEquals(9, tiers.all().size());
        assertEquals(StyleKind.RAINBOW, styles.get(Channel.NAME, "cau_vong_dong").kind());
        assertTrue(styles.get(Channel.NAME, "cau_vong_dong").animated());
    }

    @Test
    void higherTierIncludesLowerTiers() {
        assertTrue(tiers.allows(1, Channel.CHAT, "hong_pastel"));
        assertFalse(tiers.allows(1, Channel.CHAT, "oai_huong"));
        assertTrue(tiers.allows(3, Channel.CHAT, "hong_pastel"));
        assertTrue(tiers.allows(3, Channel.CHAT, "oai_huong"));
        assertFalse(tiers.allows(8, Channel.NAME, "ba_chu"));
        assertTrue(tiers.allows(9, Channel.NAME, "ba_chu"));
        assertFalse(tiers.allows(0, Channel.CHAT, "hong_pastel"));
        assertTrue(tiers.allows(TierRegistry.ALL, Channel.NAME, "anything"));
    }

    @Test
    void featuresUnlockAtTheirTier() {
        assertFalse(tiers.hasFeature(0, Feature.NICK));
        assertTrue(tiers.hasFeature(1, Feature.NICK));
        assertFalse(tiers.hasFeature(6, Feature.CUSTOM_CHAT));
        assertTrue(tiers.hasFeature(7, Feature.CUSTOM_CHAT));
        assertFalse(tiers.hasFeature(7, Feature.ANIMATED_NAME));
        assertTrue(tiers.hasFeature(8, Feature.ANIMATED_NAME));
        // custom-name (1.3) đọc thành nick
        assertEquals(Feature.NICK, Feature.parse("custom-name"));
    }

    @Test
    void animatedNameColorsOnlyFromUltimate() {
        for (var style : styles.list(Channel.NAME)) {
            if (!style.animated()) continue;
            assertTrue(tiers.unlockTier(Channel.NAME, style.id()).level() >= 8, style.id());
        }
        assertEquals(8, tiers.unlockTier(Channel.NAME, "cuc_quang").level());
        assertEquals(9, tiers.unlockTier(Channel.NAME, "ba_chu").level());
    }

    @Test
    void currentTierIsHighestNotAbove() {
        assertNull(tiers.current(0));
        assertEquals(2, tiers.current(2).level());
        assertEquals(9, tiers.current(99).level());
    }

    @Test
    void badEntriesAreWarned() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("chat.bad.type", "sparkle");
        yml.set("chat.short.type", "gradient");
        yml.set("chat.short.colors", List.of("#FFFFFF"));
        yml.set("chat.ok.type", "solid");
        yml.set("chat.ok.colors", List.of("#FFFFFF"));
        List<String> w = new ArrayList<>();
        StyleRegistry s = StyleRegistry.load(yml, w::add);
        assertEquals(1, s.count(Channel.CHAT));
        assertEquals(2, w.size());

        YamlConfiguration t = new YamlConfiguration();
        t.set("tiers.1.chat-colors", List.of("ok", "missing"));
        t.set("tiers.1.features", List.of("nick", "fly"));
        t.set("tiers.abc.name", "x");
        TierRegistry r = TierRegistry.load(t, s, w::add);
        assertEquals(5, w.size());
        assertTrue(r.allows(1, Channel.CHAT, "ok"));
        assertTrue(r.hasFeature(1, Feature.NICK));
    }
}
