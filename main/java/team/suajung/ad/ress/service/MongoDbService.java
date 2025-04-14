package team.suajung.ad.ress.service;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import team.suajung.ad.ress.model.ClothingItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MongoDbService {

    private final MongoTemplate mongoTemplate;
    private final List<String> collections = Arrays.asList(
            "top_final", "pants_final", "outer_final", "dress_final", "skirt_final"
    );

    public ClothingItem getItemById(String itemId) {
        ObjectId objectId = new ObjectId(itemId);

        for (String collectionName : collections) {
            Map<String, Object> result = mongoTemplate.findOne(
                    Query.query(Criteria.where("_id").is(objectId)),
                    Map.class,
                    collectionName
            );

            if (result != null) {
                ClothingItem item = new ClothingItem();
                item.setId(objectId);

                for (Map.Entry<String, Object> entry : result.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (!key.endsWith("_embedding")) {
                        switch (key) {
                            case "_id":
                                break;
                            case "image_url":
                                item.setImageUrl(value.toString());
                                break;
                            case "product_url":
                                item.setProductUrl(value.toString());
                                break;
                            case "category":
                                item.setCategory(value.toString());
                                break;
                            case "color":
                                item.setColor(value.toString());
                                break;
                            case "top_type":
                                item.setTopType(value.toString());
                                break;
                            case "pattern":
                                item.setPattern(value.toString());
                                break;
                            case "sleeve_type":
                                item.setSleeveType(value.toString());
                                break;
                            case "fit":
                                item.setFit(value.toString());
                                break;
                            case "neckline":
                                item.setNeckline(value.toString());
                                break;
                            case "seasons":
                                item.setSeasons(value.toString());
                                break;
                            case "texture":
                                item.setTexture(value.toString());
                                break;
                            case "thickness":
                                item.setThickness(value.toString());
                                break;
                            case "style":
                                item.setStyle(value.toString());
                                break;
                            case "tpo":
                                item.setTpo(value.toString());
                                break;
                            case "details":
                                item.setDetails(value.toString());
                                break;
                            case "vibe":
                                item.setVibe(value.toString());
                                break;
                            case "bottom_length_type":
                                item.setBottomLengthType(value.toString());
                                break;
                            case "pants_fit":
                                item.setPantsFit(value.toString());
                                break;
                            case "fastening_method":
                                item.setFasteningMethod(value.toString());
                                break;
                            case "skirt_type":
                                item.setSkirtType(value.toString());
                                break;
                            case "skirt_fit":
                                item.setSkirtFit(value.toString());
                                break;
                        }
                    }
                }

                return item;
            }
        }
        return null;
    }
}
