package me.robot7769.productionCollector.Commands;

import me.robot7769.productionCollector.ProductionCollector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardCommand implements CommandExecutor, TabCompleter {
    private final ProductionCollector plugin;

    public ScoreboardCommand(ProductionCollector plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            // Refresh scoreboard
            plugin.getProductionScoreboard().updatePlayerScoreboard(player);
            player.sendMessage(Component.text("✓ Scoreboard aktualizován!").color(NamedTextColor.GREEN));
            return true;
        }

        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "refresh", "reload", "update" -> {
                    plugin.getProductionScoreboard().updatePlayerScoreboard(player);
                    player.sendMessage(Component.text("✓ Scoreboard aktualizován!").color(NamedTextColor.GREEN));
                    return true;
                }
                case "hide", "off" -> {
                    plugin.getProductionScoreboard().removeScoreboard(player);
                    player.sendMessage(Component.text("✓ Scoreboard skryt!").color(NamedTextColor.YELLOW));
                    return true;
                }
                case "show", "on" -> {
                    plugin.getProductionScoreboard().updatePlayerScoreboard(player);
                    player.sendMessage(Component.text("✓ Scoreboard zobrazen!").color(NamedTextColor.GREEN));
                    return true;
                }
                default -> {
                    sendHelp(player);
                    return true;
                }
            }
        }

        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== Production Scoreboard ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/scoreboard").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Aktualizovat scoreboard").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/scoreboard refresh").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Aktualizovat scoreboard").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/scoreboard hide").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Skrýt scoreboard").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/scoreboard show").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Zobrazit scoreboard").color(NamedTextColor.GRAY)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("refresh");
            completions.add("hide");
            completions.add("show");
            return completions;
        }
        return new ArrayList<>();
    }
}

