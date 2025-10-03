package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.ProductionCollector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlaceCollector implements Listener {
    private ProductionCollector plugin;

    public PlaceCollector(ProductionCollector plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBarrelPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType().toString().contains("BARREL")) {
            plugin.getLogger().info("P:" + event.getBlock().getLocation().toString());
        }
    }
}