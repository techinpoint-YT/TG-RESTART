package com.tgrestart.command;

import com.tgrestart.TGRestart;
import com.tgrestart.manager.RestartManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the /tgrestart command and its subcommands
 */
public class RestartCommand implements CommandExecutor, TabCompleter {

    private final TGRestart plugin;

    public RestartCommand(TGRestart plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("tgrestart.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }

        // No arguments - show usage
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "cancel":
                return handleCancel(sender);

            case "reload":
                return handleReload(sender);

            default:
                // Assume it's a time string
                return handleSchedule(sender, args[0]);
        }
    }

    /**
     * Handle scheduling a restart
     * @param sender Command sender
     * @param timeString Time string
     * @return True if handled
     */
    private boolean handleSchedule(CommandSender sender, String timeString) {
        // Check if restart is already scheduled
        if (plugin.getRestartManager().isRestartScheduled()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("restart-already-scheduled"));
            return true;
        }

        // Parse time
        int seconds = RestartManager.parseTime(timeString);
        if (seconds <= 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid-time-format"));
            return true;
        }

        // Schedule restart
        plugin.getRestartManager().scheduleRestart(seconds);
        plugin.getLogger().info(sender.getName() + " scheduled a restart in " + RestartManager.formatTime(seconds));

        return true;
    }

    /**
     * Handle cancelling a restart
     * @param sender Command sender
     * @return True if handled
     */
    private boolean handleCancel(CommandSender sender) {
        if (!plugin.getRestartManager().isRestartScheduled()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-restart-scheduled"));
            return true;
        }

        plugin.getRestartManager().cancelRestart();
        plugin.getLogger().info(sender.getName() + " cancelled the scheduled restart");

        return true;
    }

    /**
     * Handle reloading the config
     * @param sender Command sender
     * @return True if handled
     */
    private boolean handleReload(CommandSender sender) {
        plugin.reloadPlugin();
        sender.sendMessage(plugin.getMessageManager().getMessage("reload-success"));
        plugin.getLogger().info(sender.getName() + " reloaded the configuration");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        // Only suggest if they have permission
        if (!sender.hasPermission("tgrestart.admin")) {
            return completions;
        }

        if (args.length == 1) {
            // Suggest subcommands and time examples
            List<String> suggestions = Arrays.asList(
                    "cancel",
                    "reload",
                    "30s",
                    "1m",
                    "5m",
                    "10m",
                    "30m",
                    "1h"
            );

            String input = args[0].toLowerCase();
            for (String suggestion : suggestions) {
                if (suggestion.startsWith(input)) {
                    completions.add(suggestion);
                }
            }
        }

        return completions;
    }
}
