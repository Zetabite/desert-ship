package zetabite.desertship.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zetabite.desertship.CamelEntityDuckInterface;

@Environment(EnvType.CLIENT)
@Mixin(HorseScreen.class)
public abstract class HorseScreenMixin extends HandledScreen<HorseScreenHandler> {
	@Shadow
	private HorseBaseEntity entity;

	@Shadow
	private static Identifier TEXTURE;

	public HorseScreenMixin(HorseScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Inject(method = "drawBackground", at = @At("TAIL"))
	protected void desertship$drawCamelChestBackground(GuiGraphics graphics, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		int i = (this.width - this.backgroundWidth) / 2;
		int j = (this.height - this.backgroundHeight) / 2;

		if (this.entity instanceof CamelEntity camel && ((CamelEntityDuckInterface)camel).hasChest()) {
			graphics.drawTexture(this.TEXTURE, i + 79, j + 17, 0, this.backgroundHeight, ((CamelEntityDuckInterface)camel).getInventoryColumns() * 18, 54);
		}
	}
}
