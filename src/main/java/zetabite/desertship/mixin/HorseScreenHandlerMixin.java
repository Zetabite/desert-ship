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
import zetabite.desertship.CamelEntityDuckInterface;

@Mixin(HorseScreenHandler.class)
public abstract class HorseScreenHandlerMixin extends ScreenHandler {

	protected HorseScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	private boolean hasChestCamel(HorseBaseEntity horse) {
		return (horse instanceof CamelEntity && ((CamelEntityDuckInterface)horse).hasChest());
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	public void desertship$setCamelChestSlots(int syncId, PlayerInventory playerInventory, Inventory inventory, final HorseBaseEntity entity, CallbackInfo ci) {
		if (this.hasChestCamel(entity)) {
			int inventoryColumns = ((CamelEntityDuckInterface)entity).getInventoryColumns();

			for(int k = 0; k < 3; ++k) {
				for(int l = 0; l < inventoryColumns; ++l) {
					this.addSlot(new Slot(inventory, 2 + l + k * inventoryColumns, 80 + l * 18, 18 + k * 18));
				}
			}
		}
	}
}
