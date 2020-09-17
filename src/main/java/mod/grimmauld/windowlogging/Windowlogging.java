package mod.grimmauld.windowlogging;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Windowlogging.MODID)
public class Windowlogging {
	public static final String MODID = "windowlogging";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	public static final ResourceLocation WindowableBlockTagLocation = new ResourceLocation(MODID, "windowable");

	public Windowlogging() {
		MinecraftForge.EVENT_BUS.register(new EventListener());
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			FMLJavaModLoadingContext.get().getModEventBus().addListener(EventListener::clientInit);
			FMLJavaModLoadingContext.get().getModEventBus().addListener(EventListener::onModelBake);
		});
	}
}
