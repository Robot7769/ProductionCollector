package me.robot7769.productionCollector;

import me.robot7769.productionCollector.Listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProductionCollector extends JavaPlugin {

    @Override
    public void onEnable() {

        // Register listeners
        getServer().getPluginManager().registerEvents(new ProductionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlaceCollector(this), this);
        getServer().getPluginManager().registerEvents(new BreakCollector(this), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
