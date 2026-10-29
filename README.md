# BoxColor

Màu chat, màu tên và nick cho BoxPvP (Paper 1.21.1). Mở khóa theo tier: mỗi rank chỉ cần một permission.

## Cài đặt

1. Chép `build/libs/BoxColor-1.3.1.jar` vào `plugins/`. Cần **PlaceholderAPI** để các plugin khác đọc được màu.
2. Khởi động server một lần để tạo `config.yml`, `colors.yml`, `tiers.yml`, `messages.yml`.
3. Sửa `tiers.yml` cho đúng rank của server, rồi cấp quyền tier trong LuckPerms (xem phần dưới).
4. Nối placeholder vào EzChat, TAB, UnlimitedNameTag (xem phần dưới).
5. `/boxcolor reload`.

## Lệnh

| Lệnh | Việc |
|---|---|
| `/chatcolor`, `/namecolor` (`/mauchat`, `/mauten`) | Mở trang chính (cả hai lệnh cùng một trang) |
| `/chatcolor reset`, `/namecolor reset` | Bỏ màu |
| `/chatcolor custom #FFB3C6 #A0E7E5` | Tự chỉnh mã màu (cần tính năng `custom-chat` / `custom-name`) |
| `/nick <tên>`, `/nick reset` | Đặt / bỏ nick (cần tính năng `nick`) |
| `/nick #FFD166Đức #FF8FABAnh` | Nick kèm màu: mã `&#RRGGBB`, `#RRGGBB` hoặc `&a`... ở đâu cũng được, 1 mã = màu đơn, nhiều mã = chuyển màu cả nick. Cần thêm `custom-name`. Gõ lại nick cũ kèm mã chỉ đổi màu, không tính thời gian chờ |
| `/realname <nick>` (`/tenthat`) | Xem nick là của ai |
| `/boxcolor reload` | Nạp lại mọi file |
| `/boxcolor reset <người chơi> <chat\|name\|nick\|all>` | Xóa màu/nick (offline: chỉ xóa được nick) |
| `/boxcolor nick <người chơi> <tên\|reset>` | Admin đặt nick, bỏ qua độ dài, từ cấm và thời gian chờ |

## Phân quyền

Mỗi rank chỉ cần một dòng, tier cao tự có hết màu của tier thấp hơn:

```
lp group elite permission set boxcolor.tier.1 true
lp group titan permission set boxcolor.tier.2 true
lp group hero permission set boxcolor.tier.3 true
lp group master permission set boxcolor.tier.4 true
lp group legend permission set boxcolor.tier.5 true
lp group champion permission set boxcolor.tier.6 true
lp group zeus permission set boxcolor.tier.7 true
lp group ultimate permission set boxcolor.tier.8 true
lp group overlord permission set boxcolor.tier.9 true
```

| Permission | Mặc định | Việc |
|---|---|---|
| `boxcolor.use` | mọi người | Mở menu, chọn màu đã mở khóa |
| `boxcolor.realname` | mọi người | `/realname` |
| `boxcolor.tier.<n>` | không | Tier của rank |
| `boxcolor.color.<chat\|name>.<mã>` | không | Tặng lẻ một màu (quà sự kiện), không cần lên rank |
| `boxcolor.feature.<custom-chat\|custom-name\|nick>` | không | Tặng lẻ một tính năng |
| `boxcolor.nick.bypass-cooldown` | op | Đổi nick không cần chờ |
| `boxcolor.admin` | op | Mọi màu, mọi tính năng, lệnh admin |

Mất rank thì màu và nick **tạm ẩn** (kiểm tra lại mỗi `access-check-seconds`), dữ liệu vẫn giữ. Lên lại rank là hiện lại.
Nick do admin đặt cũng chỉ hiện khi người đó có tính năng `nick`.

## Placeholder

