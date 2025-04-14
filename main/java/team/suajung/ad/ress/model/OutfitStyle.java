package team.suajung.ad.ress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 단일 의상 조합 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitStyle {
    private String title;
    private String description;
    private Map<String, OutfitItem> items;
}
