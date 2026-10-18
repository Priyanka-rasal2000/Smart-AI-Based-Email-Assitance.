package com.email.writer.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


import java.util.Map;

@Service
public class EmailGenratorService {

    private  final WebClient webClient;
@Value("${gemini.api.url}")
private String geminiApiUrl;
@Value("${gemini.api.key}")
private String geminiApiKey;

    public EmailGenratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        // Here we are building the prompt which will going as input to the backend....

        String prompt= buildPrompt(emailRequest);

        //Also crafting the request (request is suppose to be in certain format)

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text",prompt)
                                }
                        )
                }
        );

        // Do request and get response

        String response = webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //  Extract response and Return


        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            String text = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            return cleanText(text);

        } catch (Exception e) {
            return "Error processing request : " + e.getMessage();
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text
                .replace("\\n", "\n")
                .replaceAll("\n{2,}", "\n")
                .trim();
    }


    private String buildPrompt(EmailRequest emailRequest) {

        StringBuilder prompt =new StringBuilder();
        prompt.append("Generate a professional mail reply for the following email content. Please don't generate a subject Line ");
        if((emailRequest.getTone() != null) && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");

        }
        prompt.append("\n Original email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
