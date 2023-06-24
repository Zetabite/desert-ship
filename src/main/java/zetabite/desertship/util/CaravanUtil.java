package zetabite.desertship.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import zetabite.desertship.DesertShip;

import java.util.ArrayList;

public abstract class CaravanUtil {
	private static ArrayList<MobEntity> getHeldEntities(Entity holderEntity) {
		return getHeldEntities(holderEntity, 7.0);
	}

	private static ArrayList<MobEntity> getHeldEntities(Entity holderEntity, double radius) {
		Vec3d pos = holderEntity.getPos();
		int x = (int) pos.getX();
		int y = (int) pos.getY();
		int z = (int) pos.getZ();

		ArrayList<MobEntity> heldEntities = new ArrayList<>();

		for(MobEntity mobEntity : holderEntity.getWorld().getNonSpectatingEntities(
			MobEntity.class,
			new Box(
				(double)x - radius,
				(double)y - radius,
				(double)z - radius,
				(double)x + radius,
				(double)y + radius,
				(double)z + radius)
		)) {
			if (mobEntity.getHoldingEntity() == holderEntity) {
				heldEntities.add(mobEntity);
			}
		}
		return heldEntities;
	}

	public static <E extends Entity> boolean onTryLeashAttach(E entity, PlayerEntity player, Hand hand) {
		if (entity instanceof MobEntity mob) {
			DesertShip.LOGGER.info("Fetching player held Entities");
			ArrayList<MobEntity> pHeldEntities = getHeldEntities(player);

			if (pHeldEntities.size() == 1 && pHeldEntities.get(0) instanceof CamelEntity otherCamel) {
				DesertShip.LOGGER.info("Fetching this held Entities");
				ArrayList<MobEntity> cHeldEntities = getHeldEntities(mob);
				DesertShip.LOGGER.info("Fetching other held Entities");
				ArrayList<MobEntity> oHeldEntities = getHeldEntities(otherCamel);

				// this helds camel
				if (oHeldEntities.size() == 0 && cHeldEntities.size() <= 1) {
					DesertShip.LOGGER.info("this camel secondary, other camel main");
					otherCamel.detachLeash(true, false);
					mob.attachLeash(otherCamel, true);
					return true;
					//mob.goalSelector.enableControl(Goal.Control.MOVE);
				}
				// if other or neither helds camels
				else if (cHeldEntities.size() == 0 && oHeldEntities.size() == 1) {
					DesertShip.LOGGER.info("other camel secondary, this camel main");
					otherCamel.detachLeash(true, false);
					otherCamel.attachLeash(mob, true);
					return true;
				}
			}
		}
		// Pseudo Code
		/*
		 * pList = player.heldEntities();
		 * if (pList.size() == 1 && pList.get(0) instanceof CamelEntity otherCamel) {
		 * 	// Camel is holding already, the new camel will be main
		 * 	if (camel.heldEntity() instanceof CamelEntity heldCamel) {
		 * 		otherCamel.setHoldingEntity(this);
		 * 	}
		 * 	// Is not already holding, becomes secondary
		 * 	else {
		 * 		this.setHoldingEntity(otherCamel);
		 * 	}
		 * }
		 */
		//this.attachLeash(player, true);
		//this.goalSelector.enableControl(Goal.Control.MOVE);
		return false;
	}

	public static <E extends Entity> boolean onTryLeashDetach(E entity, PlayerEntity player, Hand hand) {
		if (entity instanceof MobEntity mob) {
			return false;
		}
		//this.detachLeash(true, true);
		//this.goalSelector.disableControl(Goal.Control.MOVE);
		return false;
	}

	public static <E extends Entity> boolean isCaravanQualified(E entity) {
		return entity instanceof CamelEntity;
	}
}
