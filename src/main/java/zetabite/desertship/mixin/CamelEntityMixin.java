package zetabite.desertship.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zetabite.desertship.duckinterface.CamelEntityDuckInterface;
import zetabite.desertship.util.CaravanUtil;

import static zetabite.desertship.util.CaravanUtil.onTryLeashDetach;

@Debug(export = false)
@Mixin(CamelEntity.class)
public abstract class CamelEntityMixin extends HorseBaseEntity implements CamelEntityDuckInterface {
	private static final int INVENTORY_SLOT_COUNT = 15;
	private static final int INVENTORY_COLUMNS = 5;
	private static final int INVENTORY_ROWS = INVENTORY_SLOT_COUNT / INVENTORY_COLUMNS;
	private boolean chestedCamel = false;

	public CamelEntityMixin(EntityType<? extends CamelEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	public void desertship$writeChestDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		nbt.putBoolean("ChestedCamel", chestedCamel);

		if (chestedCamel) {
			NbtList itemNbtList = new NbtList();

			for(int i = INVENTORY_BASE_SIZE; i < this.items.size(); i++) {
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
		this.applyChestedChange(nbt.getBoolean("ChestedCamel"));

		if (this.hasChest()) {
			NbtList itemNbtList = nbt.getList("Items", NbtElement.COMPOUND_TYPE);

			for(int i = 0; i < itemNbtList.size(); i++) {
				NbtCompound slotNbt = itemNbtList.getCompound(i);
				int j = slotNbt.getByte("Slot") & 255;

				if (j >= INVENTORY_BASE_SIZE && j < this.items.size()) {
					this.items.setStack(j, ItemStack.fromNbt(slotNbt));
				}
			}
		}
		this.updateSaddle();
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
		return chestedCamel;
	}

	public void setHasChest(boolean hasChest) {
		this.chestedCamel = hasChest;

		if (!this.getWorld().isClient) {
			if (ClientPlayNetworking.canSend(CaravanUtil.CHESTED_CAMEL_PACKET_ID)) {
				PacketByteBuf buf = PacketByteBufs.create();
				buf.writeBoolean(hasChest);
				buf.writeInt(this.getId());

				ClientPlayNetworking.send(CaravanUtil.CHESTED_CAMEL_PACKET_ID, buf);
			}
		}
	}

	public int getInventorySlotCount() {
		return INVENTORY_SLOT_COUNT;
	}

	@Override
	protected int getInventorySize() {
		return this.chestedCamel ? (INVENTORY_BASE_SIZE + INVENTORY_SLOT_COUNT) : INVENTORY_BASE_SIZE;
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

	public void applyChestedChange(boolean hasChest) {
		this.setHasChest(hasChest);
		this.onChestedStatusChanged();
	}

	private StackReference createChestStackReference() {
		return new StackReference() {
			@Override
			public ItemStack get() {
				return ((CamelEntityDuckInterface)(CamelEntityMixin.this)).hasChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
			}

			@Override
			public boolean set(ItemStack stack) {
				if (stack.isEmpty()) {
					if (((CamelEntityDuckInterface)(CamelEntityMixin.this)).hasChest()) {
						((CamelEntityDuckInterface)(CamelEntityMixin.this)).applyChestedChange(false);
					}
					return true;
				} else if (stack.isOf(Items.CHEST)) {
					if (!((CamelEntityDuckInterface)(CamelEntityMixin.this)).hasChest()) {
						((CamelEntityDuckInterface)(CamelEntityMixin.this)).applyChestedChange(true);
					}
					return true;
				}
				return false;
			}
		};
	}

	@Override
	public StackReference getStackReference(int mappedIndex) {
		return mappedIndex == CHEST_SLOT_OFFSET ? createChestStackReference() : super.getStackReference(mappedIndex);
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
		return INVENTORY_COLUMNS;
	}

	public int getInventoryRows() {
		return INVENTORY_ROWS;
	}

	/*
	@Override
	protected void updateForLeashLength(float leashLength) {
		if (leashLength > CaravanUtil.MAX_LEASH_LENGTH && this.isEatingGrass()) {
			this.setEatingGrass(false);
		}
	}
	*/

	@Override
	protected void updateLeash() {
		if (this.getHoldingEntity() instanceof CamelEntity holdingCamel) {
			if (!this.isAlive() || !holdingCamel.isAlive()) {
				this.detachLeash(true, true);
				return;
			}

			if (holdingCamel.getWorld() != this.getWorld()) {
				return;
			}

			this.setPositionTarget(holdingCamel.getBlockPos(), 5);
			float currentLeashLength = this.distanceTo(holdingCamel);
			this.updateForLeashLength(currentLeashLength);

			if (currentLeashLength > CaravanUtil.MAX_LEASH_LENGTH) {
				double d = (holdingCamel.getX() - this.getX()) / (double)currentLeashLength;
				double e = (holdingCamel.getY() - this.getY()) / (double)currentLeashLength;
				double g = (holdingCamel.getZ() - this.getZ()) / (double)currentLeashLength;
				this.setVelocity(this.getVelocity().add(Math.copySign(d * d * 0.4, d), Math.copySign(e * e * 0.4, e), Math.copySign(g * g * 0.4, g)));
				this.limitFallDistance();
			} else if (this.runsFromLeash()) {
				this.goalSelector.enableControl(Goal.Control.MOVE);
				float h = 2.0F;
				Vec3d vec3d = new Vec3d(holdingCamel.getX() - this.getX(), holdingCamel.getY() - this.getY(), holdingCamel.getZ() - this.getZ());
				vec3d = vec3d.normalize().multiply(Math.max(currentLeashLength - h, 0.0F));
				this.getNavigation().startMovingTo(this.getX() + vec3d.x, this.getY() + vec3d.y, this.getZ() + vec3d.z, this.getRunFromLeashSpeed());
			}
		} else {
			super.updateLeash();
		}
	}
}
