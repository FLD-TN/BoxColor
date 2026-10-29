package vn.boxpvp.boxcolor.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageTest {

    @TempDir
    Path dir;

    @Test
    void saveLoadAndNickIndex() throws Exception {
        Storage storage = new Storage();
        storage.open(new File(dir.toFile(), "data.db"));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertNull(storage.load(a));

        storage.save(new Storage.Row(a, "hong_pastel", "#FF0000,#0000FF", "cuc_quang", "Đức Anh", "dlcanh", 42L));
        storage.save(new Storage.Row(b, null, "lua_chay", null, null, null, 0L));
        Storage.Row row = storage.load(a);
        assertEquals("hong_pastel", row.chat());
        assertEquals("#FF0000,#0000FF", row.name());
        assertEquals("cuc_quang", row.decor());
        assertEquals("Đức Anh", row.nick());
        assertEquals(42L, row.nickChangedAt());
        assertEquals(Map.of("dlcanh", a), storage.nickKeys());

        // Lưu đè
        storage.save(new Storage.Row(a, null, null, null, "Mới", "mol", 50L));
        assertEquals("Mới", storage.load(a).nick());
        storage.clearNick(a);
        assertNull(storage.nickOf(a));
        assertEquals(Map.of(), storage.nickKeys());
        storage.close();
    }
}
