package team.suajung.ad.ress.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import team.suajung.ad.ress.model.ClothingItem;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OpenAiService {
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final MongoDbService mongoDbService;

    public Map<String, Object> generateOutfit(int minTemp, int maxTemp, String desiredStyle,
                                              String coordinationType, String schedule,
                                              List<String> essentialItems) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "o3-mini");

        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "당신은 패션 스타일리스트입니다.");
        messages.add(systemMessage);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Map<String, Object>> contentParts = new ArrayList<>();

        if (coordinationType.equals("독특한 코디네이션")) {
            Random random = new Random();
            int number = random.nextInt(5) + 1;
            if (number == 1) {
                coordinationType = "크로스오버 코디네이션";
            } else if (number == 2) {
                coordinationType = "심플한 긴소매 티셔츠를 반소매나 민소매와 레이어드";
            } else if (number == 3) {
                coordinationType = "심플한 셔츠/블라우스를 맨투맨, 후드 티셔츠, 니트, 반소매 티셔츠나 민소매 티셔츠와 레이어드";
            } else if (number == 4) {
                coordinationType = "바지를 원피스와 레이어드";
            } else if (number == 5) {
                coordinationType = "바지를 랩스커트와 레이어드";
            }
        }

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", buildBasePrompt(minTemp, maxTemp, desiredStyle, coordinationType, schedule));
        contentParts.add(textPart);

        if (!essentialItems.isEmpty()) {
            Map<String, Object> essentialItemsTextPart = new HashMap<>();
            essentialItemsTextPart.put("type", "text");
            essentialItemsTextPart.put("text", "꼭 포함해야 하는 옷은 다음과 같습니다. 꼭 포함해야 하는 옷은 응답에서 생략하세요:");
            contentParts.add(essentialItemsTextPart);

            for (int i = 0; i < essentialItems.size(); i++) {
                ClothingItem item = mongoDbService.getItemById(essentialItems.get(i));

                Map<String, Object> itemMap = objectMapper.convertValue(item, Map.class);
                List<String> fieldDescriptions = new ArrayList<>();

                for (Map.Entry<String, Object> entry : itemMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (!key.equals("id") && !key.equals("imageUrl") && !key.equals("productUrl")) {
                        fieldDescriptions.add(key + ": " + value);
                    }
                }

                Map<String, Object> itemTextPart = new HashMap<>();
                itemTextPart.put("type", "text");
                itemTextPart.put("text", "꼭 포함해야 하는 옷 " + (i + 1) + " 정보: " + String.join(", ", fieldDescriptions));
                contentParts.add(itemTextPart);
            }
        }

        userMessage.put("content", contentParts);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        try {
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

            return extractJsonFromText(content);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }

    private String buildBasePrompt(int minTemp, int maxTemp, String desiredStyle,
                                   String coordinationType, String schedule) {
        return String.format("""
                다음 정보를 바탕으로 적합한 옷차림을 구상해주세요:
                - 기온: 최저 %d°C, 최고 %d°C
                - 원하는 옷차림: %s
                - 코디네이션 유형: %s(사용 여부는 조건에 따라 다름. 아래 명시한 조건 참고)
                - 일정: %s
                
                다음 중 필요한 옷과 각 옷의 필요한 속성(최대 7가지)을 선택하여 JSON 형태로 출력해 주세요. 카테고리 값은 한글로, 나머지 속성 값은 영어로 출력하세요. 각 속성의 값은 콤마로 구분하여 문자열로 출력하세요.:
                - top1: category(맨투맨, 후드 티셔츠, 셔츠/블라우스, 티셔츠, 니트 중에서 복수 선택 가능), color, top_type(상의 기장), pattern, sleeve_type(long sleeve, short sleeve, sleeveless 중에서 하나 선택), fit, neckline, seasons, texture, thickness, style, tpo, details, vibe, isSimple(yes, no 중에서 하나 선택)
                - top2: top1과 동일
                - pants: category(데님 팬츠, 트레이닝 팬츠, 코튼 팬츠, 슬랙스, 레깅스, 오버올 중에서 복수 선택 가능), color, bottom_length_type, pattern, pants_fit(fit이 아니라 pants_fit임), seasons, texture, thickness, style, tpo, details, vibe
                - outer1: category(후드집업, 블루종/MA-1, 레더/라이더스 재킷, 카디건, 트러커 재킷, 슈트/블레이저 재킷, 스타디움 재킷, 나일론/코치 재킷, 아노락 재킷, 트레이닝 재킷, 코트, 사파리/헌팅 재킷, 패딩, 무스탕/퍼, 플리스/뽀글이 중에서 복수 선택 가능), color, top_type, pattern, sleeve_type, fit, seasons, texture, thickness, style, tpo, details, vibe, fastening_method
                - outer2: outer1과 동일
                - dress: category(미니원피스, 미디원피스, 맥시원피스 중에서 복수 선택 가능), color, skirt_type(pleated, tiered, wrap, plain, skirt pants 중에서 하나 선택), pattern, sleeve_type, fit, neckline, seasons, texture, thickness, style, tpo, details, vibe
                - skirt: category(미니스커트, 미디스커트, 롱스커트 중에서 복수 선택 가능), color, pattern, skirt_type(pleated, tiered, wrap, plain, skirt pants 중에서 하나 선택), skirt_fit, seasons, texture, thickness, style, tpo, details, vibe
                - category, sleeve_type, isSimple, skirt_type을 제외한 '%s'과 관련이 깊은 속성의 이름(예: color)을 각 옷마다 선택해서 "style_attributes" 키에 배열로 추가해주세요. 만약 없다면 추가하지 않아도 됩니다. 배열의 요소는 큰따옴표로 묶지만 배열을 다시 큰따옴표로 묶지 않습니다. 해당 옷에 대하여 선택된 속성 내에서만 선택할 수 있습니다.
                
                1. 만약 코디네이션 유형이 평범한 코디네이션이라면 코디네이션 유형을 무시하세요. 그렇지 않다면 2번으로 가세요.
                2. 만약 사용자가 독특한 코디네이션의 방법을 원하는 옷차림에 명시했다면 코디네이션 유형을 무시하세요. 그렇지 않다면 3번으로 가세요.
                3. 만약 꼭 포함해야 하는 옷의 정보가 제공되었다면 코디네이션 유형을 무시하고 다음 중 해당 옷에 적합한 코디네이션 유형을 선택하세요. 그렇지 않다면 4번으로 가세요.
                - 크로스오버 코디네이션(크로스오버 코디네이션이란 이질적인 두 개의 요소를 믹스하여 새로운 이미지 창출하는 것을 말함. vibe 속성을 적극적으로 활용. vibe 값의 예시는 feminine, masculine, elegant, sporty, romantic, punk, cute, tough, sophisticated, carefree, chic, lovely, sexy, classic이 있음. 크로스오버 코디네이션의 예시는 정장 재킷과 찢어진 청바지 매치, 로맨틱한 상의와 매니시한 팬츠 조합, 로맨틱한 레이스 블라우스와 밀리터리 팬츠 조합, 도시적 감각의 모던 재킷과 전원풍 감각의 플로럴 스커트, 클래식한 트렌치코트와 스트릿 감성의 그래피티 프린트 티셔츠 등이 있음. vibe만이 아니라 카테고리 속성으로도 크로스오버 스타일을 연출할 수 있음. 예를 들어 맨투맨, 후드 티셔츠나 트레이닝 팬츠를 코트와 조합, 셔츠/블라우스를 후드집업과 조합 등이 있음)
                - 심플한(isSimple 속성 값이 yes) 긴소매 티셔츠를 반소매나 민소매(isSimple 속성 값이 no)와 레이어드(top1과 top2는 color 속성 값이 서로 달라야 함)
                - 심플한(isSimple 속성 값이 yes) 셔츠/블라우스를 맨투맨, 후드 티셔츠, 니트, 반소매 티셔츠나 민소매 티셔츠(isSimple 속성 값이 no)와 레이어드(top1과 top2는 color 속성 값이 서로 달라야 함)
                - 바지를 원피스와 레이어드(만약 원피스가 민소매라면 상의를 추가로 레이어드 가능)
                - 바지를 랩스커트(skirt_type 속성 값이 wrap)와 레이어드하기(랩스커트와 슬림핏 바지 조합 불가)
                4. 만약 최소 기온이 17도 이상이라면 코디네이션 유형을 무시하고 크로스오버 코디네이션, 바지를 원피스와 레이어드, 바지를 랩스커트와 레이어드 중 하나를 선택하세요. 그렇지 않다면 5번으로 가세요.
                5. 코디네이션 유형을 지키세요.
                
                만약 위 지시문의 최종 결과로 크로스오버 코디네이션이 나왔다면 레이어드는 하지마세요. 반대로 레이어드가 나왔다면 크로스오버 코디네이션이나 다른 레이어드는 하지 마세요. 다만 아우터를 걸치는 것은 레이어드로 간주하지 않습니다.
                만약 사용자가 원하는 옷차림에 vibe 값을 명시했고 위 지시문의 최종 결과로 크로스오버 코디네이션이 나왔다면 해당 vibe와 서로 이질적인 vibe를 활용하세요.
                프린트는 pattern이 아니라 details로 분류합니다.
                원하는 옷차림에서 요구하는 디자인이 해당 카테고리에서 드문 디자인이라면 카테고리를 확장하세요. 예를 들어 동물 프린팅이 셔츠/블라우스에서 드문 디자인이므로 카테고리를 동물 프린팅이 흔한 티셔츠, 맨투맨이나 후드 티셔츠까지 확장하세요.
                만약 최소 기온이 23도 이상이면 아우터를 생략하세요.
                다른 무엇보다도 원하는 옷차림을 우선시해야 합니다.
                카라가 있는 상의 2개를 조합하지 마세요.
                슬림핏 아우터는 슬림핏 상의와 조합하세요.
                니트와 카디건을 조합하지 마세요.
                """, minTemp, maxTemp, desiredStyle, coordinationType, schedule, desiredStyle);
    }

    private Map<String, Object> extractJsonFromText(String text) {
        try {
            int startIdx = text.indexOf('{');
            int endIdx = text.lastIndexOf('}') + 1;

            if (startIdx >= 0 && endIdx > 0) {
                String jsonStr = text.substring(startIdx, endIdx);
                return objectMapper.readValue(jsonStr, Map.class);
            } else {
                throw new RuntimeException("JSON not found in response: " + text);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON from response: " + e.getMessage(), e);
        }
    }
}
