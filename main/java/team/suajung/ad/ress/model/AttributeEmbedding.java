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
public class AttributeEmbedding {
    private String category;
    private String sleeveType;
    private String isSimple;
    private String skirtType;
    private String itemKey;
    private Map<String, List<Double>> embeddings;
}
