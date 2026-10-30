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
        assertEquals(38, styles.count(Channel.CHAT));
        assertEquals(61, styles.count(Channel.NAME));
        assertEquals(9, tiers.all().size());
        assertEquals(StyleKind.RAINBOW, styles.get(Channel.NAME, "cau_vong_dong").kind());
        assertTrue(styles.get(Channel.NAME, "cau_vong_dong").animated());
    }

    @Test
    void higherTierIncludesLowerTiers() {
        // Màu chat miễn phí từ Master (tier 4), mỗi rank 1 màu
        assertFalse(tiers.allows(3, Channel.CHAT, "la_non"));
        assertTrue(tiers.allows(4, Channel.CHAT, "la_non"));
        assertFalse(tiers.allows(4, Channel.CHAT, "do_ruc"));
        assertTrue(tiers.allows(9, Channel.CHAT, "la_non"));
        assertFalse(tiers.allows(8, Channel.NAME, "ba_chu"));
        assertTrue(tiers.allows(9, Channel.NAME, "ba_chu"));
        assertFalse(tiers.allows(0, Channel.CHAT, "la_non"));
        assertTrue(tiers.allows(TierRegistry.ALL, Channel.NAME, "anything"));
    }

    @Test
    void featuresUnlockAtTheirTier() {
        assertFalse(tiers.hasFeature(0, Feature.NICK));
        assertTrue(tiers.hasFeature(1, Feature.NICK));
        // Không rank nào được tự chỉnh màu chat
        assertFalse(tiers.hasFeature(9, Feature.CUSTOM_CHAT));
        // Ai cũng vào được trang hiệu ứng động: không tier nào cần animated-name
        assertNull(tiers.featureTier(Feature.ANIMATED_NAME));
        // Tự chọn kiểu chuyển động từ Zeus
        assertFalse(tiers.hasFeature(6, Feature.MOTION));
        assertTrue(tiers.hasFeature(7, Feature.MOTION));
        // custom-name (1.3) đọc thành nick
        assertEquals(Feature.NICK, Feature.parse("custom-name"));
    }

    @Test
    void animatedNameColorsOnlyFromZeus() {
        for (var style : styles.list(Channel.NAME)) {
            if (!style.animated()) continue;
            // Hiệu ứng động theo rank chỉ từ Zeus; hiệu ứng crate không thuộc tier nào
            var tier = tiers.unlockTier(Channel.NAME, style.id());
            assertTrue(tier == null || tier.level() >= 7, style.id());
        }
        assertNull(tiers.unlockTier(Channel.NAME, "neon_dong"));
        assertFalse(tiers.allows(9, Channel.NAME, "neon_dong"));
        // Quà Zeus: Thần Sấm; Ultimate: Cực Quang; Cầu Vồng Động chỉ có ở crate
        assertEquals(7, tiers.unlockTier(Channel.NAME, "than_sam").level());
        assertEquals(8, tiers.unlockTier(Channel.NAME, "cuc_quang").level());
        assertNull(tiers.unlockTier(Channel.NAME, "cau_vong_dong"));
        assertEquals(9, tiers.unlockTier(Channel.NAME, "ba_chu").level());
    }

    @Test
    void chatColorsSplitBetweenRanksAndCrate() {
        int ranked = 0;
        for (var style : styles.list(Channel.CHAT)) {
            if (tiers.unlockTier(Channel.CHAT, style.id()) != null) ranked++;
        }
        assertEquals(6, ranked);
        for (var tier : tiers.all()) {
            assertEquals(tier.level() >= 4 ? 1 : 0, tier.chatColors().size(), "rank " + tier.level());
        }
        // Màu crate chỉ mở bằng permission lẻ, kể cả rank cao nhất
        assertFalse(tiers.allows(9, Channel.CHAT, "vu_tru"));
        assertFalse(tiers.allows(9, Channel.CHAT, "cuc_quang"));
        // Màu chat theo rank chỉ là màu đơn; màu trộn chỉ có ở crate
        for (var style : styles.list(Channel.CHAT)) {
            boolean inTier = tiers.unlockTier(Channel.CHAT, style.id()) != null;
            assertEquals(inTier, style.kind() == vn.boxpvp.boxcolor.style.StyleKind.SOLID, style.id());
        }
    }

    @Test
    void oneAnimatedEffectPerTopRank() {
        int zeus = 0;
        int ultimate = 0;
        int overlord = 0;
        for (var style : styles.list(Channel.NAME)) {
            if (!style.animated()) continue;
            var tier = tiers.unlockTier(Channel.NAME, style.id());
            if (tier == null) continue;
            if (tier.level() == 7) zeus++;
            else if (tier.level() == 8) ultimate++;
            else if (tier.level() == 9) overlord++;
            else throw new AssertionError("hiệu ứng động ở rank thấp: " + style.id());
        }
        assertEquals(1, zeus);
        assertEquals(1, ultimate);
        assertEquals(1, overlord);
    }

    @Test
    void everyAnimatedEffectHasSignatureMotion() {
        for (var style : styles.list(Channel.NAME)) {
            if (!style.animated()) continue;
            assertTrue(style.effect().base() != vn.boxpvp.boxcolor.style.Effect.Base.CLASSIC, style.id());
        }
        var baChu = styles.get(Channel.NAME, "ba_chu").effect();
        assertTrue(baChu.shine() && baChu.spark());
        assertEquals(0xFFF4C2, baChu.highlight());
        assertEquals(7, styles.get(Channel.NAME, "cau_vong_dong").effect().wave());
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
        // tiers.yml cũ không ghi motion: mặc định tier 7, không có tier 7 thì không ai có
        assertNull(r.featureTier(Feature.MOTION));
        assertTrue(r.hasFeature(1, Feature.NICK));
    }
}
