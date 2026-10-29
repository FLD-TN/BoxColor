package vn.boxpvp.boxcolor.tier;

import java.util.Set;
import vn.boxpvp.boxcolor.style.Channel;

/**
 * Một tier trong tiers.yml. Chỉ liệt kê những gì tier này MỞ THÊM; tier cao tự có mọi thứ của tier thấp hơn.
 *
 * @param name tên hiện trong GUI ("Mở khóa: Rank Y"), có mã màu '&'
 */
public record Tier(int level, String name, Set<String> chatColors, Set<String> nameColors, Set<Feature> features) {

    public Tier {
        chatColors = Set.copyOf(chatColors);
        nameColors = Set.copyOf(nameColors);
        features = Set.copyOf(features);
    }

    public Set<String> colors(Channel channel) {
        return channel == Channel.CHAT ? chatColors : nameColors;
    }
}
