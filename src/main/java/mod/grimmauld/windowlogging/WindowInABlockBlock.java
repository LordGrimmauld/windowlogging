package mod.grimmauld.windowlogging;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext.Builder;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class WindowInABlockBlock extends PaneBlock {

	public WindowInABlockBlock() {
		super(Properties.create(Material.ROCK).notSolid());
	}

	private static void addBlockHitEffects(ParticleManager manager, BlockPos pos, BlockRayTraceResult target, BlockState blockstate, World world) {
		VoxelShape shape = blockstate.getShape(world, pos);
		if (shape.isEmpty())
			return;
		Direction side = target.getFace();
		int i = pos.getX();
		int j = pos.getY();
		int k = pos.getZ();
		AxisAlignedBB axisalignedbb = shape.getBoundingBox();
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

		manager.addEffect((new DiggingParticle(world, d0, d1, d2, 0.0D, 0.0D, 0.0D, blockstate)).setBlockPos(pos).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
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
								   boolean willHarvest, IFluidState fluid) {
		if (player == null)
			return super.removedByPlayer(state, world, pos, null, willHarvest, fluid);

		Vec3d start = player.getEyePosition(1);
		Vec3d end = start.add(player.getLookVec().scale(player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue()));
		BlockRayTraceResult target =
			world.rayTraceBlocks(new RayTraceContext(start, end, BlockMode.OUTLINE, FluidMode.NONE, player));
		WindowInABlockTileEntity tileEntity = getTileEntity(world, pos);
		if (tileEntity == null)
			return super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
		BlockState windowBlock = tileEntity.getWindowBlock();
		CompoundNBT partialBlockTileData = tileEntity.getPartialBlockTileData();
		for (AxisAlignedBB bb : windowBlock.getShape(world, pos).toBoundingBoxList()) {
			if (bb.grow(.1d).contains(target.getHitVec().subtract(new Vec3d(pos)))) {
				windowBlock.getBlock().onBlockHarvested(world, pos, windowBlock, player);
				if (!player.isCreative())
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
				if (world.getBlockState(pos).hasTileEntity()) {
					TileEntity te = world.getTileEntity(pos);
					if (te != null) {
						te.deserializeNBT(partialBlockTileData);
						te.markDirty();
					}
				}
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
	public float getBlockHardness(BlockState blockState, IBlockReader worldIn, BlockPos pos) {
		return getSurroundingBlockState(worldIn, pos).getBlockHardness(worldIn, pos);
	}

	@Override
	public float getExplosionResistance(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder,
										Explosion explosion) {
		return getSurroundingBlockState(world, pos).getExplosionResistance(world, pos, exploder, explosion);
	}

	@Override
	public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos,
								  PlayerEntity player) {
		BlockState window = getWindowBlockState(world, pos);
		for (AxisAlignedBB bb : window.getShape(world, pos).toBoundingBoxList()) {
			if (bb.grow(.1d).contains(target.getHitVec().subtract(new Vec3d(pos))))
				return window.getPickBlock(target, world, pos, player);
		}
		BlockState surrounding = getSurroundingBlockState(world, pos);
		return surrounding.getPickBlock(target, world, pos, player);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, Builder builder) {
		TileEntity tileentity = builder.get(LootParameters.BLOCK_ENTITY);
		if (!(tileentity instanceof WindowInABlockTileEntity))
			return Collections.emptyList();

		WindowInABlockTileEntity te = (WindowInABlockTileEntity) tileentity;
		TileEntity partialTE = te.getPartialBlockTileEntityIfPresent();
		if (partialTE != null)
			builder.withParameter(LootParameters.BLOCK_ENTITY, partialTE);
		List<ItemStack> drops = te.getPartialBlock().getDrops(builder);
		builder.withParameter(LootParameters.BLOCK_ENTITY, tileentity);
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
	public MaterialColor getMaterialColor(BlockState state, IBlockReader worldIn, BlockPos pos) {
		return getSurroundingBlockState(worldIn, pos).getMaterialColor(worldIn, pos);
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
		if (te != null) {
			te.getWindowBlock().addHitEffects(world, target, manager);
			addBlockHitEffects(manager, pos, (BlockRayTraceResult) target, te.getWindowBlock(), world);
			return te.getPartialBlock().addHitEffects(world, target, manager);
		}
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	public IBakedModel createModel(IBakedModel original) {
		return new WindowInABlockModel(original);
	}

	@Override
	public boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos) {
		return false;
	}

	@Override
	public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null) {
			BlockState partialState = te.getPartialBlock();
			partialState.getBlock().getLightValue(partialState, world, pos);
		}
		return 0;
	}

	@Override
	public float getPlayerRelativeBlockHardness(BlockState state, PlayerEntity player, IBlockReader worldIn, BlockPos pos) {
		return getSurroundingBlockState(worldIn, pos).getPlayerRelativeBlockHardness(player, worldIn, pos);
	}
}
