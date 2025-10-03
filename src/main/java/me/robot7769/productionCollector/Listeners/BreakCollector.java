package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.ProductionCollector;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class BreakCollector implements Listener {
    private final ProductionCollector plugin;
    private final NamespacedKey ownerKey;

    public BreakCollector(ProductionCollector plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "barrel_owner");
    }

    @EventHandler
    public void onBarrelBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().toString().contains("BARREL")) {
            String ownerInfo = "";

            // Načteme informaci o vlastníkovi z NBT dat
            if (event.getBlock().getState() instanceof TileState tileState) {
                String ownerUUID = tileState.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

                if (ownerUUID != null) {
                    try {
                        UUID uuid = UUID.fromString(ownerUUID);
                        String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                        ownerInfo = " Owner: " + (playerName != null ? playerName : ownerUUID);
                    } catch (IllegalArgumentException e) {
                        ownerInfo = " Owner: " + ownerUUID;
                    }
                }
            }

            plugin.getLogger().warning("B:" + event.getBlock().getLocation() + ownerInfo);
        }

    }
}