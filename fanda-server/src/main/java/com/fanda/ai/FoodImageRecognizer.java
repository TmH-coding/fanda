package com.fanda.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 菜品图片识别服务 — 调用通义千问 qwen-vl-plus 视觉模型
 */
@Service
@Slf4j
public class FoodImageRecognizer {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String VL_API_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    /**
     * 识别图片中的菜品，返回菜名、描述和建议记录信息
     */
    public String recognize(String imageBase64) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> imageContent = Map.of(
                "image", "data:image/jpeg;base64," + imageBase64
        );
        Map<String, Object> textContent = Map.of(
                "text", """
                        请识别这张图片中的食物或菜品。
                        按以下格式回答（用中文）：
                        菜名：xxx
                        菜系/类型：xxx（如中式、西式、快餐、轻食等）
                        主要食材：xxx
                        预估热量：xxx 千卡（大致范围）
                        营养特点：xxx（1句话）
                        建议记录为：xxx（给出一个简短的菜名，方便记录到饮食日志）

                        如果图片中没有食物，请回复：未识别到食物图片
                        """
        );

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", List.of(imageContent, textContent)
        );

        Map<String, Object> input = Map.of("messages", List.of(message));
        Map<String, Object> parameters = Map.of("result_format", "message");

        Map<String, Object> requestBody = Map.of(
                "model", "qwen-vl-plus",
                "input", input,
                "parameters", parameters
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(VL_API_URL, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractContent(response.getBody());
            }
            return "识别服务暂时不可用，请稍后再试";
        } catch (Exception e) {
            log.error("菜品识别失败", e);
            return "识别失败：" + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        try {
            Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
            Map<String, Object> choice = (Map<String, Object>) ((List<?>) output.get("choices")).get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            List<Map<String, Object>> content = (List<Map<String, Object>>) message.get("content");
            return content.get(0).get("text").toString();
        } catch (Exception e) {
            return "解析识别结果失败";
        }
    }
}
