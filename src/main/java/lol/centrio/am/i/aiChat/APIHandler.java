package lol.centrio.am.i.aiChat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class APIHandler {

    public static String getAIResponse(AiChatPlugin plugin, String prompt, UUID playerUuid) {
        String apiKey = plugin.getConfig().getString("api-key", "").trim();
        int maxLen = plugin.getConfig().getInt("max-length", 230);
        boolean memoryEnabled = plugin.getConfig().getBoolean("memory-enabled", true);
        int maxHistory = plugin.getConfig().getInt("max-history", 5);

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));

        String systemInstruction = "You are a helpful AI assistant integrated into a Minecraft server. " +
                "You are not a player, an entity, or a mob; you are a utility interface. " +
                "Keep your answers concise, game-focused, and strictly under " + maxLen + " characters. " +
                "The current date is " + currentDate + ".";

        HttpClient client = HttpClient.newHttpClient();
        String escapedPrompt = prompt.replace("\"", "\\\"");

        StringBuilder jsonMessages = new StringBuilder();
        jsonMessages.append(String.format("{\"role\": \"system\", \"content\": \"%s\"}", systemInstruction));

        if (memoryEnabled && playerUuid != null) {
            List<ChatMemory.ChatMessage> history = ChatMemory.getHistory(playerUuid);
            for (ChatMemory.ChatMessage msg : history) {
                jsonMessages.append(String.format(", {\"role\": \"%s\", \"content\": \"%s\"}",
                        msg.role, msg.content.replace("\"", "\\\"")));
            }
        }

        jsonMessages.append(String.format(", {\"role\": \"user\", \"content\": \"%s\"}", escapedPrompt));

        String json = String.format("{\"model\": \"deepseek-ai/DeepSeek-V4-Pro\", \"messages\": [%s]}", jsonMessages.toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepinfra.com/v1/openai/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "Error: API returned code " + response.statusCode();
            }

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            String aiContent = jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            if (memoryEnabled && playerUuid != null) {
                ChatMemory.addMessage(playerUuid, "user", prompt, maxHistory);
                ChatMemory.addMessage(playerUuid, "assistant", aiContent, maxHistory);
            }

            return aiContent.length() > maxLen ? aiContent.substring(0, maxLen - 3) + "..." : aiContent;

        } catch (Exception e) {
            return "Error contacting AI: " + e.getMessage();
        }
    }
}