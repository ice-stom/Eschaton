package io.gitlab.icestom.eschaton.server.entity;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.block.Block;

public class BoatMarker extends Entity {
    public BoatMarker() {
        super(EntityType.BLOCK_DISPLAY);

        BlockDisplayMeta meta = (BlockDisplayMeta) getEntityMeta();

        meta.setBlockState(Block.RED_STAINED_GLASS);
        meta.setScale(new Vec(1.375, 0, 1.375));
        meta.setTranslation(new Vec(-1.375 / 2, 0.01, -1.375 / 2));
        meta.setBrightness(15, 15);
    }
}
