package com.tgrestart.manager;

import com.tgrestart.TGRestart;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Duration;

/**
 * Manages all message formatting and display using MiniMessage
 */
public class MessageManager {

    private final TGRestart plugin;
    private final MiniMessage miniMessage;
    private FileConfiguration messagesConfig;

    public MessageManager(TGRestart plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        loadMessages();
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Parse a MiniMessage string into a Component
     * @param message Message string with MiniMessage formatting
     * @return Parsed Component
     */
    public Component parse(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(message);
    }

    /**
     * Parse a message with placeholders
     * @param message Message string
     * @param placeholder Placeholder to replace
     * @param value Value to replace with
     * @return Parsed Component
     */
    public Component parse(String message, String placeholder, String value) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(message.replace(placeholder, value));
    }

    /**
     * Broadcast a message to all online players
     * @param message Message to broadcast
     */
    public void broadcast(Component message) {
        Bukkit.getServer().broadcast(message);
    }

    /**
     * Broadcast schedule message with time
     * @param timeString Time string to replace %time% with
     */
    public void broadcastScheduleMessage(String timeString) {
        String message = messagesConfig.getString("schedule-message", "");
        if (!message.isEmpty()) {
            broadcast(parse(message, "%time%", timeString));
        }
    }

    /**
     * Broadcast warning message with time
     * @param timeString Time string to replace %time% with
     */
    public void broadcastWarning(String timeString) {
        String message = messagesConfig.getString("warning-message", "");
        if (!message.isEmpty()) {
            broadcast(parse(message, "%time%", timeString));
        }
    }

    /**
     * Send a title to all online players
     * @param titleText Title text
     * @param subtitleText Subtitle text
     * @param fadeIn Fade in time in ticks
     * @param stay Stay time in ticks
     * @param fadeOut Fade out time in ticks
     */
    public void sendTitleToAll(String titleText, String subtitleText, int fadeIn, int stay, int fadeOut) {
        Component title = parse(titleText);
        Component subtitle = parse(subtitleText);

        Title titleObj = Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(titleObj);
        }
    }

    /**
     * Send countdown title to all online players
     * @param timeString Time string to display
     */
    public void sendCountdownTitle(String timeString) {
        if (!plugin.getConfig().getBoolean("titles.enabled", true)) {
            return;
        }

        String titleText = plugin.getConfig().getString("titles.title", "<red><bold>Server Restart");
        String subtitleText = plugin.getConfig().getString("titles.subtitle", "<yellow>Restarting in <white>%time%");
        int fadeIn = plugin.getConfig().getInt("titles.fade-in", 10);
        int stay = plugin.getConfig().getInt("titles.stay", 30);
        int fadeOut = plugin.getConfig().getInt("titles.fade-out", 10);

        subtitleText = subtitleText.replace("%time%", timeString);

        sendTitleToAll(titleText, subtitleText, fadeIn, stay, fadeOut);
    }

    /**
     * Send final restart title to all online players
     */
    public void sendFinalTitle() {
        if (!plugin.getConfig().getBoolean("final-title.enabled", true)) {
            return;
        }

        String titleText = plugin.getConfig().getString("final-title.title", "<dark_red><bold>Server Restarting");
        String subtitleText = plugin.getConfig().getString("final-title.subtitle", "<red>Please reconnect in a moment...");
        int fadeIn = plugin.getConfig().getInt("final-title.fade-in", 10);
        int stay = plugin.getConfig().getInt("final-title.stay", 40);
        int fadeOut = plugin.getConfig().getInt("final-title.fade-out", 10);

        sendTitleToAll(titleText, subtitleText, fadeIn, stay, fadeOut);
    }

    /**
     * Send action bar to all online players
     * @param timeString Time string to display
     */
    public void sendActionBar(String timeString) {
        if (!plugin.getConfig().getBoolean("action-bar.enabled", true)) {
            return;
        }

        String format = plugin.getConfig().getString("action-bar.format", "<gold><bold>⚠ <yellow>Restart in <white>%time% <gold><bold>⚠");
        Component message = parse(format, "%time%", timeString);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(message);
        }
    }

    /**
     * Get a message from messages.yml with prefix
     * @param path Path to message in messages.yml
     * @return Parsed Component
     */
    public Component getMessage(String path) {
        String prefix = messagesConfig.getString("prefix", "");
        String message = messagesConfig.getString(path, "");
        return parse(prefix + message);
    }

    /**
     * Get kick message component
     * @return Kick message Component
     */
    public Component getKickMessage() {
        String message = String.join("\n", messagesConfig.getStringList("kick-message"));
        if (message.isEmpty()) {
            message = messagesConfig.getString("kick-message", "<red>Server restarting!");
        }
        return parse(message);
    }
}
