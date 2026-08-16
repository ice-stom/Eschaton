package io.gitlab.icestom.eschaton.server.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.block.Block;

public class LightningRod extends Entity {
    public LightningRod() {
        super(EntityType.BLOCK_DISPLAY);

        ((BlockDisplayMeta) this.getEntityMeta()).setBlockState(Block.LIGHTNING_ROD);
        ((BlockDisplayMeta) this.getEntityMeta()).setTranslation(new Pos(-0.5, 0, -0.5));
    }
}
