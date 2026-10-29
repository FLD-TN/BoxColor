package vn.boxpvp.boxcolor.nick;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.boxpvp.boxcolor.nick.NickRules.Result;

class NickRulesTest {

    private final NickRules rules = new NickRules(3, 16, true, true, List.of("admin", "owner"));

    @Test
    void keyIgnoresAccentsCaseAndLookalikes() {
        assertEquals(NickRules.key("admin"), NickRules.key("Ádm1n_"));
        assertEquals(NickRules.key("Steve"), NickRules.key("st3ve"));
        assertEquals("ducanh", NickRules.key("Đức Anh"));
        assertEquals(NickRules.key("Il"), NickRules.key("ll"));
    }

    @Test
    void cleanCollapsesSpaces() {
        assertEquals("Đức Anh", NickRules.clean("  Đức    Anh "));
    }

    @Test
    void validatesLengthAndCharacters() {
        assertEquals(Result.OK, rules.validate("Đức Anh"));
        assertEquals(Result.TOO_SHORT, rules.validate("ab"));
        assertEquals(Result.TOO_LONG, rules.validate("abcdefghijklmnopq"));
        assertEquals(Result.BAD_CHARS, rules.validate("a&cbc"));
        assertEquals(Result.BAD_CHARS, rules.validate("<b>hi"));
        assertEquals(Result.NO_LETTERS, rules.validate("___"));
    }

    @Test
    void blocksWordsEvenDisguised() {
        assertEquals(Result.BLOCKED, rules.validate("Ádm1n Pro"));
        assertEquals(Result.BLOCKED, rules.validate("TheOwner"));
    }

    @Test
    void asciiWithoutSpacesWhenConfigured() {
        NickRules strict = new NickRules(3, 16, false, false, List.of());
        assertEquals(Result.BAD_CHARS, strict.validate("Đức"));
        assertEquals(Result.BAD_CHARS, strict.validate("ab cd"));
        assertEquals(Result.OK, strict.validate("Duc_Anh"));
    }
}
