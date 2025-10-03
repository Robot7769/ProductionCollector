package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.ProductionCollector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BreakCollector implements Listener {
    private ProductionCollector plugin;

    public BreakCollector(ProductionCollector plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBarrelBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().toString().contains("BARREL")) {
            plugin.getLogger().info("B:" + event.getBlock().getLocation().toString());
        }

    }
}