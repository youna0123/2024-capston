package team.suajung.ad.ress.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import team.suajung.ad.ress.model.AttributeEmbedding;
import team.suajung.ad.ress.model.SimilarItem;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitSearchService {

    private final MongoTemplate mongoTemplate;
    private final WebClient webClient;

    public Map<String, AttributeEmbedding> createAttributeEmbeddings(Map<String, Map<String, Object>> outfitJson) {
        Map<String, AttributeEmbedding> embeddingsResult = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : outfitJson.entrySet()) {
            String itemKey = entry.getKey();
            Map<String, Object> attributes = entry.getValue();

            String category = (String) attributes.get("category");
            String sleeveType = (String) attributes.get("sleeve_type");
            String isSimple = (String) attributes.get("isSimple");
            String skirtType = (String) attributes.get("skirt_type");

            Map<String, List<Double>> attrEmbeddings = new HashMap<>();

            for (Map.Entry<String, Object> attrEntry : attributes.entrySet()) {
                String attrName = attrEntry.getKey();
                Object attrValue = attrEntry.getValue();

                if (!"category".equals(attrName) && !"sleeve_type".equals(attrName) && !"isSimple".equals(attrName) && !"skirt_type".equals(attrName) && !"style_attributes".equals(attrName)) {
                    String attrText = attrValue.toString();

                    try {
                        List<Double> embeddingVector = createEmbedding(attrText);

                        attrEmbeddings.put(attrName + "_embedding", embeddingVector);
                    } catch (Exception e) {
                        log.error("'{}' 임베딩 생성 중 오류: {}", attrName, e.getMessage());
                    }
                }
            }

            AttributeEmbedding attributeEmbedding = AttributeEmbedding.builder()
                    .category(category)
                    .sleeveType(sleeveType)
                    .isSimple(isSimple)
                    .skirtType(skirtType)
                    .itemKey(itemKey)
                    .embeddings(attrEmbeddings)
                    .build();

            embeddingsResult.put(itemKey, attributeEmbedding);
        }

        return embeddingsResult;
    }

    private List<Double> createEmbedding(String text) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "text-embedding-3-large");
        requestBody.put("input", text);

        Map<String, Object> response = webClient.post()
                .uri("/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            return (List<Double>) data.get(0).get("embedding");
    }

    public String findMatchingCollection(String itemKey) {
        // 아이템 키 기반 매핑
        Map<String, String> keyMapping = Map.of(
                "top1", "top_final",
                "top2", "top_final",
                "pants", "pants_final",
                "outer1", "outer_final",
                "outer2", "outer_final",
                "dress", "dress_final",
                "skirt", "skirt_final"
        );

        return keyMapping.get(itemKey);
    }

    public Map<String, Double> getAttributeWeights(Map<String, Map<String, Object>> outfitJson) {
        Map<String, Double> baseWeights = new HashMap<>();
        baseWeights.put("color", 3.0);
        baseWeights.put("top_type", 1.0);
        baseWeights.put("pattern", 1.5);
        baseWeights.put("fit", 1.5);
        baseWeights.put("neckline", 1.0);
        baseWeights.put("seasons", 1.0);
        baseWeights.put("texture", 1.0);
        baseWeights.put("thickness", 1.0);
        baseWeights.put("style", 1.0);
        baseWeights.put("tpo", 1.0);
        baseWeights.put("details", 1.0);
        baseWeights.put("vibe", 2.0);
        baseWeights.put("bottom_length_type", 5.0);
        baseWeights.put("pants_fit", 1.5);
        baseWeights.put("fastening_method", 1.0);
        baseWeights.put("skirt_fit", 1.5);

        Map<String, Double> weights = new HashMap<>(baseWeights);

        for (Map.Entry<String, Map<String, Object>> entry : outfitJson.entrySet()) {
            String itemKey = entry.getKey();
            Map<String, Object> itemData = entry.getValue();

            if (itemData.containsKey("style_attributes")) {
                @SuppressWarnings("unchecked")
                List<String> styleAttrs = (List<String>) itemData.get("style_attributes");

                for (String attr : styleAttrs) {
                    weights.put(attr, weights.get(attr) * 3.0);
                }
            }
        }

        return weights;
    }

    public List<SimilarItem> searchSimilarItems(AttributeEmbedding itemData,
                                                Map<String, Map<String, Object>> outfitJson,
                                                int topN) {
        String category = itemData.getCategory();
        String sleeveType = itemData.getSleeveType();
        String isSimple = itemData.getIsSimple();
        String skirtType = itemData.getSkirtType();
        String itemKey = itemData.getItemKey();

        String collectionName = findMatchingCollection(itemKey);

        try {
            Map<String, Double> attrWeights = getAttributeWeights(outfitJson);

            Map<String, List<Double>> allEmbeddings = itemData.getEmbeddings();

            List<SimilarItem> similarItems = new ArrayList<>();

            for (Map.Entry<String, List<Double>> entry : allEmbeddings.entrySet()) {
                String attrName = entry.getKey();
                List<Double> queryEmbedding = entry.getValue();

                String baseAttr = attrName.replace("_embedding", "");
                double weight = attrWeights.get(baseAttr);

                List<Document> categoryConditions = new ArrayList<>();
                if (category != null) {
                    String[] categories = category.split(",");
                    for (String cat : categories) {
                        categoryConditions.add(new Document("category",
                                new Document("$regex", "^" + Pattern.quote(cat.trim()) + "$")));
                    }
                }

                List<Document> pipeline = new ArrayList<>();

                Document vectorSearchDoc = new Document("index", collectionName + "_vector_index")
                        .append("path", attrName)
                        .append("queryVector", queryEmbedding)
                        .append("numCandidates", 1000)
                        .append("limit", 1000);

                pipeline.add(new Document("$vectorSearch", vectorSearchDoc));

                if (!categoryConditions.isEmpty()) {
                    pipeline.add(new Document("$match",
                            new Document("$or", categoryConditions)));
                }

                if (sleeveType != null) {
                    pipeline.add(new Document("$match",
                            new Document("sleeve_type", sleeveType)));
                }

                if (isSimple != null && !isSimple.isEmpty()) {
                    pipeline.add(new Document("$match",
                            new Document("isSimple", isSimple)));
                }

                if (skirtType != null && !skirtType.isEmpty()) {
                    pipeline.add(new Document("$match",
                            new Document("skirt_type", skirtType)));
                }

                pipeline.add(new Document("$project",
                        new Document("_id", 1)
                                .append("image_url", 1)
                                .append("product_url", 1)
                                .append("score", new Document("$meta", "vectorSearchScore"))));

                List<Document> results = mongoTemplate.getCollection(collectionName)
                        .aggregate(pipeline)
                        .into(new ArrayList<>());

                for (Document item : results) {
                    String itemId = item.getObjectId("_id").toString();

                    Optional<SimilarItem> existingItemOpt = similarItems.stream()
                            .filter(x -> x.getId().equals(itemId))
                            .findFirst();

                    if (existingItemOpt.isPresent()) {
                        SimilarItem existingItem = existingItemOpt.get();
                        existingItem.getWeightedScores().put(baseAttr, item.getDouble("score") * weight);
                        existingItem.setTotalWeight(existingItem.getTotalWeight() + weight);
                    } else {
                        Map<String, Double> weightedScores = new HashMap<>();
                        weightedScores.put(baseAttr, item.getDouble("score") * weight);

                        SimilarItem newItem = SimilarItem.builder()
                                .id(itemId)
                                .imageUrl(item.getString("image_url"))
                                .productUrl(item.getString("product_url"))
                                .weightedScores(weightedScores)
                                .totalWeight(weight)
                                .build();

                        similarItems.add(newItem);
                    }
                }
            }

            for (SimilarItem item : similarItems) {
                double weightedSum = item.getWeightedScores().values().stream().mapToDouble(Double::doubleValue).sum();
                double totalWeight = item.getTotalWeight();

                item.setSimilarity(weightedSum / totalWeight);
            }

            similarItems.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());

            return similarItems.stream().limit(topN).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("벡터 검색 수행 중 오류: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Map<String, List<SimilarItem>> searchAllOutfitItems(
            Map<String, AttributeEmbedding> outfitAttributeEmbeddings,
            Map<String, Map<String, Object>> outfitJson,
            int topN) {

        Map<String, List<SimilarItem>> allResults = new HashMap<>();

        for (Map.Entry<String, AttributeEmbedding> entry : outfitAttributeEmbeddings.entrySet()) {
            String itemKey = entry.getKey();
            AttributeEmbedding itemData = entry.getValue();

            List<SimilarItem> similarItems = searchSimilarItems(itemData, outfitJson, topN);
            allResults.put(itemKey, similarItems);
        }

        return allResults;
    }
}
