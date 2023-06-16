package zetabite.desertship;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
TODO:
- Carpet as Saddle -> Visuals
- Chest for transport -> Visuals
- Connecting one camel to the other with leash -> do for all horses?
- If chest, then the jump ability is reduced
 */

public class DesertShip implements ModInitializer {
	public static final String MODID = "desertship";
	public static final String MODNAME = "Desert Ship";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("Hello Quilt world from {}!", mod.metadata().name());
	}
}
