package zetabite.desertship.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zetabite.desertship.CamelEntityDuckInterface;

@Mixin(CamelEntity.class)
public abstract class CamelEntityMixin extends HorseBaseEntity implements CamelEntityDuckInterface {
	private static final TrackedData<Boolean> CHEST = DataTracker.registerData(CamelEntityMixin.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final int INVENTORY_SLOT_COUNT = 15;

	public CamelEntityMixin(EntityType<? extends CamelEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	public void desertship$writeChestDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		nbt.putBoolean("ChestedCamel", this.hasChest());

		if (this.hasChest()) {
			NbtList itemNbtList = new NbtList();

			for(int i = 2; i < this.items.size(); ++i) {
				ItemStack itemStack = this.items.getStack(i);

				if (!itemStack.isEmpty()) {
					NbtCompound slotNbt = new NbtCompound();
					slotNbt.putByte("Slot", (byte)i);
					itemStack.writeNbt(slotNbt);
					itemNbtList.add(slotNbt);
				}
			}
			nbt.put("Items", itemNbtList);
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	public void desertship$readChestDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		this.setHasChest(nbt.getBoolean("ChestedCamel"));
		this.onChestedStatusChanged();

		if (this.hasChest()) {
			NbtList itemNbtList = nbt.getList("Items", NbtElement.COMPOUND_TYPE);

			for(int i = 0; i < itemNbtList.size(); ++i) {
				NbtCompound slotNbt = itemNbtList.getCompound(i);
				int j = slotNbt.getByte("Slot") & 255;

				if (j >= 2 && j < this.items.size()) {
					this.items.setStack(j, ItemStack.fromNbt(slotNbt));
				}
			}
		}
		this.updateSaddle();
	}

	@Inject(method = "initDataTracker", at = @At("TAIL"))
	public void desertship$initDataTrackerWithChest(CallbackInfo ci) {
		this.dataTracker.startTracking(CHEST, false);
	}

	@Inject(
		method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
		at = @At("HEAD"),
		cancellable = true
	)
	public void desertship$interactMobWithChest(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ItemStack itemStack = player.getStackInHand(hand);

		if (player.shouldCancelInteraction() && !this.isBaby()) {
			this.openInventory(player);
			cir.setReturnValue(ActionResult.success(this.getWorld().isClient));
		} else {
			ActionResult actionResult = itemStack.useOnEntity(player, this, hand);

			if (actionResult.isAccepted()) {
				cir.setReturnValue(actionResult);
			} else if (this.isBreedingItem(itemStack)) {
				cir.setReturnValue(this.interactHorse(player, itemStack));
			} else if(!itemStack.isEmpty() && !this.hasChest() && itemStack.isOf(Items.CHEST)) {
				this.addChest(player, itemStack);
			} else {
				if (this.getPassengerList().size() < 2 && !this.isBaby()) {
					this.putPlayerOnBack(player);
				}
			}
			cir.setReturnValue(ActionResult.success(this.getWorld().isClient));
		}
	}

	public boolean hasChest() {
		return this.dataTracker.get(CHEST);
	}

	public void setHasChest(boolean hasChest) {
		this.dataTracker.set(CHEST, hasChest);
	}

	public int getInventorySlotCount() {
		return INVENTORY_SLOT_COUNT;
	}

	@Override
	protected int getInventorySize() {
		return this.hasChest() ? (super.getInventorySize() + INVENTORY_SLOT_COUNT) : super.getInventorySize();
	}

	@Override
	protected void dropInventory() {
		super.dropInventory();

		if (this.hasChest()) {
			if (!this.getWorld().isClient) {
				this.dropItem(Blocks.CHEST);
			}
			this.setHasChest(false);
		}
	}

	private StackReference createChestStackReference() {
		return new StackReference() {
			@Override
			public ItemStack get() {
				return CamelEntityMixin.this.hasChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
			}

			@Override
			public boolean set(ItemStack stack) {
				boolean check = stack.isEmpty() || stack.isOf(Items.CHEST);

				if (stack.isEmpty() && CamelEntityMixin.this.hasChest()) {
					CamelEntityMixin.this.setHasChest(false);
					CamelEntityMixin.this.onChestedStatusChanged();
				} else if (stack.isOf(Items.CHEST) && !CamelEntityMixin.this.hasChest()) {
					CamelEntityMixin.this.setHasChest(true);
					CamelEntityMixin.this.onChestedStatusChanged();
				}
				return check;
			}
		};
	}

	@Override
	public StackReference getStackReference(int mappedIndex) {
		return mappedIndex == 449 ? createChestStackReference() : super.getStackReference(mappedIndex);
	}

	private void addChest(PlayerEntity player, ItemStack stack) {
		this.setHasChest(true);
		this.playAddChestSound();

		if (!player.getAbilities().creativeMode) {
			stack.decrement(1);
		}

		this.onChestedStatusChanged();
	}

	protected void playAddChestSound() {
		this.playSound(SoundEvents.ENTITY_DONKEY_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
	}

	public int getInventoryColumns() {
		return 5;
	}
}
