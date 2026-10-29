# Changelog

## 1.7.1 (2026-10-29)

- Menu chọn loại màu tên ghi rõ hơn: Màu tên động hiện ở danh sách người chơi (phím Tab) và trên đầu nhân vật,
  trong chat đứng yên, yêu cầu Ultimate trở lên, chỉ chọn màu có sẵn. Màu tên tĩnh nhắc nút Màu tùy chỉnh (/nick).

## 1.7.0 (2026-10-29)

- Bỏ toàn bộ phần shader / resource pack của 1.5.0 - 1.6.0 (đã revert). Màu động chạy ở TAB, scoreboard, nametag;
  chat là màu tĩnh, như 1.4.0.
- Màu chat: mỗi rank 2 màu; 27 màu đặc sắc (9 màu cũ + 18 màu mới) không thuộc tier nào, chỉ mở bằng permission
  `boxcolor.color.chat.<mã>`. Bỏ tự chỉnh mã màu chat ở mọi rank (chỉ còn với quyền lẻ `boxcolor.feature.custom-chat`).
- Phiếu màu: `/boxcolor give <người chơi> <chat|name> <mã> [số lượng]`, chuột phải để nhận permission
  (`voucher.grant-command`), không trừ phiếu khi đã sở hữu.
- GUI: lore "Đã sở hữu / Chưa sở hữu", bộ lọc Tất cả / Đã sở hữu / Chưa sở hữu; `unlock-text` riêng từng màu trong
  colors.yml.

## 1.4.0 (2026-10-29)

- Màu tên tách hai trang: **Màu tĩnh** và **Màu động**, chọn từ menu mới sau ô Màu tên. Lore mỗi loại có tên mẫu.
- **Màu tùy chỉnh** gộp "Tự chỉnh mã màu" và "Đổi nick" (tính năng `nick`): mã màu đọc như Minecraft, mỗi mã tô các
  chữ phía sau nó (`&x&R&R&G&G&B&B`, `&#RRGGBB`, `#RRGGBB`, `&0`-`&f`, `&r`). Không gõ mã thì chỉ đổi chữ, giữ màu.
  Cách gõ `#mã` của 1.1.0 (gradient qua cả tên) không còn.
- Trang Màu động cần tính năng `animated-name`; ô "Đổi chữ của tên" đặt tên cho màu động chạy theo.
- `custom-name` trong tiers.yml được đọc thành `nick`. `/namecolor custom` chuyển người chơi sang `/nick`.
- Mặc định bỏ từ cấm và thời gian chờ đổi tên, vẫn chặn trùng tên người khác.
- Bộ màu mẫu: Màu tùy chỉnh từ Elite, màu động chỉ từ Ultimate (5 màu) và Overlord (4 màu); 9 màu tĩnh mới bù cho
  các rank bị chuyển màu động đi.

## 1.3.1 (2026-10-29)

- `animation.enabled`: tắt toàn bộ màu chuyển động khi server nặng (màu vẫn giữ, chỉ đứng yên).
- `animation.frame-ms`: làm tròn chuyển động theo khung hình; trong một khung `%boxcolor_name%` không đổi nên
  TAB/UNT không gửi lại gói tin.

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
