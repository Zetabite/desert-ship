package zetabite.desertship.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Targeter;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zetabite.desertship.duckinterface.MobEntityDuckInterface;

import static zetabite.desertship.util.CaravanUtil.isCaravanQualified;
import static zetabite.desertship.util.CaravanUtil.onTryLeashAttach;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements Targeter, MobEntityDuckInterface {
	protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(
		method = "interactWithItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void interactWithItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ItemStack itemStack = player.getStackInHand(hand);

		if (player.isSneaking() && itemStack.isOf(Items.LEAD) && this.canBeLeashedBy(player)) {
			if (onTryLeashAttach(this, player, hand)) {
				cir.setReturnValue(ActionResult.success(this.getWorld().isClient));
			}
		}
	}
}
