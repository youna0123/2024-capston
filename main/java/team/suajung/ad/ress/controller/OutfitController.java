package team.suajung.ad.ress.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.suajung.ad.ress.model.*;
import team.suajung.ad.ress.service.MongoDbService;
import team.suajung.ad.ress.service.MultipleOutfitsService;
import team.suajung.ad.ress.service.OpenAiService;
import team.suajung.ad.ress.service.OutfitSearchService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outfit")
@RequiredArgsConstructor
public class OutfitController {

    private final MongoDbService mongoDbService;
    private final OpenAiService openAiService;
    private final OutfitSearchService outfitSearchService;
    private final MultipleOutfitsService multipleOutfitsService;

    @PostMapping("/recommend-search-combine")
    public ResponseEntity<Map<String, Object>> recommendSearchCombine(@RequestBody OutfitRequest request) {
        try {
            Map<String, Object> outfitData = openAiService.generateOutfit(
                    request.getMinTemp(),
                    request.getMaxTemp(),
                    request.getDesiredStyle(),
                    request.getCoordinationType(),
                    request.getSchedule(),
                    request.getEssentialItems()
            );

            Map<String, Map<String, Object>> structuredOutfitData = convertToAttributeStructure(outfitData);

            Map<String, AttributeEmbedding> attributeEmbeddings =
                    outfitSearchService.createAttributeEmbeddings(structuredOutfitData);

            Map<String, List<SimilarItem>> searchResults =
                    outfitSearchService.searchAllOutfitItems(attributeEmbeddings, structuredOutfitData, 5);

            Map<String, List<ClothingItem>> clothingItems = convertSearchResultsToClothingItems(searchResults);

            Map<String, Object> multipleOutfits = multipleOutfitsService.generateMultipleOutfits(
                    clothingItems,
                    request.getMinTemp(),
                    request.getMaxTemp(),
                    request.getDesiredStyle(),
                    request.getSchedule(),
                    request.getEssentialItems(),
                    3 // 3개의 조합 생성
            );

            return ResponseEntity.ok(multipleOutfits);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, List<ClothingItem>> convertSearchResultsToClothingItems(Map<String, List<SimilarItem>> searchResults) {
        Map<String, List<ClothingItem>> clothingItems = new HashMap<>();

        for (Map.Entry<String, List<SimilarItem>> entry : searchResults.entrySet()) {
            String category = entry.getKey();
            List<SimilarItem> items = entry.getValue();

            List<ClothingItem> convertedItems = items.stream()
                    .map(item -> {
                        ClothingItem clothingItem = new ClothingItem();
                        clothingItem.setImageUrl(item.getImageUrl());
                        clothingItem.setProductUrl(item.getProductUrl());
                        return clothingItem;
                    })
                    .toList();

            clothingItems.put(category, convertedItems);
        }

        return clothingItems;
    }

    private Map<String, Map<String, Object>> convertToAttributeStructure(Map<String, Object> outfitData) {
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : outfitData.entrySet()) {
            String itemKey = entry.getKey();
            Object value = entry.getValue();

            @SuppressWarnings("unchecked") // *
            Map<String, Object> attributeMap = (Map<String, Object>) value;
            result.put(itemKey, attributeMap);
        }

        return result;
    }
}

