package mod.grimmauld.windowlogging;

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;

public class RegistryEntries {
    @ObjectHolder(Windowlogging.MODID + ":window_in_a_block")
    public static WindowInABlockBlock WINDOW_IN_A_BLOCK;

    @ObjectHolder(Windowlogging.MODID + ":window_in_a_block")
    public static TileEntityType<WindowInABlockTileEntity> WINDOW_IN_A_BLOCK_TILE_ENTITY;

    private RegistryEntries() {
    }
}
