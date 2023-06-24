package zetabite.desertship.networking.packet;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.network.PacketByteBuf;
import org.quiltmc.qsl.networking.api.PacketSender;
import zetabite.desertship.duckinterface.CamelEntityDuckInterface;

public abstract class ChestedCamelS2CPacket {
	public static void handler(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
		boolean hasChest = buf.readBoolean();
		int entityId = buf.readInt();
		CamelEntity camel = (CamelEntity) handler.getWorld().getEntityById(entityId);
		((CamelEntityDuckInterface)camel).setHasChest(hasChest);
	}
}
