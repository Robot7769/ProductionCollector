package me.robot7769.productionCollector.Listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Duration;

public class WarningTitle implements Title {
    @Override
    public @NotNull Component title() {
        return Component.text("Barrel nepatří tobě! Nedělej to!", TextColor.color(255, 14, 0));
    }

    @Override
    public @NotNull Component subtitle() {
        return Component.text("Přístě to může bolet víc!", TextColor.color(255, 75, 27));
    }

    @Override
    public @Nullable Times times() {
        return new Times() {
            @Override
            public @NotNull Duration fadeIn() {
                return Duration.ofSeconds(1);
            }

            @Override
            public @NotNull Duration stay() {
                return Duration.ofSeconds(10);
            }

            @Override
            public @NotNull Duration fadeOut() {
                return Duration.ofSeconds(1);
            }
        };
    }

    @Override
    public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
        return null;
    }
}
