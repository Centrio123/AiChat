package lol.centrio.am.i.aiChat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;

public class ChatListener implements Listener {

    private final AiChatPlugin plugin;

    public ChatListener(AiChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (!msg.contains("@AI")) return;

        String prompt = msg.replace("@AI", "").trim();
        String botName = plugin.getConfig().getString("bot-name", "DeepSeek");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String response = APIHandler.getAIResponse(plugin, prompt);
            int maxLen = plugin.getConfig().getInt("max-length", 100);
            if (response.length() > maxLen) {
                response = response.substring(0, maxLen) + "...";
            }
            Component fullMessage = Component.text(botName + ": ").color(NamedTextColor.GREEN)
                    .append(Component.text(response).color(NamedTextColor.WHITE));
            Bukkit.broadcast(fullMessage);
        });
    }
}