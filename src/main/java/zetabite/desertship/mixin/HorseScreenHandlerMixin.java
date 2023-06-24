package zetabite.desertship.mixin;

import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zetabite.desertship.duckinterface.CamelEntityDuckInterface;

@Mixin(HorseScreenHandler.class)
public abstract class HorseScreenHandlerMixin extends ScreenHandler {

	protected HorseScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	private boolean hasChestCamel(HorseBaseEntity horse) {
		if (horse instanceof CamelEntity camel) {
			return ((CamelEntityDuckInterface)camel).hasChest();
		}
		return false;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	public void desertship$setCamelChestSlots(int syncId, PlayerInventory playerInventory, Inventory inventory, final HorseBaseEntity entity, CallbackInfo ci) {
		if (this.hasChestCamel(entity)) {
			int inventoryColumns = ((CamelEntityDuckInterface)entity).getInventoryColumns();
			int inventoryRows = ((CamelEntityDuckInterface)entity).getInventoryRows();

			for(int row = 0; row < inventoryRows; row++) {
				for(int column = 0; column < inventoryColumns; column++) {
					this.addSlot(
						new Slot(inventory, CamelEntity.INVENTORY_BASE_SIZE + column + row * inventoryColumns, 80 + column * 18, 18 + row * 18)
					);
				}
			}
		}
	}
}
