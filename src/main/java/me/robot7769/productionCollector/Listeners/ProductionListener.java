package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.ProductionCollector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductionListener implements Listener {
    private final ProductionCollector plugin;
    private List<Material> collecteble;
    final private String objectiveName = "ProductionCollector";
    private final NamespacedKey ownerKey;

    public ProductionListener(ProductionCollector plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "barrel_owner");
        //load collectable item from config
        collecteble = new ArrayList<>();
        collecteble.add(Material.PUMPKIN);
        collecteble.add(Material.PUMPKIN_PIE);

    }

    public List<Material> getCollecteble() {
        return collecteble;
    }

    private void addScore(OfflinePlayer player, Material material, int amount) {
        if (amount <= 0) return;
        if (material == Material.PUMPKIN_PIE) {
            amount *= 2; //pumpkin pie is worth double
        }
        plugin.getLogger().info("Added " + amount + " of " + material + " to " + player.getName() + "'s score.");

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective obj = sb.getObjective(objectiveName);
        if (obj == null) {
            obj = sb.registerNewObjective(objectiveName, "dummy", "Produkce");
        }
        Score score = obj.getScore(player.getName());
        score.setScore(score.getScore() + amount);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        if (clickedInv.getType() != InventoryType.BARREL) return;

        ItemStack current = event.getCurrentItem();
        if (current == null) return;

        Material type = current.getType();
        if (!collecteble.contains(type)) return;
        int amount = current.getAmount();
        addScore(player, type, amount);
        current.setAmount(0);
        //remove item from inventory
        event.setCancelled(true);
        clickedInv.remove(current);
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (destination.getType() != InventoryType.BARREL) return;

        ItemStack item = event.getItem();
        Material type = item.getType();
        if (!collecteble.contains(type)) return;
        int amount = item.getAmount();

        //barrel owner
        OfflinePlayer barrelOwner = getBarrelOwner(destination);
        if (barrelOwner != null) {
            addScore(barrelOwner, type, amount);
            item.setAmount(0);
            // Smazat item z barelu aby se nemohl znovu započítat
            //event.setCancelled(true); // Zruší přesun
            // Nebo alternativně můžeme item smazat z cílového inventáře:
            destination.removeItem(item);
        } else {
            plugin.getLogger().warning("Could not determine barrel owner");
        }
    }

    private OfflinePlayer getBarrelOwner(Inventory barrelInventory) {
        if (barrelInventory.getLocation() == null) return null;

        if (barrelInventory.getLocation().getBlock().getState() instanceof TileState tileState) {
            String ownerUUID = tileState.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

            if (ownerUUID != null) {
                try {
                    UUID uuid = UUID.fromString(ownerUUID);
                    return Bukkit.getOfflinePlayer(uuid); // Vrátí OfflinePlayer i pro offline hráče
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in barrel data: " + ownerUUID);
                }
            }
        }
        return null;
    }

}