package lol.centrio.am.i.aiChat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

        List<String> contextLines = plugin.getConfig().getStringList("Context");
        StringBuilder systemInstructionBuilder = new StringBuilder();

        if (contextLines.isEmpty()) {
            systemInstructionBuilder.append("You are a helpful AI assistant on a Minecraft server.");
        } else {
            for (String line : contextLines) {
                systemInstructionBuilder.append(line).append(" ");
            }
        }

        // Get the player's name safely from their UUID
        String playerName = "Player";
        if (playerUuid != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            if (offlinePlayer.getName() != null) {
                playerName = offlinePlayer.getName();
            }
        }

        // Append player identity and current date to the system instruction
        systemInstructionBuilder.append(" The player currently talking to you is named ").append(playerName).append(".");

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        systemInstructionBuilder.append(" The current date is ").append(currentDate).append(".");
        systemInstructionBuilder.append(" Keep your response strictly under ").append(maxLen).append(" characters.");

        String systemInstruction = systemInstructionBuilder.toString();

        HttpClient client = HttpClient.newHttpClient();
        String escapedPrompt = prompt.replace("\"", "\\\"");

        StringBuilder jsonMessages = new StringBuilder();
        jsonMessages.append(String.format("{\"role\": \"system\", \"content\": \"%s\"}", systemInstruction.replace("\"", "\\\"")));

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