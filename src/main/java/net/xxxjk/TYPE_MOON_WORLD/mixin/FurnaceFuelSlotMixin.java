package net.xxxjk.TYPE_MOON_WORLD.mixin;

import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.alchemy.AlchemyFurnaceService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FurnaceFuelSlot.class)
public class FurnaceFuelSlotMixin {
   @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
   private void typemoonworld$allowAlchemyContainers(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      if (AlchemyFurnaceService.isAlchemyContainer(stack)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
   private void typemoonworld$singleAlchemyContainer(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
      if (AlchemyFurnaceService.isAlchemyContainer(stack)) {
         cir.setReturnValue(1);
      }
   }
}
