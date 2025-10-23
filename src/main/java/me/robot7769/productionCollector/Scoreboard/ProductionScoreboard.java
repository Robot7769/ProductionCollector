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

        // Používáme statické entry bez čísel
        String[] entries = new String[16];
        for (int i = 0; i < 16; i++) {
            entries[i] = "§" + Integer.toHexString(i) + "§r";
        }

        int currentLine = 15; // Začínáme shora (vyšší číslo = vyšší pozice)

        // Prázdný řádek
        addStaticLine(scoreboard, objective, entries[15 - currentLine], "", currentLine--);

        // Celkové skóre
        addStaticLine(scoreboard, objective, entries[15 - currentLine],
            Component.text("Score: ").color(NamedTextColor.YELLOW)
                .append(Component.text(totalScore).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)), currentLine--);

        // Pořadí
        addStaticLine(scoreboard, objective, entries[15 - currentLine],
            Component.text("Pořadí: ").color(NamedTextColor.AQUA)
                .append(Component.text(rankRange).color(NamedTextColor.LIGHT_PURPLE)), currentLine--);

        // Prázdný řádek
        addStaticLine(scoreboard, objective, entries[15 - currentLine], " ", currentLine--);

        // Produkce jednotlivých itemů
        if (!productions.isEmpty()) {
            addStaticLine(scoreboard, objective, entries[15 - currentLine],
                Component.text("Produkce:").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED), currentLine--);

            int itemCount = 0;
            for (Map.Entry<Material, Integer> entry : productions.entrySet()) {
                if (itemCount >= 8 || currentLine < 0) break;

                Material material = entry.getKey();
                int amount = entry.getValue();
                
                String materialName = getMaterialDisplayName(material);
                String emoji = getMaterialEmoji(material);
                
                addStaticLine(scoreboard, objective, entries[15 - currentLine],
                    Component.text(emoji + " " + materialName + ": ").color(NamedTextColor.WHITE)
                        .append(Component.text(amount).color(NamedTextColor.YELLOW)), currentLine--);

                itemCount++;
            }
        } else {
            addStaticLine(scoreboard, objective, entries[15 - currentLine],
                Component.text("Zatím žádná produkce").color(NamedTextColor.DARK_GRAY), currentLine--);
        }

        // Prázdný řádek na konci
        if (currentLine >= 0) {
            addStaticLine(scoreboard, objective, entries[15 - currentLine], "  ", currentLine);
        }

        // Použijeme NumberFormat pro skrytí čísel (dostupné v Paper 1.20.3+)
        try {
            // Zkusíme použít blank number format pro skrytí čísel
            objective.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        } catch (Exception | NoSuchMethodError | NoClassDefFoundError e) {
            // Pokud NumberFormat není k dispozici, ignorujeme
        }

        player.setScoreboard(scoreboard);
    }

    // Nová metoda pro přidání statického řádku bez čísel
    private void addStaticLine(Scoreboard scoreboard, Objective objective, String entry, Object content, int score) {
        Team team = scoreboard.registerNewTeam("line_" + entry.replace("§", "").replace("r", ""));
        team.addEntry(entry);

        if (content instanceof String) {
            team.prefix(Component.text((String) content));
        } else if (content instanceof Component) {
            team.prefix((Component) content);
        }

        team.suffix(Component.empty());

        // Musíme zaregistrovat score, aby se řádek zobrazil
        Score lineScore = objective.getScore(entry);
        lineScore.setScore(score);

        // Pokusíme se skrýt číslo pomocí NumberFormat (Paper API 1.20.3+)
        try {
            lineScore.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        } catch (Exception | NoSuchMethodError | NoClassDefFoundError e) {
            // NumberFormat není k dispozici v této verzi
        }
    }

    // Pomocná metoda pro přidání řádku bez čísel - DEPRECATED, používáme addStaticLine
    private void addLine(Scoreboard scoreboard, Objective objective, String text, int score) {
        Team team = scoreboard.registerNewTeam("line_" + score);
        String entry = getInvisibleString(score);
        team.addEntry(entry);
        team.prefix(Component.text(text));
        objective.getScore(entry).setScore(score);
    }

    // Vytvoří SKUTEČNĚ neviditelný unikátní string pro každý řádek
    private String getInvisibleString(int index) {
        // Použijeme kombinaci barevných kódů a reset kódů aby byl každý string unikátní
        StringBuilder builder = new StringBuilder();
        String[] colors = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"};

        // Vytvoříme unikátní neviditelný string z indexu
        int temp = index;
        do {
            builder.append(colors[temp % 16]).append("§r");
            temp /= 16;
        } while (temp > 0);

        return builder.toString();
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
