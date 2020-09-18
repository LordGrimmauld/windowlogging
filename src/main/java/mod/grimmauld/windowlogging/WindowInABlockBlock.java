package mod.grimmauld.windowlogging;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.extensions.IForgeBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class WindowInABlockBlock extends PaneBlock implements IForgeBlock {

	public WindowInABlockBlock() {
		super(Properties.create(Material.ROCK).notSolid());
	}

	private static void addBlockHitEffects(ParticleManager manager, BlockPos pos, BlockRayTraceResult target, BlockState blockstate, ClientWorld world) {
		Direction side = target.getFace();
		if (blockstate.getRenderType() != BlockRenderType.INVISIBLE) {
			int i = pos.getX();
			int j = pos.getY();
			int k = pos.getZ();
			AxisAlignedBB axisalignedbb = blockstate.getShape(world, pos).getBoundingBox();
			double d0 = (double) i + manager.rand.nextDouble() * (axisalignedbb.maxX - axisalignedbb.minX - (double) 0.2F) + (double) 0.1F + axisalignedbb.minX;
			double d1 = (double) j + manager.rand.nextDouble() * (axisalignedbb.maxY - axisalignedbb.minY - (double) 0.2F) + (double) 0.1F + axisalignedbb.minY;
			double d2 = (double) k + manager.rand.nextDouble() * (axisalignedbb.maxZ - axisalignedbb.minZ - (double) 0.2F) + (double) 0.1F + axisalignedbb.minZ;
			if (side == Direction.DOWN) {
				d1 = (double) j + axisalignedbb.minY - (double) 0.1F;
			}

			if (side == Direction.UP) {
				d1 = (double) j + axisalignedbb.maxY + (double) 0.1F;
			}

			if (side == Direction.NORTH) {
				d2 = (double) k + axisalignedbb.minZ - (double) 0.1F;
			}

			if (side == Direction.SOUTH) {
				d2 = (double) k + axisalignedbb.maxZ + (double) 0.1F;
			}

			if (side == Direction.WEST) {
				d0 = (double) i + axisalignedbb.minX - (double) 0.1F;
			}

			if (side == Direction.EAST) {
				d0 = (double) i + axisalignedbb.maxX + (double) 0.1F;
			}

			manager.addEffect((new DiggingParticle(world, d0, d1, d2, 0.0D, 0.0D, 0.0D, blockstate)).setBlockPos(pos).multiplyVelocity(0.2F).multiplyParticleScaleBy(0.6F));
		}
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new WindowInABlockTileEntity();
	}

	@Override
	public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player,
								   boolean willHarvest, FluidState fluid) {
		if (player == null)
			return super.removedByPlayer(state, world, pos, null, willHarvest, fluid);

		Vector3d start = player.getEyePosition(1);
		ModifiableAttributeInstance reachDistanceAttribute = player.getAttribute(ForgeMod.REACH_DISTANCE.get());
		if (reachDistanceAttribute == null)
			return super.removedByPlayer(state, world, pos, null, willHarvest, fluid);
		Vector3d end = start.add(player.getLookVec().scale(reachDistanceAttribute.getValue()));
		BlockRayTraceResult target =
			world.rayTraceBlocks(new RayTraceContext(start, end, BlockMode.OUTLINE, FluidMode.NONE, player));
		WindowInABlockTileEntity tileEntity = getTileEntity(world, pos);
		if (tileEntity == null)
			return super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
		BlockState windowBlock = tileEntity.getWindowBlock();
		for (AxisAlignedBB bb : windowBlock.getShape(world, pos).toBoundingBoxList()) {
			if (bb.grow(.1d).contains(target.getHitVec().subtract(pos.getX(), pos.getY(), pos.getZ()))) {
				windowBlock.getBlock().onBlockHarvested(world, pos, windowBlock, player);
				Block.spawnDrops(windowBlock, world, pos, null, player, player.getHeldItemMainhand());
				BlockState partialBlock = tileEntity.getPartialBlock();
				world.setBlockState(pos, partialBlock);
				for (Direction d : Direction.values()) {
					BlockPos offset = pos.offset(d);
					BlockState otherState = world.getBlockState(offset);
					partialBlock = partialBlock.updatePostPlacement(d, otherState, world, pos, offset);
					world.notifyBlockUpdate(offset, otherState, otherState, 2);
				}
				if (partialBlock != world.getBlockState(pos))
					world.setBlockState(pos, partialBlock);
				return false;
			}
		}

		return super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
	}

	@Override
	public boolean isReplaceable(BlockState state, BlockItemUseContext useContext) {
		return false;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
		return getSurroundingBlockState(reader, pos).propagatesSkylightDown(reader, pos);
	}

	@Override
	public boolean collisionExtendsVertically(BlockState state, IBlockReader world, BlockPos pos,
											  Entity collidingEntity) {
		return getSurroundingBlockState(world, pos).collisionExtendsVertically(world, pos, collidingEntity);
	}

	@Override
	public float getPlayerRelativeBlockHardness(BlockState state, PlayerEntity player, IBlockReader worldIn, BlockPos pos) {
		return getSurroundingBlockState(worldIn, pos).getPlayerRelativeBlockHardness(player, worldIn, pos);
	}

	@Override
	public float getExplosionResistance(BlockState state, IBlockReader world, BlockPos pos, Explosion explosion) {
		return getSurroundingBlockState(world, pos).getExplosionResistance(world, pos, explosion);
	}

	@Override
	public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos,
								  PlayerEntity player) {
		BlockState window = getWindowBlockState(world, pos);
		for (AxisAlignedBB bb : window.getShape(world, pos).toBoundingBoxList()) {
			if (bb.grow(.1d).contains(target.getHitVec().subtract(pos.getX(), pos.getY(), pos.getZ())))
				return window.getPickBlock(target, world, pos, player);
		}
		BlockState surrounding = getSurroundingBlockState(world, pos);
		return surrounding.getPickBlock(target, world, pos, player);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
		TileEntity tileentity = builder.get(LootParameters.BLOCK_ENTITY);
		if (!(tileentity instanceof WindowInABlockTileEntity))
			return Collections.emptyList();

		WindowInABlockTileEntity te = (WindowInABlockTileEntity) tileentity;
		List<ItemStack> drops = te.getPartialBlock().getDrops(builder);
		drops.addAll(te.getWindowBlock().getDrops(builder));
		return drops;
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		VoxelShape shape1 = getSurroundingBlockState(worldIn, pos).getShape(worldIn, pos, context);
		VoxelShape shape2 = getWindowBlockState(worldIn, pos).getShape(worldIn, pos, context);
		return VoxelShapes.or(shape1, shape2);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos,
										ISelectionContext context) {
		return getShape(state, worldIn, pos, context);
	}

	@Override
	public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn,
										  BlockPos currentPos, BlockPos facingPos) {
		WindowInABlockTileEntity te = getTileEntity(worldIn, currentPos);
		if (te == null)
			return stateIn;
		te.setWindowBlock(
			te.getWindowBlock().updatePostPlacement(facing, facingState, worldIn, currentPos, facingPos));
		BlockState blockState =
			te.getPartialBlock().updatePostPlacement(facing, facingState, worldIn, currentPos, facingPos);
		if (blockState.getBlock() instanceof FourWayBlock) {
			for (BooleanProperty side : Arrays.asList(FourWayBlock.EAST, FourWayBlock.NORTH, FourWayBlock.SOUTH,
				FourWayBlock.WEST))
				blockState = blockState.with(side, false);
			te.setPartialBlock(blockState);
		}
		te.requestModelDataUpdate();

		return stateIn;
	}

	private BlockState getSurroundingBlockState(IBlockReader reader, BlockPos pos) {
		WindowInABlockTileEntity te = getTileEntity(reader, pos);
		if (te != null)
			return te.getPartialBlock();
		return Blocks.AIR.getDefaultState();
	}

	private BlockState getWindowBlockState(IBlockReader reader, BlockPos pos) {
		WindowInABlockTileEntity te = getTileEntity(reader, pos);
		if (te != null)
			return te.getWindowBlock();
		return Blocks.AIR.getDefaultState();
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
		return false;
	}

	@Nullable
	private WindowInABlockTileEntity getTileEntity(IBlockReader world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof WindowInABlockTileEntity)
			return (WindowInABlockTileEntity) te;
		return null;
	}

	@Override
	public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		return super.getSoundType(te != null ? te.getPartialBlock() : state, world, pos, entity);
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerWorld world, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null) {
			te.getWindowBlock().addLandingEffects(world, pos, state2, entity, numberOfParticles / 2);
			return te.getPartialBlock().addLandingEffects(world, pos, state2, entity, numberOfParticles / 2);
		}
		return false;
	}

	@Override
	public boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null) {
			te.getWindowBlock().addRunningEffects(world, pos, entity);
			return te.getPartialBlock().addRunningEffects(world, pos, entity);
		}
		return false;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean addDestroyEffects(BlockState state, World world, BlockPos pos, ParticleManager manager) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null) {
			te.getWindowBlock().addDestroyEffects(world, pos, manager);
			manager.addBlockDestroyEffects(pos, te.getWindowBlock());
			return te.getPartialBlock().addDestroyEffects(world, pos, manager);
		}
		return false;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean addHitEffects(BlockState state, World world, RayTraceResult target, ParticleManager manager) {
		if (target.getType() != RayTraceResult.Type.BLOCK || !(target instanceof BlockRayTraceResult))
			return false;
		BlockPos pos = ((BlockRayTraceResult) target).getPos();
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null && world instanceof ClientWorld) {
			te.getWindowBlock().addHitEffects(world, target, manager);
			addBlockHitEffects(manager, pos, (BlockRayTraceResult) target, te.getWindowBlock(), (ClientWorld) world);
			return te.getPartialBlock().addHitEffects(world, target, manager);
		}
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	public IBakedModel createModel(IBakedModel original) {
		return new WindowInABlockModel(original);
	}
}
