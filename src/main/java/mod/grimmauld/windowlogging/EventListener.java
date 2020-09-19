package mod.grimmauld.windowlogging;

import net.minecraft.block.*;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.SlabType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public class EventListener {
    public static void clientInit(FMLClientSetupEvent event) {
        registerRenderers();
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerRenderers() {
        RenderTypeLookup.setRenderLayer(RegistryEntries.WINDOW_IN_A_BLOCK, renderType -> true);
        ClientRegistry.bindTileEntityRenderer(RegistryEntries.WINDOW_IN_A_BLOCK_TILE_ENTITY, WindowInABlockTileEntityRenderer::new);
    }

    @OnlyIn(Dist.CLIENT)
    public static void onModelBake(ModelBakeEvent event) {
        Map<ResourceLocation, IBakedModel> modelRegistry = event.getModelRegistry();
        swapModels(modelRegistry, getAllBlockStateModelLocations(RegistryEntries.WINDOW_IN_A_BLOCK), RegistryEntries.WINDOW_IN_A_BLOCK::createModel);

    }

    @OnlyIn(Dist.CLIENT)
    protected static List<ModelResourceLocation> getAllBlockStateModelLocations(Block block) {
        List<ModelResourceLocation> models = new ArrayList<>();
        block.getStateContainer().getValidStates().forEach(state -> {
            ModelResourceLocation rl = getBlockModelLocation(block, BlockModelShapes.getPropertyMapString(state.getValues()));
            if (rl != null)
                models.add(rl);
        });
        return models;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected static ModelResourceLocation getBlockModelLocation(Block block, String suffix) {
        ResourceLocation rl = block.getRegistryName();
        if (rl == null)
            return null;
        return new ModelResourceLocation(rl, suffix);
    }

    @OnlyIn(Dist.CLIENT)
    protected static <T extends IBakedModel> void swapModels(Map<ResourceLocation, IBakedModel> modelRegistry,
                                                             ModelResourceLocation location, Function<IBakedModel, T> factory) {
        modelRegistry.put(location, factory.apply(modelRegistry.get(location)));
    }

    @OnlyIn(Dist.CLIENT)
    protected static <T extends IBakedModel> void swapModels(Map<ResourceLocation, IBakedModel> modelRegistry,
                                                             List<ModelResourceLocation> locations, Function<IBakedModel, T> factory) {
        locations.forEach(location -> swapModels(modelRegistry, location, factory));
    }

    @SubscribeEvent
    public void rightClickPartialBlockWithPaneMakesItWindowLogged(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem() == Event.Result.DENY)
            return;
        if (event.getEntityLiving().isSneaking())
            return;
        if (!event.getPlayer().isAllowEdit())
            return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty())
            return;
        if (!(stack.getItem() instanceof BlockItem))
            return;
        BlockItem item = (BlockItem) stack.getItem();
        if (!item.isIn(Tags.Items.GLASS_PANES)) {
            item.getBlock();
            if (!item.getBlock().isIn(Tags.Blocks.GLASS_PANES)) return;
        }

        BlockPos pos = event.getPos();
        World world = event.getWorld();
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.getBlock().getTags().contains(Windowlogging.WindowableBlockTagLocation))
            return;
        if (blockState.getBlock() instanceof WindowInABlockBlock)
            return;
        if (blockState.func_235903_d_(BlockStateProperties.SLAB_TYPE).orElse(null) == SlabType.DOUBLE)
            return;

        BlockState defaultState = RegistryEntries.WINDOW_IN_A_BLOCK.getDefaultState();
        CompoundNBT partialBlockTileData = new CompoundNBT();
        TileEntity currentTE = world.getTileEntity(pos);
        if (currentTE != null)
            partialBlockTileData = currentTE.serializeNBT();
        world.setBlockState(pos, defaultState);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof WindowInABlockTileEntity) {
            WindowInABlockTileEntity wte = (WindowInABlockTileEntity) te;
            wte.setWindowBlock(item.getBlock().getDefaultState());
            wte.updateWindowConnections();
            SoundType soundtype = wte.getWindowBlock().getSoundType(world, pos, event.getPlayer());
            world.playSound(null, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

            if (blockState.getBlock() instanceof FourWayBlock) {
                for (BooleanProperty side : Arrays.asList(FourWayBlock.EAST, FourWayBlock.NORTH, FourWayBlock.SOUTH,
                        FourWayBlock.WEST))
                    blockState = blockState.with(side, false);
            }
            if (blockState.getBlock() instanceof WallBlock)
                blockState = blockState.with(WallBlock.UP, true);

            wte.setPartialBlock(blockState);
            wte.setPartialBlockTileData(partialBlockTileData);
            wte.requestModelDataUpdate();

            if (!event.getPlayer().isCreative())
                stack.shrink(1);
            event.getPlayer().swingArm(event.getHand());
        }

        event.setCanceled(true);
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void registerBlocks(final RegistryEvent.Register<Block> event) {
            Windowlogging.LOGGER.debug("blocks registering");
            event.getRegistry().register(new WindowInABlockBlock().setRegistryName("window_in_a_block"));
        }

        @SubscribeEvent
        public static void registerTEs(final RegistryEvent.Register<TileEntityType<?>> event) {
            Windowlogging.LOGGER.debug("TEs registering");
            event.getRegistry().register(TileEntityType.Builder.create(WindowInABlockTileEntity::new, RegistryEntries.WINDOW_IN_A_BLOCK).build(null).setRegistryName("window_in_a_block"));
        }
    }
}
