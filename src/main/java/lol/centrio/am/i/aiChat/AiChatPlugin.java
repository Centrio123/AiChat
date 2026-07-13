package lol.centrio.am.i.aiChat;

import org.bukkit.plugin.java.JavaPlugin;

public class AiChatPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }
}