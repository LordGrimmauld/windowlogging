package mod.grimmauld.windowlogging;

import com.mojang.blaze3d.matrix.MatrixStack;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WindowInABlockTileEntityRenderer extends TileEntityRenderer<WindowInABlockTileEntity> {
	public WindowInABlockTileEntityRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
		super(rendererDispatcherIn);
	}

	@Override
	public void render(WindowInABlockTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
		TileEntity partialTE = tileEntityIn.getPartialBlockTileEntityIfPresent();
		if (partialTE == null)
			return;
		TileEntityRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(partialTE);
		if (renderer == null)
			return;
		try {
			renderer.render(partialTE, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
		} catch (Exception ignored) {
		}
	}
}
