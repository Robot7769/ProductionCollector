package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.ProductionCollector;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;

public class PlaceCollector implements Listener {
    private final ProductionCollector plugin;
    private final NamespacedKey ownerKey;

    public PlaceCollector(ProductionCollector plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "barrel_owner");
    }

    @EventHandler
    public void onBarrelPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType().toString().contains("BARREL")) {
            // Uložíme informaci o vlastníkovi do NBT dat bloku
            if (event.getBlock().getState() instanceof TileState tileState) {
                tileState.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, event.getPlayer().getUniqueId().toString());
                tileState.update();

                plugin.getLogger().info("P:" + event.getBlock().getLocation() + " Owner: " + event.getPlayer().getName());
            }
        }
    }
}