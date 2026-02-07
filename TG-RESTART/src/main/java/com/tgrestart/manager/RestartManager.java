package com.tgrestart.manager;

import com.tgrestart.TGRestart;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages the restart timer and execution
 */
public class RestartManager {

    private final TGRestart plugin;
    private BukkitTask restartTask;
    private int remainingSeconds;
    private boolean restartScheduled;
    private final Set<Integer> broadcastedIntervals;

    public RestartManager(TGRestart plugin) {
        this.plugin = plugin;
        this.restartScheduled = false;
        this.broadcastedIntervals = new HashSet<>();
    }

    /**
     * Schedule a restart
     * @param seconds Seconds until restart
     */
    public void scheduleRestart(int seconds) {
        if (restartScheduled) {
            plugin.getLogger().warning("Attempted to schedule restart while one is already active");
            return;
        }

        this.remainingSeconds = seconds;
        this.restartScheduled = true;
        this.broadcastedIntervals.clear();

        plugin.debug("Scheduling restart in " + seconds + " seconds");

        // Send initial announcement
        plugin.getMessageManager().broadcastScheduleMessage(formatTime(seconds));

        // Get update interval from config (default 20 ticks = 1 second)
        int updateInterval = plugin.getConfig().getInt("update-interval", 20);

        // Start countdown task
        restartTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remainingSeconds <= 0) {
                executeRestart();
                return;
            }

            // Send countdown displays
            String timeString = formatTime(remainingSeconds);

            // Send title
            plugin.getMessageManager().sendCountdownTitle(timeString);

            // Send action bar
            plugin.getMessageManager().sendActionBar(timeString);

            // Check if we should send a broadcast message
            sendBroadcastIfNeeded(remainingSeconds);

            // Decrement timer
            remainingSeconds--;

        }, 0L, updateInterval);
    }

    /**
     * Cancel the active restart
     */
    public void cancelRestart() {
        if (!restartScheduled) {
            return;
        }

        plugin.debug("Cancelling restart");

        if (restartTask != null) {
            restartTask.cancel();
            restartTask = null;
        }

        restartScheduled = false;
        broadcastedIntervals.clear();

        // Send cancel message
        plugin.getMessageManager().broadcast(
                plugin.getMessageManager().getMessage("cancel-message")
        );
    }

    /**
     * Check if a restart is currently scheduled
     * @return True if restart is scheduled
     */
    public boolean isRestartScheduled() {
        return restartScheduled;
    }

    /**
     * Get remaining seconds until restart
     * @return Remaining seconds
     */
    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * Send broadcast message if at a configured interval
     * @param seconds Current remaining seconds
     */
    private void sendBroadcastIfNeeded(int seconds) {
        List<Integer> intervals = plugin.getConfig().getIntegerList("broadcast-intervals");
        if (intervals.contains(seconds) && !broadcastedIntervals.contains(seconds)) {
            broadcastedIntervals.add(seconds);
            plugin.getMessageManager().broadcastWarning(formatTime(seconds));
            plugin.debug("Sent broadcast for " + seconds + " seconds remaining");
        }
    }

    /**
     * Execute the restart
     */
    private void executeRestart() {
        plugin.debug("Executing restart");

        if (restartTask != null) {
            restartTask.cancel();
            restartTask = null;
        }

        restartScheduled = false;

        // Send final title
        plugin.getMessageManager().sendFinalTitle();

        // Execute pre-restart commands
        List<String> preCommands = plugin.getConfig().getStringList("pre-restart-commands");
        for (String command : preCommands) {
            plugin.debug("Executing pre-restart command: " + command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        // Kick all players
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> 
                player.kick(plugin.getMessageManager().getKickMessage())
            );

            // Execute restart based on configured method
            executeRestartMethod();

        }, 40L); // Wait 2 seconds before kicking
    }

    /**
     * Execute the configured restart method
     */
    private void executeRestartMethod() {
        String method = plugin.getConfig().getString("restart-method", "SPIGOT_RESTART").toUpperCase();
        plugin.debug("Using restart method: " + method);

        switch (method) {
            case "BUKKIT_SHUTDOWN":
                Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 20L);
                break;

            case "SPIGOT_RESTART":
                Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.spigot().restart(), 20L);
                break;

            case "COMMANDS_ONLY":
                List<String> commands = plugin.getConfig().getStringList("restart-commands");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (String command : commands) {
                        plugin.debug("Executing restart command: " + command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }, 20L);
                break;

            default:
                plugin.getLogger().warning("Unknown restart method: " + method + ". Using SPIGOT_RESTART");
                Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.spigot().restart(), 20L);
                break;
        }
    }

    /**
     * Format seconds into a readable time string
     * @param seconds Seconds to format
     * @return Formatted time string
     */
    public static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            int minutes = seconds / 60;
            int remainingSecs = seconds % 60;
            if (remainingSecs == 0) {
                return minutes + "m";
            }
            return minutes + "m " + remainingSecs + "s";
        } else {
            int hours = seconds / 3600;
            int remainingMins = (seconds % 3600) / 60;
            if (remainingMins == 0) {
                return hours + "h";
            }
            return hours + "h " + remainingMins + "m";
        }
    }

    /**
     * Parse time string to seconds
     * @param timeString Time string (e.g., "30s", "5m", "1h")
     * @return Seconds, or -1 if invalid
     */
    public static int parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return -1;
        }

        timeString = timeString.toLowerCase().trim();

        try {
            // Check for hours
            if (timeString.endsWith("h")) {
                int hours = Integer.parseInt(timeString.substring(0, timeString.length() - 1));
                return hours * 3600;
            }
            // Check for minutes
            else if (timeString.endsWith("m")) {
                int minutes = Integer.parseInt(timeString.substring(0, timeString.length() - 1));
                return minutes * 60;
            }
            // Check for seconds
            else if (timeString.endsWith("s")) {
                return Integer.parseInt(timeString.substring(0, timeString.length() - 1));
            }
            // Try parsing as plain seconds
            else {
                return Integer.parseInt(timeString);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
