package team.suajung.ad.ress.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Data
public class ClothingItem {
    @Id
    private ObjectId id;
    private String category;
    private String color;
    private String topType;
    private String pattern;
    private String sleeveType;
    private String fit;
    private String neckline;
    private String seasons;
    private String texture;
    private String thickness;
    private String style;
    private String tpo;
    private String details;
    private String vibe;
    private String bottomLengthType;
    private String pantsFit;
    private String skirtType;
    private String skirtFit;
    private String fasteningMethod;
    private String imageUrl;
    private String productUrl;
    private String isSimple;

}
