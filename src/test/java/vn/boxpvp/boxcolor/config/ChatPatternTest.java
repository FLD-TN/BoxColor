package vn.boxpvp.boxcolor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Mẫu keep-plain và typed-color-patterns mặc định, khớp từ khoá InteractiveChat của server. */
class ChatPatternTest {

    private static Pattern fromJar(String key) {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(new InputStreamReader(
                ChatPatternTest.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
        List<String> warnings = new ArrayList<>();
        Pattern p = Settings.pattern(c.getStringList(key), key, warnings::add);
        assertEquals(List.of(), warnings);
        return p;
    }

    @Test
    void keepsInteractiveChatKeywordsAndMentions() {
        Pattern keep = fromJar("chat.keep-plain");
        for (String keyword : List.of("[item]", "[ITEM]", "[inv]", "[inventory]", "[e]", "[ender]", "[money]",
                "[m]", "[ah]", "[choden]", "[pos]", "[toado]", "[ping]", "[time]", "[gametime]", "@Steve")) {
            assertTrue(keep.matcher("xem " + keyword + " nè").find(), keyword);
        }
        assertFalse(keep.matcher("chào mọi người").find());
    }

    @Test
    void detectsTypedColors() {
        Pattern typed = fromJar("chat.typed-color-patterns");
        for (String msg : List.of("&ahi", "&#FF0000hi", "#FF0000hi", "[color=#FF0000]hi", "[font=uniform]hi",
                "<#FF0000>hi", "&lđậm")) {
            assertTrue(typed.matcher(msg).find(), msg);
        }
        assertFalse(typed.matcher("giá 50% & còn #1 server").find());
    }

    @Test
    void literalAndRegexEntriesAndBadRegex() {
        List<String> warnings = new ArrayList<>();
        Pattern p = Settings.pattern(List.of("[hand]", "regex:@\\S+", "regex:(unclosed"), "x", warnings::add);
        assertTrue(p.matcher("[HAND]").find());
        assertFalse(p.matcher("[hund]").find());
        assertTrue(p.matcher("@ai").find());
        assertEquals(1, warnings.size());
        assertNull(Settings.pattern(List.of(), "x", warnings::add));
    }
}
