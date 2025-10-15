package me.robot7769.productionCollector.Listeners;

import me.robot7769.productionCollector.Database.DatabaseManager;
import me.robot7769.productionCollector.ProductionCollector;
import me.robot7769.productionCollector.Scoreboard.ProductionScoreboard;
import me.robot7769.productionCollector.Utils.Logger;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProductionListener implements Listener {
    private final ProductionCollector plugin;
    private final DatabaseManager databaseManager;
    private final ProductionScoreboard productionScoreboard;
    private final Logger logger;
    private Map<Material, Integer> collectableItems;
    private final NamespacedKey ownerKey;

    public ProductionListener(ProductionCollector plugin, DatabaseManager databaseManager, ProductionScoreboard productionScoreboard, Logger logger) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.productionScoreboard = productionScoreboard;
        this.logger = logger;
        this.ownerKey = new NamespacedKey(plugin, "barrel_owner");

        // Load collectable items from config
        loadCollectableItems();
    }

    private void loadCollectableItems() {
        collectableItems = new HashMap<>();

        if (plugin.getConfig().contains("collectable_items")) {
            var section = plugin.getConfig().getConfigurationSection("collectable_items");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        Material material = Material.valueOf(key.toUpperCase());
                        int score = plugin.getConfig().getInt("collectable_items." + key);
                        collectableItems.put(material, score);
                        logger.info("Loaded collectable item: " + material + " with score value: " + score);
                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid material in config: " + key);
                    }
                }
            }
        } else {
            // Default values if config is missing
            logger.warning("No collectable_items found in config, using defaults");
            collectableItems.put(Material.PUMPKIN, 3);
            collectableItems.put(Material.PUMPKIN_PIE, 8);
        }
    }

    public Map<Material, Integer> getCollectableItems() {
        return collectableItems;
    }

    private void addScore(OfflinePlayer player, Material material, int amount) {
        if (amount <= 0) return;

        Integer scoreValue = collectableItems.get(material);
        if (scoreValue == null) return;

        int totalScore = amount * scoreValue;

        // Log to database
        databaseManager.logProduction(
            player.getName(),
            player.getUniqueId().toString(),
            material,
            amount,
            scoreValue,
            totalScore
        );

        // Log production (DEBUG level - detailní info)
        logger.logProduction(player.getName(), material.name(), amount, totalScore, "earned");

        // Update scoreboard if player is online
        if (player.isOnline() && player.getPlayer() != null) {
            productionScoreboard.updatePlayerScoreboard(player.getPlayer());
        }
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
            if (!collectableItems.containsKey(type)) return;

            // Počítá se score hráči, který na item kliknul (původní funkčnost)
            int amount = current.getAmount();
            addScore(player, type, amount);
            current.setAmount(0);
            event.setCancelled(true);
            clickedInv.remove(current);
            logger.logBarrelAction(player.getName(), player.getName(), "collected", type.name(), amount);
        }
        // Případ 2: Hráč vkládá item do barelu (kliká s itemem na kurzoru do barelu)
        else if (clickedInv != null && clickedInv.getType() == InventoryType.BARREL && !cursor.getType().isAir()) {
            Material cursorType = cursor.getType();
            if (!collectableItems.containsKey(cursorType)) return;

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
                    logger.logBarrelAction(player.getName(), barrelOwner.getName(), "added", cursorType.name(), cursorAmount);
                }
                // Pokud kliká na jiný typ itemu - vyměna
                else if (current.getType() != cursorType) {
                    // Započítat cursor item pro vlastníka barelu
                    addScore(barrelOwner, cursorType, cursorAmount);

                    // Pokud je current item taky collectible, započítat ho hráči
                    if (collectableItems.containsKey(current.getType())) {
                        addScore(player, current.getType(), current.getAmount());
                    }

                    event.setCancelled(true);
                    player.setItemOnCursor(current.clone());
                    current.setType(cursorType);
                    current.setAmount(cursorAmount);
                    logger.logBarrelAction(player.getName(), barrelOwner.getName(), "swapped", cursorType.name(), cursorAmount);
                }
            }
        }
        // Případ 3: Hráč shift-clickne na item ve svém inventáři (přesune do barelu)
        else if (clickedInv != null && clickedInv.getType() != InventoryType.BARREL && current != null && event.isShiftClick()) {
            Material type = current.getType();
            if (!collectableItems.containsKey(type)) return;

            // Získat vlastníka barelu - počítá se score vlastníkovi barelu
            OfflinePlayer barrelOwner = getBarrelOwner(topInv);
            if (barrelOwner != null) {
                int amount = current.getAmount();
                addScore(barrelOwner, type, amount);
                // Smazat item z inventáře hráče
                event.setCancelled(true);
                current.setAmount(0);
                player.updateInventory();
                logger.logBarrelAction(player.getName(), barrelOwner.getName(), "shift-added", type.name(), amount);
            }
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (destination.getType() != InventoryType.BARREL) return;

        ItemStack item = event.getItem();
        Material type = item.getType();
        if (!collectableItems.containsKey(type)) return;
        int amount = item.getAmount();

        //barrel owner
        OfflinePlayer barrelOwner = getBarrelOwner(destination);
        if (barrelOwner != null) {
            addScore(barrelOwner, type, amount);
            item.setAmount(0);
            destination.removeItem(item);
            logger.debug("Hopper moved " + amount + "x " + type + " to barrel owned by " + barrelOwner.getName());
        } else {
            logger.warning("Could not determine barrel owner for hopper transfer");
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
                    logger.warning("Invalid UUID in barrel data: " + ownerUUID);
                }
            }
        }
        return null;
    }

}