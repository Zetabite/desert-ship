package zetabite.desertship.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zetabite.desertship.duckinterface.CamelEntityDuckInterface;

import static zetabite.desertship.util.CaravanUtil.onTryLeashDetach;

@Debug(export = true)
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
	public void desertship$interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ItemStack itemStack = player.getStackInHand(hand);

		if (player.shouldCancelInteraction() && !this.isBaby()) {
			if(this.isLeashed() && player.isSneaking() && itemStack.isEmpty()) {
				if (onTryLeashDetach(this, player, hand)) {
					cir.setReturnValue(ActionResult.success(this.getWorld().isClient));
				}
			}
			this.openInventory(player);
		} else if (this.isBreedingItem(itemStack)) {
			cir.setReturnValue(this.interactHorse(player, itemStack));
		} else if(!itemStack.isEmpty() && !this.hasChest() && itemStack.isOf(Items.CHEST)) {
			this.addChest(player, itemStack);
		} else if (this.getPassengerList().size() < 2 && !this.isBaby()) {
			this.putPlayerOnBack(player);
		}
		cir.setReturnValue(ActionResult.success(this.getWorld().isClient));
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

	@Override
	protected void updateLeash() {
		if (this.getHoldingEntity() instanceof CamelEntity holdingCamel) {
			if (!this.isAlive() || !holdingCamel.isAlive()) {
				this.detachLeash(true, true);
				return;
			}

			if (holdingCamel.getWorld() == this.getWorld()) {
				this.setPositionTarget(holdingCamel.getBlockPos(), 5);
				float f = this.distanceTo(holdingCamel);
				this.updateForLeashLength(f);

				if (f > 6.0F) {
					double d = (holdingCamel.getX() - this.getX()) / (double)f;
					double e = (holdingCamel.getY() - this.getY()) / (double)f;
					double g = (holdingCamel.getZ() - this.getZ()) / (double)f;
					this.setVelocity(this.getVelocity().add(Math.copySign(d * d * 0.4, d), Math.copySign(e * e * 0.4, e), Math.copySign(g * g * 0.4, g)));
					this.limitFallDistance();
				} else if (this.runsFromLeash()) {
					this.goalSelector.enableControl(Goal.Control.MOVE);
					float h = 2.0F;
					Vec3d vec3d = new Vec3d(holdingCamel.getX() - this.getX(), holdingCamel.getY() - this.getY(), holdingCamel.getZ() - this.getZ())
						.normalize()
						.multiply(Math.max(f - h, 0.0F));
					this.getNavigation().startMovingTo(this.getX() + vec3d.x, this.getY() + vec3d.y, this.getZ() + vec3d.z, this.getRunFromLeashSpeed());
				}
			}
		} else {
			super.updateLeash();
		}
	}
}
