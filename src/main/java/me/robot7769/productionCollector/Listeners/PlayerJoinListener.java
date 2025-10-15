package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.ProductionCollector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    private final ProductionCollector plugin;

    public PlayerJoinListener(ProductionCollector plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Update scoreboard after a short delay to ensure player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getProductionScoreboard().updatePlayerScoreboard(event.getPlayer());
        }, 20L); // 1 second delay
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up scoreboard when player leaves
        plugin.getProductionScoreboard().removeScoreboard(event.getPlayer());
    }
}

