package net.xxxjk.TYPE_MOON_WORLD.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.xxxjk.TYPE_MOON_WORLD.alchemy.AlchemyFurnaceService;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
   @Inject(method = "getBurnDuration", at = @At("HEAD"), cancellable = true)
   private void typemoonworld$alchemyContainerBurnTime(ItemStack fuel, CallbackInfoReturnable<Integer> cir) {
      AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)(Object)this;
      if (AlchemyFurnaceService.shouldKeepAlchemyContainer(furnace, fuel)) {
         cir.setReturnValue(AlchemyFurnaceService.ALCHEMY_BURN_TIME);
      }
   }

   @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
   private void typemoonworld$allowAlchemyContainerFuelSlot(int index, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)(Object)this;
      if (index == 1 && furnace.getType() == BlockEntityType.BLAST_FURNACE && AlchemyFurnaceService.isAlchemyContainer(stack)) {
         cir.setReturnValue(true);
      }
   }

   @Redirect(
      method = "serverTick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V")
   )
   private static void typemoonworld$keepAlchemyContainerDuringIgnition(
      ItemStack stack, int amount, Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity
   ) {
      if (!AlchemyFurnaceService.shouldKeepAlchemyContainer(blockEntity, stack)) {
         stack.shrink(amount);
      }
   }

   @Inject(
      method = "burn",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V", shift = At.Shift.BEFORE)
   )
   private static void typemoonworld$fillAlchemyContainer(
      RegistryAccess registryAccess,
      @Nullable RecipeHolder<?> recipe,
      NonNullList<ItemStack> inventory,
      int maxStackSize,
      AbstractFurnaceBlockEntity furnace,
      CallbackInfoReturnable<Boolean> cir
   ) {
      if (!AlchemyFurnaceService.isAlchemyBlast(furnace)) {
         return;
      }

      ItemStack fuel = inventory.get(1);
      ItemStack result = AlchemyFurnaceService.alchemyFuelResult(fuel);
      if (!result.isEmpty()) {
         inventory.set(1, result);
      }
   }
}
