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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.xxxjk.TYPE_MOON_WORLD.client.GemGravityModeClient;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemManaStorageService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemUseService;
import org.jetbrains.annotations.NotNull;

public class FullManaCarvedGemItem extends Item {
   private final Supplier<Item> emptyGemSupplier;
   private final GemQuality quality;
   private final GemType type;

   public FullManaCarvedGemItem(Properties properties, Supplier<Item> emptyGemSupplier, GemQuality quality, GemType type) {
      super(properties);
      this.emptyGemSupplier = emptyGemSupplier;
      this.quality = quality;
      this.type = type;
   }

   public GemQuality getQuality() {
      return this.quality;
   }

   public GemType getType() {
      return this.type;
   }

   public Item getEmptyGemItem() {
      return this.emptyGemSupplier.get();
   }

   public double getManaAmount(ItemStack stack) {
      int manaAmount = this.quality.getCapacity(this.type);
      if (stack != null && !stack.isEmpty()) {
         if (GemEngravingService.getEngravedMagicId(stack) != null) {
            double engravedCost = GemEngravingService.getEngravedManaCost(stack);
            if (engravedCost > 0.0) {
               manaAmount = Math.max(1, (int)Math.ceil(engravedCost));
            }
         }

         return manaAmount;
      } else {
         return manaAmount;
      }
   }

   @Deprecated(
      forRemoval = false
   )
   public double getManaAmount() {
      return this.quality.getCapacity(this.type);
   }

   @NotNull
   public Component getName(@NotNull ItemStack stack) {
      return (Component)(GemEngravingService.getEngravedMagicId(stack) == null
         ? super.getName(stack)
         : Component.translatable("item.typemoonworld.engraved_gem_full", Component.translatable(getGemColorKey(this.type))));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.addAll(GemEngravingService.getEngravingDetailLines(stack));
   }

   @OnlyIn(Dist.CLIENT)
   public boolean isFoil(@NotNull ItemStack stack) {
      return true;
   }

   @NotNull
   public InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      String engravedMagicId = GemEngravingService.getEngravedMagicId(stack);
      boolean shiftGravitySelfCast = player.isShiftKeyDown() && "gravity_magic".equals(engravedMagicId);
      if (world.isClientSide) {
         if (shiftGravitySelfCast && FMLEnvironment.dist == Dist.CLIENT) {
            FullManaCarvedGemItem.ClientHandler.openGravitySelector(hand);
         }

         return InteractionResultHolder.sidedSuccess(stack, true);
      } else if (player instanceof ServerPlayer serverPlayer) {
         stack = this.normalizeEngravedVariant(serverPlayer, hand, stack);
         if (shiftGravitySelfCast) {
            return InteractionResultHolder.sidedSuccess(stack, false);
         } else if (player.isShiftKeyDown()) {
            ItemStack result = GemManaStorageService.withdrawFromGem(serverPlayer, hand, stack, this.emptyGemSupplier.get(), this.type, this.quality);
            return InteractionResultHolder.sidedSuccess(result, false);
         } else {
            ItemStack result = GemUseService.useFilledGem(world, player, hand, stack, this.type);
            return InteractionResultHolder.sidedSuccess(result, false);
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
         Item normalizedItem = ModItems.getNormalizedFullCarvedGem(this.type);
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

   private static final class ClientHandler {
      private static void openGravitySelector(InteractionHand hand) {
         GemGravityModeClient.openSelector(hand);
      }
   }
}
