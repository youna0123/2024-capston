package team.suajung.ad.ress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 의상 조합 내 아이템 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitItem {
    private String imageUrl;
    private String productUrl;
}
