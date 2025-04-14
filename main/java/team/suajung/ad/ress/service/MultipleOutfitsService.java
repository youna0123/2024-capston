package team.suajung.ad.ress.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import team.suajung.ad.ress.model.ClothingItem;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultipleOutfitsService {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final MongoDbService mongoDbService;

    public Map<String, Object> generateMultipleOutfits(
            Map<String, List<ClothingItem>> clothingItems,
            int minTemp,
            int maxTemp,
            String desiredStyle,
            String schedule,
            List<String> essentialItems,
            int numOutfits) {

        List<Map<String, Object>> messageContent = new ArrayList<>();

        Map<String, Object> promptPart = new HashMap<>();
        promptPart.put("type", "text");
        promptPart.put("text", buildBasePrompt(numOutfits, minTemp, maxTemp, desiredStyle, schedule));
        messageContent.add(promptPart);

        for (Map.Entry<String, List<ClothingItem>> entry : clothingItems.entrySet()) {
            String clothingType = entry.getKey();
            List<ClothingItem> items = entry.getValue();

            for (ClothingItem item : items) {
                Map<String, Object> imagePart = new HashMap<>();
                imagePart.put("type", "image_url");

                Map<String, String> imageUrl = new HashMap<>();
                imageUrl.put("url", item.getImageUrl());
                imagePart.put("image_url", imageUrl);

                messageContent.add(imagePart);

                Map<String, Object> typeInfoPart = new HashMap<>();
                typeInfoPart.put("type", "text");
                typeInfoPart.put("text", "위 이미지는 '" + clothingType + "' 유형의 의류입니다. ");
                messageContent.add(typeInfoPart);

                Map<String, Object> urlInfoPart = new HashMap<>();
                urlInfoPart.put("type", "text");
                urlInfoPart.put("text", "위 이미지의 이미지URL은 '" + item.getImageUrl() + "' 입니다.");
                messageContent.add(urlInfoPart);

                Map<String, Object> productUrlInfoPart = new HashMap<>();
                productUrlInfoPart.put("type", "text");
                productUrlInfoPart.put("text", "위 이미지의 상품URL은 '" + item.getProductUrl() + "' 입니다.");
                messageContent.add(productUrlInfoPart);
            }
        }

        if (!essentialItems.isEmpty()) {
            Map<String, Object> essentialItemsPart = new HashMap<>();
            essentialItemsPart.put("type", "text");
            essentialItemsPart.put("text", "꼭 포함해야 하는 옷은 다음과 같습니다.");
            messageContent.add(essentialItemsPart);

            for (String i : essentialItems) {
                ClothingItem item = mongoDbService.getItemById(i);
                Map<String, Object> imagePart = new HashMap<>();
                imagePart.put("type", "image_url");

                Map<String, String> imageUrl = new HashMap<>();
                imageUrl.put("url", item.getImageUrl());
                imagePart.put("image_url", imageUrl);

                messageContent.add(imagePart);

                Map<String, Object> urlInfoPart = new HashMap<>();
                urlInfoPart.put("type", "text");
                urlInfoPart.put("text", "위 이미지의 이미지URL은 '" + item.getImageUrl() + "' 입니다.");
                messageContent.add(urlInfoPart);

                Map<String, Object> productUrlInfoPart = new HashMap<>();
                productUrlInfoPart.put("type", "text");
                productUrlInfoPart.put("text", "위 이미지의 상품URL은 '" + item.getProductUrl() + "' 입니다.");
                messageContent.add(productUrlInfoPart);
            }
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o");

            List<Map<String, Object>> messages = new ArrayList<>();

            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "당신은 패션 스타일리스트입니다.");
            messages.add(systemMessage);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", messageContent);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = (String) message.get("content");

            System.out.println(content);

            Map<String, Object> outfitsJson = extractJsonFromText(content);

            return outfitsJson;

        } catch (Exception e) {
            log.error("여러 의상 조합 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("의상 조합 생성 실패: " + e.getMessage(), e);
        }
    }

    private String buildBasePrompt(int numOutfits, int minTemp, int maxTemp,
                                   String desiredStyle, String schedule) {
        return String.format("""
                제공된 의류 이미지들을 바탕으로 %d가지 서로 다른 옷차림을 제안해주세요.

                조건:
                - 기온: 최저 %d°C, 최고 %d°C
                - 원하는 옷차림: %s
                - 일정: %s

                JSON 형식으로 응답해주세요. 중괄호를 대괄호로 대체하지 마세요. 대괄호를 추가하지도 마세요:
                {
                  "style1": {
                    "title": "옷차림을 한 문장으로 요약한 제목",
                    "description": "옷차림을 100자 정도 카탈로그처럼 설명",
                    "items": { 
                      "top1": { "image_url": "이미지 URL", "product_url": "제품 URL" }(top1이 제공되지 않은 경우에만 생략 가능),
                      "top2": { ... }(top2이 제공되지 않은 경우에만 생략 가능),
                      "pants": { ... }(pants이 제공되지 않은 경우에만 생략 가능),
                      "outer1": { ... }(outer1이 제공되지 않은 경우에만 생략 가능),
                      "outer2": { ... }(outer2이 제공되지 않은 경우에만 생략 가능),
                      "dress": { ... }(dress이 제공되지 않은 경우에만 생략 가능),
                      "skirt": { ... }(skirt이 제공되지 않은 경우에만 생략 가능)
                    }
                  }
                  "style2": {
                    ...
                  },
                  "style3": {
                    ...
                  }
                }

                두 상의를 레이어드하는 것, 스커트나 원피스를 바지를 레이어드하는 것 모두 가능합니다. 그러므로 다음 사항은 반드시 지켜주세요:
                - top1가 제공된 경우 style 1, 2, 3 모두에서 top1을 생략하지 마세요.
                - top2가 제공된 경우 style 1, 2, 3 모두에서 top2을 생략하지 마세요.
                - pants가 제공된 경우 style 1, 2, 3 모두에서 pants을 생략하지 마세요.
                - outer1가 제공된 경우 style 1, 2, 3 모두에서 outer1을 생략하지 마세요.
                - outer2가 제공된 경우 style 1, 2, 3 모두에서 outer2을 생략하지 마세요.
                - dress가 제공된 경우 style 1, 2, 3 모두에서 dress을 생략하지 마세요.
                - skirt가 제공된 경우 style 1, 2, 3 모두에서 skirt을 생략하지 마세요.
                - 예를 들어 top1, top2, pants가 제공되었다면 top1에서 하나, top2에서 하나, pants에서 하나를 선택하여 옷차림에 포함해야 합니다.
                
                스커트나 원피스를 치마바지와 같은 옷차림에 포함하지 마세요.
                카라가 있는 상의 2개를 같은 옷차림에 포함하지 마세요.
               
                서로 다른 옷차림끼리 옷이 1~2개 겹쳐도 괜찮습니다.
                """,
                numOutfits, minTemp, maxTemp, desiredStyle, schedule);
    }

    private Map<String, Object> extractJsonFromText(String text) {
        try {
            int startIdx = text.indexOf('{');
            int endIdx = text.lastIndexOf('}') + 1;

            if (startIdx >= 0 && endIdx > 0) {
                String jsonStr = text.substring(startIdx, endIdx);
                return objectMapper.readValue(jsonStr, Map.class);
            } else {
                log.error("응답에서 JSON을 찾을 수 없습니다: {}", text);
                throw new RuntimeException("응답에서 JSON을 찾을 수 없습니다");
            }
        } catch (Exception e) {
            log.error("JSON 파싱 오류: {}, 원본 텍스트: {}", e.getMessage(), text);
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
}