| Placeholder | Dùng cho |
|---|---|
| `%boxcolor_name%` | Tên (nick nếu có) đã tô màu, **có chuyển động**. Cho TAB và nametag |
| `%boxcolor_name_static%` | Như trên nhưng đứng yên. **Cho chat** |
| `%boxcolor_name_plain%` | Tên/nick không màu |
| `%boxcolor_realname%` | Tên thật |
| `%boxcolor_has_nick%` | `true` / `false` |
| `%boxcolor_chat%` | Mã màu đầu tiên của màu chat (chỉ cần khi tắt `chat.enabled`) |
| `%boxcolor_chatcolor%`, `%boxcolor_namecolor%` | Tên màu đang dùng |
| `%boxcolor_tier%`, `%boxcolor_tier_level%` | Tier đang hưởng |

Thêm đuôi `_mm` (ví dụ `%boxcolor_name_mm%`) để lấy mã MiniMessage `<#RRGGBB>`. Mặc định là `&#RRGGBB`
(`placeholder-format` trong `config.yml`).

## Nối với plugin khác

### EzChat
Màu nội dung chat do BoxColor tự tô, không cần sửa gì. Chỉ cần thay tên người chơi trong format:
```yaml
format: '... &f%player_name%&7: &f{message}'
# thành
format: '... %boxcolor_name_static%&7: &f{message}'
```
Chat không chuyển động được (dòng chat đã gửi là đứng yên), nên luôn dùng `_static` ở chat.

Nếu sau khi cài, nội dung chat **không** có màu (EzChat tự dựng lại tin nhắn từ chữ thô), đặt `chat.enabled: false`
trong `config.yml` và dùng `%boxcolor_chat%{message}` trong format. Cách này chỉ ra màu đầu tiên, không có gradient.

### TAB
Màu tên theo rank nằm ở `rank-name-colors` trong `config.yml` của BoxColor, không để trong TAB nữa.
Ở `plugins/TAB/groups.yml`, **xóa `customtabname` ở mọi group** (group nào còn giữ sẽ đè màu BoxColor),
chỉ đặt một lần:
```yaml
_DEFAULT_:
  customtabname: '%boxcolor_name%'
```
Màu chuyển động cần tốc độ làm mới nhanh, ở `plugins/TAB/config.yml`:
```yaml
placeholder-refresh-intervals:
  '%boxcolor_name%': 100
```
Nếu tablist nặng khi đông người, đổi thành 200.

### UnlimitedNameTag (`settings.yml`)
```yaml
- text: '%boxcolor_name% &8[%boxclans_icon%&f%boxclans_name%&8]'
```
```yaml
placeholderUpdateRates:
  '%boxcolor_name%': 2
```

### InteractiveChat
Không cần sửa config của IC. BoxColor để nguyên (không tô gradient) các đoạn IC cần nhận ra: từ khoá `[item]`,
`[inv]`, `[money]`... (`chat.keep-plain`), tag `@tên`, tên và nick người đang online (`chat.keep-player-names`).
Người chơi tự gõ màu (`&a`, `#FF0000`, `[color=#...]`) thì BoxColor không tô màu chat cho tin nhắn đó.

### EssentialsX
- `/nick` của BoxColor được ưu tiên hơn `/nick` của Essentials (Essentials tự nhường lệnh trùng tên).
- Đặt `change-displayname: false` trong config của Essentials, để Essentials không ghi đè display name
  (tin nhắn chết, vào/thoát server) mà BoxColor đặt.

## Chống giả mạo nick

So nick theo dạng chuẩn hoá: bỏ dấu, không phân biệt hoa thường, coi `0=o`, `1=i=l`, `3=e`, `4=a`, `5=s`, `7=t`,
bỏ dấu cách và `_`. Vì vậy "Ádm1n", "St3ve" bị coi là trùng "admin", "Steve".
- Không được trùng nick của người khác (kể cả đang offline).
- Không được trùng tên thật của bất kỳ ai từng vào server (`nick.check-real-names`).
- Nếu ai đó đặt nick trùng tên một người chưa từng vào server, khi người đó vào lần đầu thì nick kia bị gỡ.
- `nick.blocked-words`: chặn nick chứa từ như admin, staff...

## File dữ liệu

`plugins/BoxColor/data.db` (SQLite). Đổi mã một màu trong `colors.yml` thì người đang dùng màu đó sẽ mất màu.
