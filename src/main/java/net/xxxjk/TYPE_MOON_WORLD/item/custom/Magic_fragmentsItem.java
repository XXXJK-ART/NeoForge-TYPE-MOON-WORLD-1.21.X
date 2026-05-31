package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Using_mana;
import org.jetbrains.annotations.NotNull;

public class Magic_fragmentsItem extends Item {
   public Magic_fragmentsItem(Properties properties) {
      super(properties);
   }

   @OnlyIn(Dist.CLIENT)
   public boolean isFoil(@NotNull ItemStack itemstack) {
      return true;
   }

   @NotNull
   public InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player entity, @NotNull InteractionHand hand) {
      InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
      Using_mana.execute(entity);
      return ar;
   }
}
