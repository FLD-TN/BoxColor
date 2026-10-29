# Changelog

## 1.3.0 (2026-10-29)

Tương thích InteractiveChat:
- `chat.keep-plain` nhận regex (dòng `regex:`). Mặc định giữ nguyên mọi từ khoá dạng `[chữ]` ([item], [inv], [e],
  [money], [ah], [pos]...) và tag `@tên`, để IC vẫn nhận ra.
- `chat.keep-player-names`: giữ nguyên tên thật và nick của người đang online trong tin nhắn, để IC gắn hover/click.
- `chat.typed-color-patterns`: tin nhắn có mã `&`, hex, `[color=...]`, `[font=...]` được coi là người chơi tự gõ màu,
  BoxColor không tô đè.

## 1.2.0 (2026-10-29)

- `rank-name-colors` trong config.yml: màu tên theo group LuckPerms, dùng khi người chơi chưa chọn màu tên.
  `%boxcolor_name%` hiện đúng màu rank ở TAB, nametag và chat; chọn màu trong GUI sẽ đè lên.
  Server đang chạy bản cũ (config.yml chưa có khoá này) tự dùng bảng mặc định trong jar.

## 1.1.0 (2026-10-29)

- `/nick` nhận mã màu ở bất kỳ đâu trong nick: `&#RRGGBB`, `#RRGGBB`, `&0`-`&f`. 1 mã = màu đơn, nhiều mã = chuyển màu.
  Cần tính năng `custom-name`; màu được lưu như màu tên tự chỉnh. Gõ lại đúng nick cũ kèm mã màu chỉ đổi màu,
  không tính thời gian chờ. `/boxcolor nick` của admin cũng nhận mã màu.
- Bộ màu mẫu cho 9 rank donate: 27 màu chat, 27 màu tên.

## 1.0.0 (2026-10-29)

Bản đầu tiên.
- Màu chat và màu tên: màu đơn, chuyển màu, cầu vồng, màu chuyển động (chạy ở TAB, nametag và GUI).
- Mở khóa theo tier (`boxcolor.tier.<n>`, tier cao gồm tier thấp), permission lẻ cho quà sự kiện.
- Tự chỉnh mã hex (1 mã = màu đơn, 2-5 mã = chuyển màu), nhập trong chat và xem trước trước khi áp dụng.
- `/nick` tự do, có dấu tiếng Việt và dấu cách; chống trùng/giả mạo với nick và tên thật của mọi người chơi.
- GUI chung cho `/chatcolor` và `/namecolor`.
- Placeholder cho EzChat, TAB, UnlimitedNameTag; lưu SQLite.
