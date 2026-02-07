package com.tgrestart;

import com.tgrestart.command.RestartCommand;
import com.tgrestart.manager.MessageManager;
import com.tgrestart.manager.RestartManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TGRestart - Professional server restart plugin with countdown titles
 * Main plugin class that initializes and manages the plugin lifecycle
 */
public class TGRestart extends JavaPlugin {

    private static TGRestart instance;
    private RestartManager restartManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config if it doesn't exist
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Initialize managers
        messageManager = new MessageManager(this);
        restartManager = new RestartManager(this);

        // Register commands
        RestartCommand restartCommand = new RestartCommand(this);
        getCommand("tgrestart").setExecutor(restartCommand);
        getCommand("tgrestart").setTabCompleter(restartCommand);

        // Log startup message
        getLogger().info("TGRestart v" + getDescription().getVersion() + " has been enabled!");
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("Debug mode is enabled");
        }
    }

    @Override
    public void onDisable() {
        // Cancel any active restart timer
        if (restartManager != null && restartManager.isRestartScheduled()) {
            restartManager.cancelRestart();
            getLogger().info("Cancelled active restart timer due to plugin shutdown");
        }

        getLogger().info("TGRestart has been disabled!");
    }

    /**
     * Get the plugin instance
     * @return Plugin instance
     */
    public static TGRestart getInstance() {
        return instance;
    }

    /**
     * Get the restart manager
     * @return RestartManager instance
     */
    public RestartManager getRestartManager() {
        return restartManager;
    }

    /**
     * Get the message manager
     * @return MessageManager instance
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Reload the plugin configuration
     */
    public void reloadPlugin() {
        reloadConfig();
        messageManager = new MessageManager(this);
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("Configuration reloaded successfully");
        }
    }

    /**
     * Log debug message if debug mode is enabled
     * @param message Message to log
     */
    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
