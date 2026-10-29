package vn.boxpvp.boxcolor.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SQLite {@code data.db}, một kết nối. Mọi phương thức chặn luồng: gọi qua {@link PlayerCache} (luồng riêng),
 * trừ lúc bật/tắt plugin.
 */
public final class Storage {

    /** Một dòng dữ liệu, chụp lại trên main thread trước khi ghi ở luồng khác. */
    /** {@code decor}: hiệu ứng tên động cho TAB và nametag (mã màu động), {@code null} nếu không dùng. */
    public record Row(UUID uuid, String chat, String name, String decor, String nick, String nickKey,
            long nickChangedAt) {
    }

    private Connection connection;

    public synchronized void open(File file) throws SQLException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("CREATE TABLE IF NOT EXISTS players ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "chat TEXT, "
                    + "name TEXT, "
                    + "nick TEXT, "
                    + "nick_key TEXT, "
                    + "nick_changed INTEGER NOT NULL DEFAULT 0)");
            st.execute("CREATE INDEX IF NOT EXISTS players_nick_key ON players(nick_key)");
            // data.db của bản cũ chưa có cột decor
            boolean hasDecor = false;
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(players)")) {
                while (rs.next()) {
                    if ("decor".equalsIgnoreCase(rs.getString("name"))) hasDecor = true;
                }
            }
            if (!hasDecor) st.execute("ALTER TABLE players ADD COLUMN decor TEXT");
        }
    }

    public synchronized void close() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException ignored) {
            // đang tắt plugin
        }
        connection = null;
    }

    /** @return {@code null} nếu người này chưa có dữ liệu */
    public synchronized Row load(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT chat, name, decor, nick, nick_key, nick_changed FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Row(uuid, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getLong(6));
            }
        }
    }

    public synchronized void save(Row row) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO players (uuid, chat, name, decor, nick, nick_key, nick_changed) VALUES (?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT(uuid) DO UPDATE SET chat = excluded.chat, name = excluded.name, "
                        + "decor = excluded.decor, nick = excluded.nick, nick_key = excluded.nick_key, "
                        + "nick_changed = excluded.nick_changed")) {
            ps.setString(1, row.uuid().toString());
            ps.setString(2, row.chat());
            ps.setString(3, row.name());
            ps.setString(4, row.decor());
            ps.setString(5, row.nick());
            ps.setString(6, row.nickKey());
            ps.setLong(7, row.nickChangedAt());
            ps.executeUpdate();
        }
    }

    /** Xoá nick của một người (kể cả đang offline). */
    public synchronized void clearNick(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE players SET nick = NULL, nick_key = NULL WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    /** Khoá nick → chủ nick, của mọi người chơi (để so trùng cả với người đang offline). */
    public synchronized Map<String, UUID> nickKeys() throws SQLException {
        Map<String, UUID> out = new HashMap<>();
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery("SELECT uuid, nick_key FROM players WHERE nick_key IS NOT NULL")) {
            while (rs.next()) {
                try {
                    out.put(rs.getString(2), UUID.fromString(rs.getString(1)));
                } catch (IllegalArgumentException ignored) {
                    // dòng hỏng, bỏ qua
                }
            }
        }
        return out;
    }

    /** Nick của một người đang offline (cho /realname), {@code null} nếu không có. */
    public synchronized String nickOf(UUID uuid) throws SQLException {
        Row row = load(uuid);
        return row == null ? null : row.nick();
    }
}
