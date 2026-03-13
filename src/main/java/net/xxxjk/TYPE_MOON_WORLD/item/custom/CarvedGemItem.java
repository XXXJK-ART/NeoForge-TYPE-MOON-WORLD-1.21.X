package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemManaStorageService;
import org.jetbrains.annotations.NotNull;

public class CarvedGemItem extends Item {
   private final GemType type;
   private final GemQuality quality;
   private final Supplier<Item> fullGemSupplier;

   public CarvedGemItem(Properties properties, GemType type, GemQuality quality, Supplier<Item> fullGemSupplier) {
      super(properties);
      this.type = type;
      this.quality = quality;
      this.fullGemSupplier = fullGemSupplier;
   }

   private Item fullGemItem() {
      return this.fullGemSupplier.get();
   }

   public boolean isFoil(@NotNull ItemStack stack) {
      return false;
   }

   public GemQuality getQuality() {
      return this.quality;
   }

   public GemType getType() {
      return this.type;
   }

   @NotNull
   public Component getName(@NotNull ItemStack stack) {
      return (Component)(GemEngravingService.getEngravedMagicId(stack) == null
         ? super.getName(stack)
         : Component.translatable("item.typemoonworld.engraved_gem", new Object[]{Component.translatable(getGemColorKey(this.type))}));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.addAll(GemEngravingService.getEngravingDetailLines(stack));
   }

   @NotNull
   public InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (world.isClientSide) {
         return InteractionResultHolder.sidedSuccess(stack, true);
      } else if (player instanceof ServerPlayer serverPlayer) {
         stack = this.normalizeEngravedVariant(serverPlayer, hand, stack);
         if (player.isShiftKeyDown()) {
            ItemStack result = GemManaStorageService.storeIntoGem(serverPlayer, hand, stack, this.fullGemItem(), this.type, this.quality);
            return InteractionResultHolder.sidedSuccess(result, false);
         } else {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.use.empty"), true);
            return InteractionResultHolder.fail(stack);
         }
      } else {
         return InteractionResultHolder.pass(stack);
      }
   }

   private static String getGemColorKey(GemType type) {
      return switch (type) {
         case RUBY -> "item.typemoonworld.gem_color.ruby";
         case SAPPHIRE -> "item.typemoonworld.gem_color.sapphire";
         case EMERALD -> "item.typemoonworld.gem_color.emerald";
         case TOPAZ -> "item.typemoonworld.gem_color.topaz";
         case WHITE_GEMSTONE -> "item.typemoonworld.gem_color.white";
         case CYAN -> "item.typemoonworld.gem_color.cyan";
         case BLACK_SHARD -> "item.typemoonworld.gem_color.black";
      };
   }

   private ItemStack normalizeEngravedVariant(ServerPlayer player, InteractionHand hand, ItemStack stack) {
      if (GemEngravingService.getEngravedMagicId(stack) == null) {
         return stack;
      } else {
         Item normalizedItem = ModItems.getNormalizedCarvedGem(this.type);
         if (stack.getItem() == normalizedItem) {
            return stack;
         } else {
            ItemStack normalized = new ItemStack(normalizedItem, stack.getCount());
            GemEngravingService.copyEngravingData(stack, normalized);
            player.setItemInHand(hand, normalized);
            return normalized;
         }
      }
   }
}
