# BoxColor

Màu chat, màu tên, hiệu ứng tên động và nick cho server BoxPvP. Màu mở theo rank: mỗi rank chỉ cần một permission,
rank cao tự có hết màu của rank thấp hơn. Màu ngoài rank thì mở bằng phiếu (bỏ vào crate) hoặc permission lẻ.

Paper 1.21.1, Java 21. Cần PlaceholderAPI để TAB, chat và nametag đọc được màu. LuckPerms không bắt buộc (dùng cho
lệnh cấp quyền của phiếu).

## Cài đặt

1. Bỏ jar vào `plugins/`, khởi động một lần để tạo `config.yml`, `colors.yml`, `tiers.yml`, `messages.yml`.
2. Sửa `tiers.yml` cho đúng rank, rồi cấp `boxcolor.tier.<n>` cho từng group:
   ```
   lp group elite permission set boxcolor.tier.1 true
   lp group titan permission set boxcolor.tier.2 true
   ...
   lp group overlord permission set boxcolor.tier.9 true
   ```
3. Nối placeholder vào chat, TAB và nametag (xem dưới), rồi `/boxcolor reload`.

## Người chơi thấy gì

- `/dasac` mở trang chính, `/chatcolor` mở thẳng trang màu chat, `/namecolor` mở thẳng trang màu tên.
- Màu tên tĩnh và hiệu ứng tên động là hai mục riêng. Hiệu ứng động chỉ tô màu lên tên thật, không đổi chữ.
- Hiệu ứng động có nhiều kiểu chuyển động: Đặc trưng (riêng từng hiệu ứng trong `colors.yml`), Mặc định, Sóng dày,
  Ánh kim quét, Nhịp thở, Lấp lánh và 3 kiểu kết hợp. Tự chọn kiểu cần tính năng `motion` (mặc định từ tier 7);
  chưa có thì luôn chạy kiểu Mặc định.
- Tính năng `nick` (Màu tùy chỉnh): tự đặt tên hiển thị và tô màu từng chữ. Mỗi mã màu (`&x&R&R&G&G&B&B`,
  `&#RRGGBB`, `#RRGGBB`, `&0`-`&f`, `&r`) tô các chữ phía sau nó. Gõ tên không kèm mã thì chỉ đổi chữ, giữ màu cũ.
- Màu chat chỉ chọn trong danh sách, không ai tự chỉnh mã màu chat được.
- GUI chỉ ghi Đã sở hữu / Chưa sở hữu, có bộ lọc Tất cả / Đã sở hữu / Chưa sở hữu.
- Mất rank thì màu và nick tạm ẩn (kiểm tra mỗi `access-check-seconds`), dữ liệu vẫn giữ; lên lại rank là hiện lại.

## Nick ở đâu, tên thật ở đâu

Để không ai giả danh được người khác, nick chỉ hiện ở chat:

| Nơi | Chữ | Màu |
|---|---|---|
| Chat | Nick nếu có | Màu tên tĩnh / màu tự chỉnh / màu rank |
| TAB, scoreboard | Luôn tên thật | Hiệu ứng động nếu có, không thì màu rank |
| Nametag | Luôn tên thật | Hiệu ứng động nếu có, không thì `nametag-default-color` |

Nick được so theo dạng chuẩn hoá: bỏ dấu, không phân biệt hoa thường, bỏ dấu cách và `_`, coi `0=o`, `1=i=l`,
`3=e`, `4=a`, `5=s`, `7=t`. Không được trùng nick người khác, cũng không được trùng tên thật của bất kỳ ai từng vào
server. Ai đặt nick trùng tên một người chưa từng vào thì nick đó bị gỡ khi người kia vào lần đầu. Từ cấm
(`nick.blocked-words`) và thời gian chờ đổi nick (`nick.cooldown-seconds`) mặc định tắt.

## Phiếu màu (crate)

1. `/boxcolor give <người chơi> <chat|name> <mã màu> [số lượng]` tạo phiếu (giáp da nhuộm màu). Có 3 loại phiếu tự
   nhận theo mã: màu chat, màu tên tĩnh, tên động.
2. Bỏ phiếu vào crate như vật phẩm thường. Nên đặt `Ignored_For_Permissions: [boxcolor.color.<chat|name>.<mã>]` để ai
   đã có màu thì không quay trúng lại.
3. Người chơi chuột phải vào phiếu: plugin chạy `voucher.grant-command` (mặc định
   `lp user {player} permission set {permission} true`) và trừ 1 phiếu. Đã có màu đó thì không trừ.

Phiếu được nhận ra bằng dữ liệu ẩn nên sửa chữ trong `messages.yml` không làm hỏng phiếu cũ.

