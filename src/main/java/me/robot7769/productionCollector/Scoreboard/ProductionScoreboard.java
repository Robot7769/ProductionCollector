package me.robot7769.productionCollector.Scoreboard;

import me.robot7769.productionCollector.Database.DatabaseManager;
import me.robot7769.productionCollector.ProductionCollector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;

public class ProductionScoreboard {
    private final ProductionCollector plugin;
    private final DatabaseManager databaseManager;

    public ProductionScoreboard(ProductionCollector plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void updatePlayerScoreboard(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!databaseManager.isConnected()) {
                return;
            }

            String uuid = player.getUniqueId().toString();
            Map<Material, Integer> productions = databaseManager.getPlayerProductionCounts(uuid);
            int totalScore = databaseManager.getPlayerTotalScore(uuid);
            int rank = databaseManager.getPlayerRank(uuid);
            String rankRange = databaseManager.getRankRange(rank);

            Bukkit.getScheduler().runTask(plugin, () -> {
                displayScoreboard(player, productions, totalScore, rankRange);
            });
        });
    }

    private void displayScoreboard(Player player, Map<Material, Integer> productions, int totalScore, String rankRange) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        
        Objective objective = scoreboard.registerNewObjective(
            "production",
            Criteria.DUMMY,
            Component.text("⚡ Produkce ⚡")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
        );
        
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;

        // Prázdný řádek (neviditelný)
        objective.getScore(getInvisibleString(line)).setScore(line--);

        // Celkové skóre
        Team scoreTeam = scoreboard.registerNewTeam("score");
        String scoreEntry = getInvisibleString(line);
        scoreTeam.addEntry(scoreEntry);
        scoreTeam.prefix(Component.text("Score: ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(totalScore)
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)));
        objective.getScore(scoreEntry).setScore(line--);

        // Pořadí
        Team rankTeam = scoreboard.registerNewTeam("rank");
        String rankEntry = getInvisibleString(line);
        rankTeam.addEntry(rankEntry);
        rankTeam.prefix(Component.text("Pořadí: ")
            .color(NamedTextColor.AQUA)
            .append(Component.text(rankRange)
                .color(NamedTextColor.LIGHT_PURPLE)));
        objective.getScore(rankEntry).setScore(line--);

        // Prázdný řádek
        objective.getScore(getInvisibleString(line)).setScore(line--);

        // Produkce jednotlivých itemů
        if (!productions.isEmpty()) {
            Team headerTeam = scoreboard.registerNewTeam("header");
            String headerEntry = getInvisibleString(line);
            headerTeam.addEntry(headerEntry);
            headerTeam.prefix(Component.text("Produkce:")
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.UNDERLINED));
            objective.getScore(headerEntry).setScore(line--);

            int itemCount = 0;
            for (Map.Entry<Material, Integer> entry : productions.entrySet()) {
                if (itemCount >= 8) break; // Omezit na 8 itemů kvůli místu
                
                Material material = entry.getKey();
                int amount = entry.getValue();
                
                String materialName = getMaterialDisplayName(material);
                String emoji = getMaterialEmoji(material);
                
                Team itemTeam = scoreboard.registerNewTeam("item_" + itemCount);
                String itemEntry = getInvisibleString(line);
                itemTeam.addEntry(itemEntry);
                itemTeam.prefix(Component.text(emoji + " " + materialName + ": ")
                    .color(NamedTextColor.WHITE)
                    .append(Component.text(amount)
                        .color(NamedTextColor.YELLOW)));
                objective.getScore(itemEntry).setScore(line--);
                
                itemCount++;
            }
        } else {
            Team emptyTeam = scoreboard.registerNewTeam("empty");
            String emptyEntry = getInvisibleString(line);
            emptyTeam.addEntry(emptyEntry);
            emptyTeam.prefix(Component.text("Zatím žádná produkce")
                .color(NamedTextColor.DARK_GRAY));
            objective.getScore(emptyEntry).setScore(line--);
        }

        // Prázdný řádek na konci
        objective.getScore(getInvisibleString(line)).setScore(line--);

        player.setScoreboard(scoreboard);
    }

    // Vytvoří neviditelný string pomocí barevných kódů pro každý řádek
    private String getInvisibleString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("§").append(Integer.toHexString(i % 16));
        }
        return sb.toString();
    }

    private String getMaterialDisplayName(Material material) {
        return switch (material) {
            case PUMPKIN -> "Dýně";
            case CARVED_PUMPKIN -> "Vyřezaná dýně";
            case JACK_O_LANTERN -> "Jack O'Lantern";
            case PUMPKIN_PIE -> "Dýňový koláč";
            case MELON -> "Meloun";
            case CARROT -> "Mrkev";
            case POTATO -> "Brambora";
            case WHEAT -> "Pšenice";
            default -> material.name().replace("_", " ").toLowerCase();
        };
    }

    private String getMaterialEmoji(Material material) {
        return switch (material) {
            case PUMPKIN -> "🎃";
            case CARVED_PUMPKIN -> "🎃";
            case JACK_O_LANTERN -> "🎃";
            case PUMPKIN_PIE -> "🥧";
            case MELON -> "🍉";
            case CARROT -> "🥕";
            case POTATO -> "🥔";
            case WHEAT -> "🌾";
            default -> "•";
        };
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
}
