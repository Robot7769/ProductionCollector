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
    private final String objectiveName = "ProductionCollector";
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
        Inventory topInv = event.getView().getTopInventory();

        // Kontrola, jestli je top inventář barel
        if (topInv.getType() != InventoryType.BARREL) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Případ 1: Hráč klikl na item v barelu (vybírání z barelu) - původní funkčnost
        if (clickedInv != null && clickedInv.getType() == InventoryType.BARREL && current != null && cursor.getType().isAir()) {
            Material type = current.getType();
            if (!collecteble.contains(type)) return;

            // Počítá se score hráči, který na item kliknul (původní funkčnost)
            int amount = current.getAmount();
            addScore(player, type, amount);
            current.setAmount(0);
            event.setCancelled(true);
            clickedInv.remove(current);
            plugin.getLogger().info("Player " + player.getName() + " collected " + amount + " " + type + " from barrel");
        }
        // Případ 2: Hráč vkládá item do barelu (kliká s itemem na kurzoru do barelu)
        else if (clickedInv != null && clickedInv.getType() == InventoryType.BARREL && !cursor.getType().isAir()) {
            Material cursorType = cursor.getType();
            if (!collecteble.contains(cursorType)) return;

            // Získat vlastníka barelu - počítá se score vlastníkovi barelu
            OfflinePlayer barrelOwner = getBarrelOwner(topInv);
            if (barrelOwner != null) {
                int cursorAmount = cursor.getAmount();

                // Pokud kliká na prázdný slot nebo slot se stejným itemem
                if (current == null || current.getType().isAir() ||
                    (current.getType() == cursorType && current.getAmount() + cursorAmount <= current.getMaxStackSize())) {

                    addScore(barrelOwner, cursorType, cursorAmount);
                    // Smazat item z kurzoru hráče
                    event.setCancelled(true);
                    player.setItemOnCursor(null);
                    plugin.getLogger().info("Player " + player.getName() + " added " + cursorAmount + " " + cursorType + " to barrel owned by " + barrelOwner.getName());
                }
                // Pokud kliká na jiný typ itemu - vyměna
                else if (current.getType() != cursorType) {
                    // Započítat cursor item pro vlastníka barelu
                    addScore(barrelOwner, cursorType, cursorAmount);

                    // Pokud je current item taky collectible, započítat ho hráči
                    if (collecteble.contains(current.getType())) {
                        addScore(player, current.getType(), current.getAmount());
                    }

                    event.setCancelled(true);
                    player.setItemOnCursor(current.clone());
                    current.setType(cursorType);
                    current.setAmount(cursorAmount);
                    plugin.getLogger().info("Player " + player.getName() + " swapped items in barrel owned by " + barrelOwner.getName());
                }
            }
        }
        // Případ 3: Hráč shift-clickne na item ve svém inventáři (přesune do barelu)
        else if (clickedInv != null && clickedInv.getType() != InventoryType.BARREL && current != null && event.isShiftClick()) {
            Material type = current.getType();
            if (!collecteble.contains(type)) return;

            // Získat vlastníka barelu - počítá se score vlastníkovi barelu
            OfflinePlayer barrelOwner = getBarrelOwner(topInv);
            if (barrelOwner != null) {
                int amount = current.getAmount();
                addScore(barrelOwner, type, amount);
                // Smazat item z inventáře hráče
                event.setCancelled(true);
                current.setAmount(0);
                player.updateInventory();
                plugin.getLogger().info("Player " + player.getName() + " shift-clicked " + amount + " " + type + " to barrel owned by " + barrelOwner.getName());
            }
        }
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