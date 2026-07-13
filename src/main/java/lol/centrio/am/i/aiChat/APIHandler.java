package lol.centrio.am.i.aiChat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class APIHandler {
    public static String getAIResponse(AiChatPlugin plugin, String prompt) {
        String apiKey = plugin.getConfig().getString("api-key", "").trim();
        int maxLen = plugin.getConfig().getInt("max-length", 230); // Default to 230

        // Define the instruction for the AI
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));

// Define the persona
        String systemInstruction = "You are a helpful AI assistant integrated into a Minecraft server. " +
                "You are not a player, an entity, or a mob; you are a utility interface. " +
                "Keep your answers concise, game-focused, and strictly under " + maxLen + " characters. " +
                "The current date is " + currentDate + ".";

        HttpClient client = HttpClient.newHttpClient();
        String escapedPrompt = prompt.replace("\"", "\\\"");

        // Construct JSON payload with System role to enforce the limit
        String json = String.format(
                "{\"model\": \"deepseek-ai/DeepSeek-V4-Pro\", \"messages\": [" +
                        "{\"role\": \"system\", \"content\": \"%s\"}," +
                        "{\"role\": \"user\", \"content\": \"%s\"}" +
                        "]}",
                systemInstruction, escapedPrompt
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepinfra.com/v1/openai/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "Error: API returned " + response.statusCode();
            }

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            String aiContent = jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            // Final safety check to ensure it fits the Minecraft message requirement
            return aiContent.length() > maxLen ? aiContent.substring(0, maxLen - 3) + "..." : aiContent;

        } catch (Exception e) {
            return "Error contacting AI: " + e.getMessage();
        }
    }
}