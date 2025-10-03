package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.ProductionCollector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

public class ProductionListener implements Listener {
    private ProductionCollector plugin;
    private List<Material> collecteble;
    final private String objectiveName = "ProductionCollector";

    public ProductionListener(ProductionCollector plugin) {
        this.plugin = plugin;
        //load collectable item from config
        collecteble = new ArrayList<>();
        collecteble.add(Material.PUMPKIN);
        collecteble.add(Material.PUMPKIN_PIE);

    }

    public List<Material> getCollecteble() {
        return collecteble;
    }

    private void addScore(Player player, Material material, int amount) {
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
        //remove item from inventory
        event.setCancelled(true);
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
        plugin.getLogger().info("Added " + amount + " of " + type + " in barrel ");

    }

}