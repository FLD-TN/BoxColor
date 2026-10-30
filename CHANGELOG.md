# Changelog

## 1.12.4 (2026-10-30)

- messages.yml mặc định trong jar lấy theo bản đang dùng trên server (chữ trang chọn màu tên, lore trang chính, nút
  Mở) kèm phiếu màu 3 loại. Bỏ khối `menu.confirm` không còn dùng.

## 1.12.3 (2026-10-30)

- Phiếu màu chia 3 loại, mỗi loại một nhãn màu riêng: Màu chat, Màu tên tĩnh, Tên động (`voucher.type.*`).
- Lore phiếu bỏ dòng mã màu, thêm mục "Hiện ở đâu" theo loại (`voucher.where.*`, placeholder `{where}`).

## 1.12.2 (2026-10-30)

- Icon GUI và phiếu màu (áo da nhuộm) không còn hiện dòng "When on Body: +3 Armor". Từ 1.20.5 HIDE_ATTRIBUTES không
  ẩn chỉ số mặc định, nên item được ghi một chỉ số giáp +0 thay chỗ rồi ẩn đi. Phiếu mặc lên người cũng không cho giáp.

## 1.12.1 (2026-10-30)

- Bỏ tự chỉnh mã màu chat (`/chatcolor custom`, nút trong GUI, `boxcolor.feature.custom-chat`). Màu chat chỉ chọn
  trong danh sách: rank tặng hoặc phiếu crate. Tự tùy chỉnh chỉ còn ở tên (`/nick`). Ai đang dùng màu chat tự chỉnh
  sẽ tự về không màu.
- Bỏ `custom.max-colors` trong config.yml (dòng cũ còn trong file không ảnh hưởng gì).

## 1.12.0 (2026-10-30)

- `/chatcolor` mở thẳng trang màu chat, `/namecolor` mở thẳng trang chọn màu tên tĩnh / hiệu ứng động.
- Lệnh mới `/dasac` mở trang chính (màu chat và màu tên). Trang chính bỏ ô đầu người chơi, 2 ô dời vào giữa.

## 1.11.2 (2026-10-30)

- Zeus được tặng hiệu ứng tên động **Thần Sấm** (trước chỉ có ở crate). tiers.yml trên server: thêm `than_sam`
  vào `name-colors` của tier 7.

## 1.11.1 (2026-10-30)

- Quà hiệu ứng động của Ultimate đổi từ Cầu Vồng Động sang **Cực Quang**. Cầu Vồng Động chuyển sang crate
  (chỉ mở bằng phiếu). tiers.yml trên server: sửa tay `name-colors` của tier 8.

## 1.11.0 (2026-10-30)

- Tự chọn kiểu chuyển động thành tính năng `motion` (tiers.yml), mở từ **Zeus** (tier 7). tiers.yml cũ không ghi
  `motion` ở tier nào thì vẫn mặc định tier 7. Quyền lẻ: `boxcolor.feature.motion`.
- Dưới Zeus vẫn mở được menu Kiểu chuyển động để xem trước, nhưng không chọn được; hiệu ứng (kể cả từ phiếu crate)
  luôn chạy kiểu **Mặc định**. Mất rank thì quay về Mặc định, lựa chọn cũ vẫn giữ, lên lại rank là dùng lại.

## 1.10.1 (2026-10-30)

- Vào server (nhất là ngay sau restart) không còn hiện tên màu trắng ở TAB/scoreboard trong lúc chờ database: màu rank
  tính ngay khi vào, hiệu ứng động hiện khi đọc xong. Trong lúc chờ không ghi gì vào database (không mất màu đã lưu).

## 1.10.0 (2026-10-30)

- Hiệu ứng tên động có **kiểu chuyển động**, nổi bật hơn trên chữ nhỏ ở TAB:
  - Sóng dày (`wave`): màu lặp mỗi vài chữ, chữ cạnh nhau khác màu hẳn.
  - Ánh kim quét (`shine`): vệt sáng quét qua tên rồi nghỉ.
  - Nhịp thở (`pulse`): cả tên đổi màu cùng lúc.
  - Lấp lánh (`spark`): vài chữ chớp sáng rồi tắt dần.
  - Kết hợp được: sóng dày + ánh kim, sóng dày + lấp lánh, ánh kim + lấp lánh.
- 25 hiệu ứng có kiểu **Đặc trưng** riêng, màu tương phản hơn (colors.yml: `motion`, `wave`, `effects`,
  `highlight`, `spark-density`). Hiệu ứng không ghi `motion` chạy như bản cũ.
- Người chơi tự chọn kiểu ở trang Hiệu ứng tên động, nút **Kiểu chuyển động** (ô 51): Đặc trưng, Mặc định (cách
  chạy cũ) và 7 kiểu trên. Áp cho mọi hiệu ứng, lưu trong cột `motion` (tự thêm vào data.db cũ).
- `animation.default-motion` (config.yml): kiểu cho người chưa tự chọn, mặc định `signature`.
- Ánh kim quét và lấp lánh trên nền đứng yên gửi ít gói tin hơn: lúc không có vệt sáng/tia chớp chuỗi không đổi.
- `/boxcolor reset <người chơi> all` xoá cả kiểu chuyển động.

## 1.9.3 (2026-10-29)

- Màu chat miễn phí chỉ từ Master trở lên, mỗi rank 1 màu đơn: Master Lá Non, Legend Đỏ Rực, Champion Tím Hoàng Gia,
  Zeus Xanh Lam, Ultimate Cam Lửa, Overlord Hồng Neon. Elite, Titan, Hero không có màu chat miễn phí.
