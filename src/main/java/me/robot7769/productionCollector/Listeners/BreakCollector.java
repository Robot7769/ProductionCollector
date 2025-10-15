package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.ProductionCollector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
        if (!event.getBlock().getType().toString().contains("BARREL")) return;
        String ownerInfo = "";

        // Načteme informaci o vlastníkovi z NBT dat
        if (event.getBlock().getState() instanceof TileState tileState) {
            String ownerUUID = tileState.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

            if (ownerUUID == null) {
                plugin.getLogger().warning("B:" + event.getBlock().getLocation() + " NO OWNER UUID");
                return;
            }
            try {
                UUID uuid = UUID.fromString(ownerUUID);
                String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                ownerInfo = " Owner: " + (playerName != null ? playerName : ownerUUID);
                Player p = event.getPlayer();
                if (playerName != null && !p.getName().equals(playerName)) {
                    event.setCancelled(true);
                    p.sendMessage("§cBarrel nepatří Tobě! Nedělej to!");
                    p.showTitle(new WarningTitle());
                    p.setHealth(1.0);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 1200, 5));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 1200, 5));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 1200, 5));
                    plugin.getServer().sendMessage(Component.text("Hráč " + p.getName() + " ničí cizí Barrely!" , TextColor.color(255, 14, 0)));
                    plugin.getLogger().warning("B:" + event.getBlock().getLocation() + " hráč: " + p.getName() + " ničí barrel " + ownerInfo);
                    return;
                } else if (playerName != null) {
                    plugin.getLogger().warning("B:" + event.getBlock().getLocation() + " " + ownerInfo + " si zničil");
                    return;
                }
            } catch (IllegalArgumentException e) {
                ownerInfo = " Owner: " + ownerUUID;
            }

        }

        plugin.getLogger().warning("B:" + event.getBlock().getLocation() + " uuid:" + ownerInfo);
    }

}