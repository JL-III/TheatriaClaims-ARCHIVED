package com.jliii.theatriaclaims.visualization.impl;

import com.jliii.theatriaclaims.config.ConfigManager;
import com.jliii.theatriaclaims.util.IntVector;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * A {@link FakeBlockVisualization} with maximum anti-cheat compatibility.
 */
public class AntiCheatCompatVisualization extends FakeBlockVisualization {
    /**
     * Construct a new {@code AntiCheatCompatVisualization}.
     *
     * @param world the {@link World} being visualized in
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     */
    public AntiCheatCompatVisualization(@NotNull World world, @NotNull IntVector visualizeFrom, int height, ConfigManager configManager) {
        super(world, visualizeFrom, height, configManager);
    }

    @Override
    protected boolean isTransparent(@NotNull Block block) {
        Collection<BoundingBox> boundingBoxes = block.getCollisionShape().getBoundingBoxes();
        // Decide transparency based on whether block physical bounding box occupies the entire block volume.
        return boundingBoxes.isEmpty() || !boundingBoxes.stream().allMatch(box -> box.getVolume() == 1.0);
    }

}