- Bỏ 12 màu chat đơn còn lại (crate chỉ có màu trộn). Màu chat: 38 (6 đơn theo rank + 32 màu trộn crate).

## 1.9.2 (2026-10-29)

- Bỏ mọi chữ giải thích cách nhận: dòng "Mở khóa" trong lore màu, mục "Cách nhận / Rank tặng sẵn" của hiệu ứng động,
  "(cần ...)" trong tin nhắn và trạng thái khóa. GUI chỉ còn Đã sở hữu / Chưa sở hữu.

## 1.9.1 (2026-10-29)

- Ai cũng vào được trang Hiệu ứng tên động và dùng hiệu ứng mình sở hữu (phiếu crate). Bỏ yêu cầu `animated-name`
  (tiers.yml cũ còn dòng này vẫn đọc được, chỉ không còn tác dụng). Ultimate tặng Cầu Vồng Động, Overlord tặng Bá Chủ;
  lore trang chọn loại liệt kê hiệu ứng rank tặng kèm ảnh rank.

## 1.9.0 (2026-10-29)

- Màu chat theo rank chỉ còn màu đơn (18 màu, mỗi rank 2); màu trộn từ 2 màu trở lên chỉ có ở crate. 10 màu
  gradient từng thuộc rank chuyển sang crate (bỏ Mật Ong vì trùng tông Hổ Phách/Cam Lửa).
- Hiệu ứng tên động theo rank chỉ còn 2: Ultimate Cầu Vồng Động, Overlord Bá Chủ. 7 hiệu ứng còn lại chuyển sang crate.
- Thêm 13 hiệu ứng động crate, lọc tự động để khác mọi hiệu ứng và màu tên tĩnh đã có: Điện Tím, Bình Minh Động,
  Lục Bảo Vàng, San Hô Động, Lửa Băng Động, Giáng Sinh, Đào Tiên Động, Ma Trận, Tử Đằng Động, Sóng Neon,
  Thiên Hà Động, Mùa Xuân Động, Cực Tím.
- Tổng: chat 50 (18 rank + 32 crate), tên tĩnh 36 (27 + 9), hiệu ứng động 25 (2 + 23).

## 1.8.2 (2026-10-29)

- Thiết kế lại bảng màu để không màu nào trùng tông: đo bằng khoảng cách CIELAB trên 16 điểm mẫu (màu động so cả
  khi lệch pha), mỗi tông chỉ một màu đại diện trong mỗi trang. Không còn màu nhạt/trắng, mọi mã màu đọc được trên
  nền chat tối. Giữ nguyên mã các màu thuộc rank (tiers.yml không đổi), một số đổi tên hiển thị.
- Màu chat 41 (18 rank + 23 crate), màu tên 48 (27 tĩnh rank + 9 hiệu ứng động rank + 12 crate).
- Bỏ màu crate trùng tông: chat Cầu Vồng Pastel, Cầu Vồng Neon, Lửa Địa Ngục, Hoàng Hôn Rực, Dung Nham cũ,
  Ruby Vàng, Hoàng Kim, Kẹo Ngọt, Ánh Trăng, Ngân Hà, Ráng Chiều, Nắng Hạ, Lục Bảo, Bạc Hà Chanh, Lam Ngọc,
  Hồng Ngọc và 7 màu đơn crate; tên Đỏ Thẫm, Rồng Lửa, Biển Sâu, Hoàng Đế, Hỏa Long Động, Cầu Vồng Neon Động.

## 1.8.1 (2026-10-29)

- Scoreboard hiện tên như TAB (`%boxcolor_tab%`: tên thật, hiệu ứng động hoặc màu rank); nick chỉ còn ở chat.
  Sửa chữ trong GUI cho khớp.

## 1.8.0 (2026-10-29)

Chống giả danh: nick chỉ hiện ở chat và scoreboard.
- `%boxcolor_name%` (chat, scoreboard): nick + màu tên tĩnh / tùy chỉnh / màu rank, luôn đứng yên.
- Mới `%boxcolor_tab%` (TAB) và `%boxcolor_nametag%` (nametag): **luôn là tên thật**; chỉ đổi màu theo hiệu ứng
  động, không có thì màu rank (TAB) hoặc `nametag-default-color` (mặc định #FFFF55, &e).
- Màu động thành **hiệu ứng trang trí** lưu riêng (cột `decor` trong data.db, tự thêm vào database cũ), chọn ở trang
  "Hiệu ứng tên động"; bỏ nút đổi chữ ở trang đó. Lựa chọn màu động cũ tự chuyển sang hiệu ứng.
- `/boxcolor reset <người chơi> decor`.

## 1.7.3 (2026-10-29)

- Thêm màu rực (crate, không thuộc tier nào): 22 màu chat (7 đơn, 14 gradient, Cầu Vồng Neon) và 16 màu tên
  (13 tĩnh, 3 động: Neon Động, Hỏa Long Động, Cầu Vồng Neon Động). Mọi màu đủ sáng để đọc trên nền chat tối.
- Bỏ 6 màu chat crate quá nhạt: Thủy Tinh, Sương Mai, Ngọc Trai, Băng Hồng, Kẹo Dẻo, Cầu Vồng Nhạt.

## 1.7.2 (2026-10-29)

- Tên tier hỗ trợ placeholder PlaceholderAPI. tiers.yml mặc định dùng ảnh rank của ItemsAdder
  (`&f%img_wellsetups_ranks_<rank>%`, như menu /rank), hiện ở dòng "Mở khóa", "cần ... trở lên", đầu người chơi
  và `%boxcolor_tier%`.

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
