package mod.grimmauld.windowlogging;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WindowInABlockTileEntity extends TileEntity {

	public static final ModelProperty<BlockState> PARTIAL_BLOCK = new ModelProperty<>();
	public static final ModelProperty<BlockState> WINDOW_BLOCK = new ModelProperty<>();
	public static final ModelProperty<BlockPos> POSITION = new ModelProperty<>();
	public static final ModelProperty<TileEntity> PARTIAL_TE = new ModelProperty<>();
	@OnlyIn(Dist.CLIENT)
	private static final Minecraft MC = Minecraft.getInstance();
	private BlockState partialBlock = Blocks.AIR.getDefaultState();
	private BlockState windowBlock = Blocks.AIR.getDefaultState();
	private CompoundNBT partialBlockTileData;
	private TileEntity partialBlockTileEntity = null;
	@OnlyIn(Dist.CLIENT)
	private IModelData modelData;

	public WindowInABlockTileEntity() {
		super(RegistryEntries.WINDOW_IN_A_BLOCK_TILE_ENTITY);
		setPartialBlockTileData(new CompoundNBT());
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::initDataMap);
	}

	public CompoundNBT getPartialBlockTileData() {
		return partialBlockTileData;
	}

	public void setPartialBlockTileData(CompoundNBT partialBlockTileData) {
		this.partialBlockTileData = partialBlockTileData;
	}

	@OnlyIn(value = Dist.CLIENT)
	private void initDataMap() {
		modelData = new ModelDataMap.Builder().withInitial(WINDOW_BLOCK, Blocks.AIR.getDefaultState())
			.withInitial(PARTIAL_BLOCK, Blocks.AIR.getDefaultState()).withInitial(POSITION, BlockPos.ZERO).withInitial(PARTIAL_TE, null).build();
	}

	@Override
	public void read(CompoundNBT compound) {
		partialBlock = NBTUtil.readBlockState(compound.getCompound("PartialBlock"));
		windowBlock = NBTUtil.readBlockState(compound.getCompound("WindowBlock"));
		setPartialBlockTileData(compound.getCompound("PartialData"));
		super.read(compound);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		compound.put("PartialBlock", NBTUtil.writeBlockState(getPartialBlock()));
		compound.put("WindowBlock", NBTUtil.writeBlockState(getWindowBlock()));
		compound.put("PartialData", partialBlockTileData);
		return super.write(compound);
	}

	public void updateWindowConnections() {
		if (world == null)
			return;
		for (Direction side : Direction.values()) {
			BlockPos offsetPos = pos.offset(side);
			windowBlock = getWindowBlock().updatePostPlacement(side, world.getBlockState(offsetPos), world, pos,
				offsetPos);
		}
		world.notifyBlockUpdate(getPos(), getBlockState(), getBlockState(), 2 | 16);
		markDirty();
	}

	@OnlyIn(value = Dist.CLIENT)
	@Override
	public IModelData getModelData() {
		modelData.setData(PARTIAL_BLOCK, partialBlock);
		modelData.setData(WINDOW_BLOCK, windowBlock);
		modelData.setData(POSITION, pos);
		modelData.setData(PARTIAL_TE, getPartialBlockTileEntityIfPresent());
		return modelData;
	}

	public BlockState getPartialBlock() {
		return partialBlock;
	}

	public void setPartialBlock(BlockState partialBlock) {
		this.partialBlock = partialBlock;
	}

	public BlockState getWindowBlock() {
		return windowBlock;
	}

	public void setWindowBlock(BlockState windowBlock) {
		this.windowBlock = windowBlock;
	}

	@Override
	public CompoundNBT getUpdateTag() {
		return write(new CompoundNBT());
	}

	@Override
	public void handleUpdateTag(CompoundNBT tag) {
		read(tag);
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(getPos(), 1, write(new CompoundNBT()));
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		read(pkt.getNbtCompound());
	}

	@Nullable
	public TileEntity getPartialBlockTileEntityIfPresent() {
		if (!getPartialBlock().hasTileEntity() || world == null)
			return null;
		if (partialBlockTileEntity == null) {
			try {
				partialBlockTileEntity = getPartialBlock().createTileEntity(world);
				if (partialBlockTileEntity != null) {
					partialBlockTileEntity.cachedBlockState = getPartialBlock();
					partialBlockTileEntity.deserializeNBT(partialBlockTileData);
					partialBlockTileEntity.setWorldAndPos(world, pos);
				}
			} catch (Exception e) {
				partialBlockTileEntity = null;
			}
		}
		return partialBlockTileEntity;
	}

	@Override
	public boolean canRenderBreaking() {
		return true;
	}

	@Override
	public void requestModelDataUpdate() {
		try {
			super.requestModelDataUpdate();
		} catch (IllegalArgumentException e) {
			if (!FMLEnvironment.dist.isClient())
				return;
			World world = this.world;
			try {
				this.world = MC.world;
				super.requestModelDataUpdate();
			} finally {
				this.world = world;
			}
		}
	}
}
