package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;

public class ServantCardItem extends Item {
   private static final String SERVANT_ID_TAG = "tmw_servant_id";
   private final String servantId;

   public ServantCardItem(Properties properties, String servantId) {
      super(properties);
      this.servantId = servantId;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
         String resolvedId = this.servantId(stack);
         if (!resolvedId.isBlank()) {
            ServantCardTransformManager.transform(serverPlayer, resolvedId);
         }
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }

   public String servantId() {
      return this.servantId;
   }

   public String servantId(ItemStack stack) {
      if (this.servantId != null && !this.servantId.isBlank()) return this.servantId;
      CustomData data = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? "" : data.copyTag().getString(SERVANT_ID_TAG);
   }

   @Override
   public Component getName(ItemStack stack) {
      String id = this.servantId(stack);
      if (this.servantId == null || this.servantId.isBlank()) {
         ResourceLocation key = ResourceLocation.tryParse(id);
         if (key != null) return Component.translatable("item." + key.getNamespace() + ".servant_card." + key.getPath());
      }
      return super.getName(stack);
   }

   public static ItemStack create(Item item, String servantId) {
      ItemStack stack = new ItemStack(item);
      CompoundTag tag = new CompoundTag();
      tag.putString(SERVANT_ID_TAG, servantId == null ? "" : servantId);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }
}
