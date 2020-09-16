package mod.grimmauld.windowlogging;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WindowInABlockTileEntity extends TileEntity {

	private BlockState partialBlock = Blocks.AIR.getDefaultState();
	private BlockState windowBlock = Blocks.AIR.getDefaultState();

	@OnlyIn(value = Dist.CLIENT)

	public WindowInABlockTileEntity() {
		super(RegistryEntries.WINDOW_IN_A_BLOCK_TILE_ENTITY);
	}

	@Override
	public void read(CompoundNBT compound) {
		partialBlock = NBTUtil.readBlockState(compound.getCompound("PartialBlock"));
		windowBlock = NBTUtil.readBlockState(compound.getCompound("WindowBlock"));
		super.read(compound);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		compound.put("PartialBlock", NBTUtil.writeBlockState(getPartialBlock()));
		compound.put("WindowBlock", NBTUtil.writeBlockState(getWindowBlock()));
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
	public void handleUpdateTag(CompoundNBT tag) {
		read(tag);
	}

	@Override
	public CompoundNBT getUpdateTag() {
		return write(new CompoundNBT());
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(getPos(), 1, write(new CompoundNBT()));
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		read(pkt.getNbtCompound());
	}
}
