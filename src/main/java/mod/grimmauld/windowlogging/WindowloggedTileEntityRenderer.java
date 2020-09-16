package mod.grimmauld.windowlogging;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

@ParametersAreNonnullByDefault
public class WindowloggedTileEntityRenderer extends TileEntityRenderer<WindowInABlockTileEntity> {
	private static final Minecraft MC = Minecraft.getInstance();

	public WindowloggedTileEntityRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
		super(rendererDispatcherIn);
	}

	@Override
	public void render(WindowInABlockTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
		BlockRendererDispatcher dispatcher = MC.getBlockRendererDispatcher();
		dispatcher.renderBlock(tileEntityIn.getPartialBlock(), matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, tileEntityIn.getModelData());

		BlockState state = tileEntityIn.getWindowBlock();
		IBakedModel modelIn = dispatcher.getModelForState(state);
		MatrixStack.Entry matrixEntry = matrixStackIn.getLast();
		IModelData modelData = tileEntityIn.getModelData();
		IVertexBuilder buffer = bufferIn.getBuffer(RenderTypeLookup.getRenderType(state));

		Random random = new Random();
		for (Direction direction : Direction.values()) {
			random.setSeed(42L);
			renderModelBrightnessColorQuads(matrixEntry, buffer, modelIn.getQuads(state, direction, random, modelData), combinedLightIn, combinedOverlayIn);
		}
		random.setSeed(42L);
		renderModelBrightnessColorQuads(matrixEntry, buffer, modelIn.getQuads(state, null, random, modelData), combinedLightIn, combinedOverlayIn);
	}

	private static void renderModelBrightnessColorQuads(MatrixStack.Entry matrixEntry, IVertexBuilder buffer, List<BakedQuad> listQuads, int combinedLightIn, int combinedOverlayIn) {
		for (BakedQuad bakedquad : listQuads) {
			fightZfighting(bakedquad);
			buffer.addQuad(matrixEntry, bakedquad, 1, 1, 1, combinedLightIn, combinedOverlayIn);
		}

	}

	// thanks simi
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
}
