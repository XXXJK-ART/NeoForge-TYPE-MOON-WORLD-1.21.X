package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HassanZabaniyaService;

public final class ZabaniyaCoreItem extends Item {
   private final boolean crushable;

   public ZabaniyaCoreItem(Properties properties, boolean crushable) {
      super(properties);
      this.crushable = crushable;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide() && crushable) HassanZabaniyaService.crush(player, stack, hand);
      return crushable ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide()) : InteractionResultHolder.pass(stack);
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
      if (HassanZabaniyaService.expire(stack, living)) return;
      if (entity instanceof net.minecraft.server.level.ServerPlayer player && (selected || player.getOffhandItem() == stack)
         && level.getGameTime() % 20 == 0) {
         long remaining = Math.max(0, (HassanZabaniyaService.data(stack).getLong(HassanZabaniyaService.EXPIRES)
            - level.getServer().overworld().getGameTime() + 19) / 20);
         player.displayClientMessage(Component.translatable("message.typemoonworld.hassan.remaining", remaining), true);
      }
   }

   @Override
   public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
      if (!entity.level().isClientSide() && HassanZabaniyaService.data(stack).getLong(HassanZabaniyaService.EXPIRES)
         <= entity.level().getServer().overworld().getGameTime()) entity.discard();
      return false;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      var data = HassanZabaniyaService.data(stack);
      tooltip.add(Component.translatable("tooltip.typemoonworld.hassan.target", data.getString("TargetName")).withStyle(ChatFormatting.GRAY));
      if (context.level() != null) tooltip.add(Component.translatable("message.typemoonworld.hassan.remaining",
         Math.max(0, (data.getLong(HassanZabaniyaService.EXPIRES) - context.level().getGameTime() + 19) / 20)).withStyle(ChatFormatting.RED));
      if (!crushable) tooltip.add(Component.translatable("tooltip.typemoonworld.hassan.clay").withStyle(ChatFormatting.DARK_GRAY));
   }
}
