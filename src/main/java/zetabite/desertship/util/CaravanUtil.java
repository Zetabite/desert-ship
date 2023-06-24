package zetabite.desertship.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.List;

public abstract class CaravanUtil {
	public static <E extends Entity> List<MobEntity> getHeldEntities(E holderEntity) {
		return getHeldEntities(holderEntity, 7.0);
	}

	public static <E extends Entity>  List<MobEntity> getHeldEntities(E holderEntity, double radius) {
		Vec3d pos = holderEntity.getPos();
		int x = (int) pos.getX();
		int y = (int) pos.getY();
		int z = (int) pos.getZ();

		List<MobEntity> heldEntities = holderEntity.getWorld().getNonSpectatingEntities(
			MobEntity.class,
			new Box(
				(double)x - radius,
				(double)y - radius,
				(double)z - radius,
				(double)x + radius,
				(double)y + radius,
				(double)z + radius)
		);

		Iterator itr = heldEntities.iterator();

		while (itr.hasNext()) {
			if (itr.next() instanceof MobEntity mobEntity) {
				if (mobEntity.getHoldingEntity() == holderEntity) {
					continue;
				}
			}
			itr.remove();
		}
		return heldEntities;
	}

	public static <E extends Entity> boolean onTryLeashAttach(E entity, PlayerEntity player, Hand hand) {
		if (entity instanceof CamelEntity camel) {
			List<MobEntity> pHeldEntities = getHeldEntities(player);

			if (pHeldEntities.size() == 1 && pHeldEntities.get(0) instanceof CamelEntity otherCamel) {
				List<MobEntity> cHeldEntities = getHeldEntities(camel);
				List<MobEntity> oHeldEntities = getHeldEntities(otherCamel);

				// this helds camel
				if (oHeldEntities.size() == 0 && cHeldEntities.size() <= 1) {
					otherCamel.detachLeash(true, false);
					camel.attachLeash(otherCamel, true);
					return true;
				}
				// if other or neither helds camels
				else if (cHeldEntities.size() == 0 && oHeldEntities.size() == 1) {
					otherCamel.detachLeash(true, false);
					otherCamel.attachLeash(camel, true);
					return true;
				}
			}
		}
		return false;
	}

	public static <E extends Entity> boolean onTryLeashDetach(E entity, PlayerEntity player, Hand hand) {
		if (entity instanceof CamelEntity camel) {
			if (camel.getHoldingEntity() instanceof CamelEntity) {
				camel.detachLeash(true, true);
				return true;
			}
		}
		return false;
	}

	public static <E extends Entity> boolean isCaravanQualified(E entity) {
		return entity instanceof CamelEntity;
	}
}