## Lệnh

| Lệnh | Mô tả |
|---|---|
| `/dasac` | Trang chính |
| `/chatcolor` (`/mauchat`), `/namecolor` (`/mauten`) | Trang màu chat / màu tên; thêm `reset` để bỏ màu |
| `/nick <tên>`, `/nick reset` | Đặt / bỏ tên hiển thị (cần tính năng `nick`) |
| `/realname <nick>` (`/tenthat`) | Xem nick là của ai |
| `/boxcolor reload` | Đọc lại mọi file |
| `/boxcolor give <người chơi> <chat\|name> <mã> [số lượng]` | Tạo phiếu màu |
| `/boxcolor reset <người chơi> <chat\|name\|decor\|nick\|all>` | Xoá màu, hiệu ứng động hoặc nick (người offline chỉ xoá được nick) |
| `/boxcolor nick <người chơi> <tên\|reset>` | Admin đặt nick, bỏ qua độ dài, từ cấm và thời gian chờ |

## Permission

| Permission | Mặc định | Mô tả |
|---|---|---|
| `boxcolor.use` | mọi người | Mở menu, chọn màu đã mở |
| `boxcolor.realname` | mọi người | `/realname` |
| `boxcolor.tier.<n>` | không | Tier của rank |
| `boxcolor.color.<chat\|name>.<mã>` | không | Mở lẻ một màu |
| `boxcolor.feature.<nick\|motion>` | không | Mở lẻ một tính năng |
| `boxcolor.nick.bypass-cooldown` | op | Đổi nick không chờ |
| `boxcolor.admin` | op | Mọi màu, mọi tính năng, lệnh admin |

## Placeholder

| Placeholder | Dùng cho |
|---|---|
| `%boxcolor_name%` (= `%boxcolor_name_static%`) | Chat: nick nếu có, kèm màu tên |
| `%boxcolor_tab%` | TAB, scoreboard: tên thật, có hiệu ứng động |
| `%boxcolor_nametag%` | Nametag: tên thật, có hiệu ứng động |
| `%boxcolor_name_plain%`, `%boxcolor_realname%` | Tên/nick không màu, tên thật |
| `%boxcolor_has_nick%` | `true` / `false` |
| `%boxcolor_chat%` | Mã màu đầu tiên của màu chat (chỉ cần khi tắt `chat.enabled`) |
| `%boxcolor_chatcolor%`, `%boxcolor_namecolor%` | Tên màu đang dùng |
| `%boxcolor_tier%`, `%boxcolor_tier_level%` | Tier đang hưởng |

Thêm đuôi `_mm` (ví dụ `%boxcolor_tab_mm%`) để lấy mã MiniMessage `<#RRGGBB>` thay vì `&#RRGGBB`.

## Nối với plugin khác

**Chat (EzChat).** Nội dung chat do BoxColor tự tô màu. Trong format chỉ cần thay tên người chơi bằng
`%boxcolor_name_static%`. Nếu plugin chat dựng lại tin nhắn từ chữ thô làm mất màu, đặt `chat.enabled: false` và dùng
`%boxcolor_chat%{message}` (chỉ ra màu đầu tiên, không có gradient).

**TAB.** Xoá `customtabname` ở mọi group trong `groups.yml`, chỉ để một dòng ở `_DEFAULT_`:
```yaml
_DEFAULT_:
  customtabname: '%boxcolor_tab%'
```
Hiệu ứng động cần làm mới nhanh, trong `config.yml` của TAB:
```yaml
placeholder-refresh-intervals:
  '%boxcolor_tab%': 100
```
Màu tên theo rank đặt ở `rank-name-colors` trong `config.yml` của BoxColor.

**UnlimitedNameTag.** Dùng `%boxcolor_nametag%` trong dòng tên, `placeholderUpdateRates: '%boxcolor_nametag%': 2`.

**InteractiveChat.** Không cần sửa gì. BoxColor không tô màu các từ khoá như `[item]`, `[inv]` (`chat.keep-plain`),
tag `@tên` và tên người đang online. Tin nhắn người chơi tự gõ mã màu thì BoxColor để nguyên.

**EssentialsX.** `/nick` của BoxColor được ưu tiên. Đặt `change-displayname: false` trong Essentials để nó không ghi đè
display name.

## Dữ liệu

`plugins/BoxColor/data.db` (SQLite). Đổi mã một màu trong `colors.yml` thì ai đang dùng màu đó sẽ mất màu.

## Build

```
./gradlew build
```

Ra file `build/libs/BoxColor-<version>.jar`. Repo chỉ chứa mã nguồn, không có jar nào ngoài
`gradle/wrapper/gradle-wrapper.jar` của Gradle.
