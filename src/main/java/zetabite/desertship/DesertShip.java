package zetabite.desertship;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zetabite.desertship.networking.packet.ChestedCamelS2CPacket;
import zetabite.desertship.util.CaravanUtil;

/*
TODO:
- Carpet as Saddle -> Visuals
- Chest for transport -> Visuals
- Connecting one camel to the other with leash -> do for all horses?
	- Only camels for now, maybe later
- If chest, then the jump ability is reduced
 */

public class DesertShip implements ModInitializer {
	public static final String MODID = "desertship";
	public static final String MODNAME = "Desert Ship";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("I sight a fata morgana on the horizon!", mod.metadata().name());
		ClientPlayNetworking.registerGlobalReceiver(CaravanUtil.CHESTED_CAMEL_PACKET_ID, ChestedCamelS2CPacket::handler);
	}
}
