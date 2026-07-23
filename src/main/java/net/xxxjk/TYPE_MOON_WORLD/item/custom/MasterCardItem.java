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
import net.xxxjk.TYPE_MOON_WORLD.api.MasterProfileApiRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterCardProfile;

public class MasterCardItem extends Item {
   private final String masterId;

   public MasterCardItem(Properties properties, String masterId) {
      super(properties);
      this.masterId = masterId;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
         MasterCardProfile.apply(serverPlayer, masterId(stack), hand);
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }

   public String masterId() {
      return this.masterId;
   }

   public String masterId(ItemStack stack) {
      if (this.masterId != null && !this.masterId.isBlank()) return this.masterId;
      CustomData data = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? "" : data.copyTag().getString("tmw_master_id");
   }

   @Override public Component getName(ItemStack stack) {
      ResourceLocation id = ResourceLocation.tryParse(masterId(stack));
      MasterProfileApiRegistry.Entry entry = MasterProfileApiRegistry.get(id);
      return entry == null ? super.getName(stack) : Component.translatable(entry.data().nameKey());
   }

   public static ItemStack create(Item item, String masterId) {
      ItemStack stack = new ItemStack(item);
      CompoundTag tag = new CompoundTag();
      tag.putString("tmw_master_id", masterId == null ? "" : masterId);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }
}
