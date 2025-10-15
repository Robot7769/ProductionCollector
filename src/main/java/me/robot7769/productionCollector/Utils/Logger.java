package me.robot7769.productionCollector.Utils;

import me.robot7769.productionCollector.ProductionCollector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private final ProductionCollector plugin;
    private final File logFile;
    private final LogLevel configuredLevel;
    private final boolean logToFile;
    private final SimpleDateFormat dateFormat;

    public enum LogLevel {
        NONE(0),
        ERROR(1),
        WARNING(2),
        INFO(3),
        DEBUG(4);

        private final int level;

        LogLevel(int level) {
            this.level = level;
        }

        public boolean shouldLog(LogLevel messageLevel) {
            return messageLevel.level <= this.level;
        }
    }

    public Logger(ProductionCollector plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Load configuration
        String levelStr = plugin.getConfig().getString("log_level", "INFO").toUpperCase();
        this.configuredLevel = parseLogLevel(levelStr);
        this.logToFile = plugin.getConfig().getBoolean("log_to_file", true);
        
        // Setup log file
        if (logToFile) {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            this.logFile = new File(dataFolder, "production.log");
            
            // Log startup
            info("=== ProductionCollector Logger Initialized ===");
            info("Log Level: " + configuredLevel.name());
            info("Log to File: " + logToFile);
        } else {
            this.logFile = null;
        }
    }

    private LogLevel parseLogLevel(String level) {
        try {
            return LogLevel.valueOf(level);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid log level '" + level + "', using INFO");
            return LogLevel.INFO;
        }
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message + ": " + throwable.getMessage());
        if (logToFile && configuredLevel.shouldLog(LogLevel.ERROR)) {
            writeToFile(LogLevel.ERROR, message, throwable);
        }
    }

    private void log(LogLevel level, String message) {
        if (!configuredLevel.shouldLog(level)) {
            return;
        }

        // Log to console
        switch (level) {
            case ERROR -> plugin.getLogger().severe(message);
            case WARNING -> plugin.getLogger().warning(message);
            case INFO -> plugin.getLogger().info(message);
            case DEBUG -> plugin.getLogger().info("[DEBUG] " + message);
        }

        // Log to file
        if (logToFile) {
            writeToFile(level, message, null);
        }
    }

    private void writeToFile(LogLevel level, String message, Throwable throwable) {
        if (logFile == null) return;

        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = dateFormat.format(new Date());
            pw.println("[" + timestamp + "] [" + level.name() + "] " + message);
            
            if (throwable != null) {
                throwable.printStackTrace(pw);
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
        }
    }

    public void logProduction(String playerName, String material, int amount, int score, String action) {
        debug("Production: " + playerName + " " + action + " " + amount + "x " + material + " (+" + score + " score)");
    }

    public void logBarrelAction(String playerName, String ownerName, String action, String material, int amount) {
        debug("Barrel: " + playerName + " " + action + " " + amount + "x " + material + " to/from barrel owned by " + ownerName);
    }
}

