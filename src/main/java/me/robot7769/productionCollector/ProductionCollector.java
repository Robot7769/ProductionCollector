package me.robot7769.productionCollector;

import me.robot7769.productionCollector.Commands.ScoreboardCommand;
import me.robot7769.productionCollector.Database.DatabaseManager;
import me.robot7769.productionCollector.Listeners.*;
import me.robot7769.productionCollector.Scoreboard.ProductionScoreboard;
import me.robot7769.productionCollector.Utils.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProductionCollector extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ProductionScoreboard productionScoreboard;
    private Logger customLogger;

    @Override
    public void onEnable() {
        // Save default config if not exists
        saveDefaultConfig();

        // Initialize custom logger
        customLogger = new Logger(this);

        // Initialize database
        databaseManager = new DatabaseManager(this, customLogger);
        databaseManager.connect();

        // Initialize scoreboard
        productionScoreboard = new ProductionScoreboard(this, databaseManager);

        // Register listeners
        getServer().getPluginManager().registerEvents(new ProductionListener(this, databaseManager, productionScoreboard, customLogger), this);
        getServer().getPluginManager().registerEvents(new PlaceCollector(this), this);
        getServer().getPluginManager().registerEvents(new BreakCollector(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Register commands
        var scoreboardCommand = new ScoreboardCommand(this);
        var command = getCommand("scoreboard");
        if (command != null) {
            command.setExecutor(scoreboardCommand);
            command.setTabCompleter(scoreboardCommand);
        } else {
            customLogger.warning("Failed to register scoreboard command!");
        }
    }

    @Override
    public void onDisable() {
        // Disconnect database
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        if (customLogger != null) {
            customLogger.info("ProductionCollector disabled");
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ProductionScoreboard getProductionScoreboard() {
        return productionScoreboard;
    }

    public Logger getCustomLogger() {
        return customLogger;
    }
}
