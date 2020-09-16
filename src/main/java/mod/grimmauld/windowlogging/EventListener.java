package mod.grimmauld.windowlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FourWayBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.SlabType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Arrays;

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
		ClientRegistry.bindTileEntityRenderer(RegistryEntries.WINDOW_IN_A_BLOCK_TILE_ENTITY, WindowloggedTileEntityRenderer::new);
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
}
