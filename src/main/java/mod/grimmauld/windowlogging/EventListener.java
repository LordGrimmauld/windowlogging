package mod.grimmauld.windowlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FourWayBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.SlabType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public class EventListener {
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

	public static void clientInit(FMLClientSetupEvent event) {
		registerRenderers();
	}

	@OnlyIn(Dist.CLIENT)
	public static void registerRenderers() {
		RenderTypeLookup.setRenderLayer(RegistryEntries.WINDOW_IN_A_BLOCK, renderType -> true);
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
		if (blockState.has(BlockStateProperties.SLAB_TYPE) && blockState.get(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE)
			return;

		BlockState defaultState = RegistryEntries.WINDOW_IN_A_BLOCK.getDefaultState();
		world.setBlockState(pos, defaultState);
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof WindowInABlockTileEntity) {
			WindowInABlockTileEntity wte = (WindowInABlockTileEntity) te;
			wte.setWindowBlock(item.getBlock().getDefaultState());
			wte.updateWindowConnections();

			if (blockState.getBlock() instanceof FourWayBlock) {
				for (BooleanProperty side : Arrays.asList(FourWayBlock.EAST, FourWayBlock.NORTH, FourWayBlock.SOUTH,
					FourWayBlock.WEST))
					blockState = blockState.with(side, false);
			}
			if (blockState.getBlock() instanceof WallBlock)
				blockState = blockState.with(WallBlock.UP, true);

			wte.setPartialBlock(blockState);
			wte.requestModelDataUpdate();

			if (!event.getPlayer().isCreative())
				stack.shrink(1);
			event.getPlayer().swingArm(event.getHand());
		}

		event.setCanceled(true);
	}

	@OnlyIn(Dist.CLIENT)
	public static void onModelBake(ModelBakeEvent event) {
		Map<ResourceLocation, IBakedModel> modelRegistry = event.getModelRegistry();
		swapModels(modelRegistry, getAllBlockStateModelLocations(RegistryEntries.WINDOW_IN_A_BLOCK), RegistryEntries.WINDOW_IN_A_BLOCK::createModel);

	}

	@OnlyIn(Dist.CLIENT)
	protected static List<ModelResourceLocation> getAllBlockStateModelLocations(Block block) {
		List<ModelResourceLocation> models = new ArrayList<>();
		block.getStateContainer().getValidStates().forEach(state -> models.add(getBlockModelLocation(block, BlockModelShapes.getPropertyMapString(state.getValues()))));
		return models;
	}

	@OnlyIn(Dist.CLIENT)
	protected static ModelResourceLocation getBlockModelLocation(Block block, String suffix) {
		return new ModelResourceLocation(block.getRegistryName(), suffix);
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
}
