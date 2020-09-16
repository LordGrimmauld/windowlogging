package mod.grimmauld.windowlogging;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;

@SuppressWarnings("deprecation")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WrappedBakedModel implements IBakedModel {

	protected IBakedModel template;

	public WrappedBakedModel(IBakedModel template) {
		this.template = template;
	}

	@Override
	public IBakedModel getBakedModel() {
		return template;
	}

	@Override
	public boolean isAmbientOcclusion() {
		return template.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return template.isGui3d();
	}

	@Override
	public boolean func_230044_c_() {
		return template.func_230044_c_();
	}

	@Override
	public boolean isBuiltInRenderer() {
		return template.isBuiltInRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleTexture(IModelData data) {
		return template.getParticleTexture(data);
	}

	@Override
	public ItemOverrideList getOverrides() {
		return template.getOverrides();
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
		return getQuads(state, side, rand, EmptyModelData.INSTANCE);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData data) {
		return template.getQuads(state, side, rand, data);
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return getParticleTexture(EmptyModelData.INSTANCE);
	}

}
