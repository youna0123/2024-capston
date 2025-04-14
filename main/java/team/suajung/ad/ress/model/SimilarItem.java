package team.suajung.ad.ress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarItem {
    private String id;
    private String imageUrl;
    private String productUrl;
    private Map<String, Double> weightedScores;
    private double totalWeight;
    private double similarity;
}
