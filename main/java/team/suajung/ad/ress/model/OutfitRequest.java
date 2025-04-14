package team.suajung.ad.ress.model;

import lombok.Data;
import java.util.List;

@Data
public class OutfitRequest {
    private int minTemp;
    private int maxTemp;
    private String desiredStyle;
    private String coordinationType;
    private String schedule;
    private List<String> essentialItems;
}
