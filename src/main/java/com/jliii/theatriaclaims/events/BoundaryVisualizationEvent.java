package com.jliii.theatriaclaims.events;

import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.util.IntVector;
import com.jliii.theatriaclaims.visualization.Boundary;
import com.jliii.theatriaclaims.visualization.VisualizationProvider;
import com.jliii.theatriaclaims.visualization.impl.AntiCheatCompatVisualization;
import com.jliii.theatriaclaims.visualization.impl.FakeBlockVisualization;
import com.jliii.theatriaclaims.visualization.BoundaryVisualization;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * An {@link org.bukkit.event.Event Event} called when a {@link Player} receives {@link Boundary} visuals.
 */
public class BoundaryVisualizationEvent extends PlayerEvent {

    private final @NotNull Collection<Boundary> boundaries;
    private final int height;
    private @NotNull VisualizationProvider provider;

    /**
     * Construct a new {@code BoundaryVisualizationEvent} for a group of {@link Boundary Boundaries} using the default
     * visualization provider.
     *
     * @param player the {@link Player} receiving visuals
     * @param boundaries the {@code Boundaries} to visualize
     * @param height the height at which the visualization was initiated
     */
    public BoundaryVisualizationEvent(@NotNull Player player, @NotNull Collection<Boundary> boundaries, int height, ConfigManager configManager) {
        this(player, boundaries, height, DEFAULT_PROVIDER, configManager);
    }

    /**
     *
     * Construct a new {@code BoundaryVisualizationEvent} for a group of {@link Boundary Boundaries}.
     *
     * @param player the {@link Player} receiving visuals
     * @param boundaries the {@code Boundaries} to visualize
     * @param height the height at which the visualization was initiated
     * @param provider the {@link VisualizationProvider}
     */
    public BoundaryVisualizationEvent(@NotNull Player player, @NotNull Collection<Boundary> boundaries, int height, @NotNull VisualizationProvider provider, ConfigManager configManager) {
        super(player);
        this.boundaries = new HashSet<>(boundaries);
        this.height = height;
        this.provider = provider;
    }

    public static final VisualizationProvider DEFAULT_PROVIDER = (world, visualizeFrom, height, configManager) -> {
        if (configManager.getSystemConfig().visualizationAntiCheatCompat) {
            return new AntiCheatCompatVisualization(world, visualizeFrom, height, configManager);
        }
        return new FakeBlockVisualization(world, visualizeFrom, height, configManager);
    };

    /**
     * Get the {@link Boundary Boundaries} to visualize.
     * The collection is mutable, addons may add or remove elements as they see fit.
     *
     * @return the {@code Boundaries} to visualize
     */
    public @NotNull Collection<Boundary> getBoundaries() {
        return boundaries;
    }

    /**
     * Get the center of the visualization area.
     *
     * @return the coordinates of the center of the visualization
     */
    public @NotNull IntVector getCenter() {
        return new IntVector(player.getLocation());
    }

    /**
     * Get the height at which the visualization was initiated.
     *
     * @return the height at which the visualization was initiated
     */
    public int getHeight() {
        return height;
    }

    /**
     * Get the {@link VisualizationProvider} used to create the {@link BoundaryVisualization} after event completion.
     *
     * @return the {@code VisualizationProvider}
     */
    public @NotNull VisualizationProvider getProvider() {
        return provider;
    }

    /**
     * Set the {@link VisualizationProvider} used to create the {@link BoundaryVisualization} after event completion.
     *
     * @param provider the {@code VisualizationProvider}
     */
    public void setProvider(@NotNull VisualizationProvider provider) {
        this.provider = provider;
    }

    // Listenable event requirements
    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

}
