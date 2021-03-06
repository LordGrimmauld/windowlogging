package mod.grimmauld.windowlogging;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static mod.grimmauld.windowlogging.WindowInABlockTileEntity.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WindowInABlockModel extends BakedModelWrapper<IBakedModel> {
	public WindowInABlockModel(IBakedModel original) {
		super(original);
	}

	private static void fightZfighting(BakedQuad q) {
		int[] data = q.getVertexData();
		Vec3i vec = q.getFace().getDirectionVec();
		int dirX = vec.getX();
		int dirY = vec.getY();
		int dirZ = vec.getZ();

		for (int i = 0; i < 4; ++i) {
			int j = data.length / 4 * i;
			float x = Float.intBitsToFloat(data[j]);
			float y = Float.intBitsToFloat(data[j + 1]);
			float z = Float.intBitsToFloat(data[j + 2]);
			double offset = q.getFace().getAxis().getCoordinate(x, y, z);

			if (offset < 1 / 1024d || offset > 1023 / 1024d) {
				data[j] = Float.floatToIntBits(x - 1 / 512f * dirX);
				data[j + 1] = Float.floatToIntBits(y - 1 / 512f * dirY);
				data[j + 2] = Float.floatToIntBits(z - 1 / 512f * dirZ);
			}
		}
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData data) {
		BlockRendererDispatcher dispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
		BlockState partialState = data.getData(PARTIAL_BLOCK);
		BlockState windowState = data.getData(WINDOW_BLOCK);
		BlockPos position = data.getData(POSITION);
		TileEntity partialTE = data.getData(PARTIAL_TE);
		ClientWorld world = Minecraft.getInstance().world;
		List<BakedQuad> quads = new ArrayList<>();
		if (world == null || position == null)
			return quads;

		if (partialState == null || windowState == null)
			return dispatcher.getModelForState(Blocks.DIRT.getDefaultState()).getQuads(state, side, rand, data);

		RenderType renderType = MinecraftForgeClient.getRenderLayer();
		if (RenderTypeLookup.canRenderInLayer(partialState, renderType) && partialState.getRenderType() == BlockRenderType.MODEL) {
			IBakedModel partialModel = dispatcher.getModelForState(partialState);
			IModelData modelData = partialModel.getModelData(world, position, partialState,
				partialTE == null ? EmptyModelData.INSTANCE : partialTE.getModelData());
			quads.addAll(partialModel.getQuads(partialState, side, rand, modelData));
		}
		if (RenderTypeLookup.canRenderInLayer(windowState, renderType)) {
			IBakedModel windowModel = dispatcher.getModelForState(windowState);
			IModelData glassModelData = windowModel.getModelData(world, position, windowState, EmptyModelData.INSTANCE);
			dispatcher.getModelForState(windowState).getQuads(windowState, side, rand, glassModelData)
				.forEach(bakedQuad -> {
					if (!Block.hasSolidSide(partialState, world, position, bakedQuad.getFace())) {
						if (!(windowState.getBlock().equals(Blocks.IRON_BARS) && bakedQuad.getFace().getAxis().isVertical()))
							fightZfighting(bakedQuad);
						quads.add(bakedQuad);
					}
				});
		}
		return quads;
	}

	@Override
	public TextureAtlasSprite getParticleTexture(IModelData data) {
		BlockRendererDispatcher dispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
		BlockState partialState = data.getData(PARTIAL_BLOCK);
		TileEntity partialTE = data.getData(PARTIAL_TE);
		if (partialState == null)
			return super.getParticleTexture(data);
		return dispatcher.getModelForState(partialState).getParticleTexture(partialTE == null ? data : partialTE.getModelData());
	}

	@Override
	public boolean isAmbientOcclusion() {
		RenderType renderLayer = MinecraftForgeClient.getRenderLayer();
		return renderLayer == RenderType.getSolid();
	}
}
