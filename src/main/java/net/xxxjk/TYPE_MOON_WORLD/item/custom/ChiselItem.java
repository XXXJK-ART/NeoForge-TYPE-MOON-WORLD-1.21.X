package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.jetbrains.annotations.NotNull;

public class ChiselItem extends Item {
   private final Random random = new Random();

   public ChiselItem(Properties properties) {
      super(properties);
   }

   public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
      return true;
   }

   @NotNull
   public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
      ItemStack retrieval = new ItemStack(this);
      retrieval.setDamageValue(itemstack.getDamageValue() + 1);
      return retrieval.getDamageValue() >= retrieval.getMaxDamage() ? ItemStack.EMPTY : retrieval;
   }

   public boolean isRepairable(@NotNull ItemStack itemstack) {
      return false;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack chiselStack = player.getItemInHand(hand);
      if (!level.isClientSide) {
         ItemStack offhandStack = player.getOffhandItem();
         if (offhandStack.isEmpty()) {
            return InteractionResultHolder.pass(chiselStack);
         }

         if (player.isShiftKeyDown() && offhandStack.getItem() instanceof CarvedGemItem) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.engrave.need_table"), true);
            return InteractionResultHolder.fail(chiselStack);
         }

         Item rawItem = offhandStack.getItem();
         ItemStack resultGem = ItemStack.EMPTY;
         if (rawItem == ModItems.RAW_EMERALD.get()) {
            resultGem = this.getRandomQualityGem(
               (Item)ModItems.CARVED_EMERALD_POOR.get(), (Item)ModItems.CARVED_EMERALD.get(), (Item)ModItems.CARVED_EMERALD_HIGH.get()
            );
         } else if (rawItem == ModItems.RAW_RUBY.get()) {
            resultGem = this.getRandomQualityGem((Item)ModItems.CARVED_RUBY_POOR.get(), (Item)ModItems.CARVED_RUBY.get(), (Item)ModItems.CARVED_RUBY_HIGH.get());
         } else if (rawItem == ModItems.RAW_SAPPHIRE.get()) {
            resultGem = this.getRandomQualityGem(
               (Item)ModItems.CARVED_SAPPHIRE_POOR.get(), (Item)ModItems.CARVED_SAPPHIRE.get(), (Item)ModItems.CARVED_SAPPHIRE_HIGH.get()
            );
         } else if (rawItem == ModItems.RAW_TOPAZ.get()) {
            resultGem = this.getRandomQualityGem(
               (Item)ModItems.CARVED_TOPAZ_POOR.get(), (Item)ModItems.CARVED_TOPAZ.get(), (Item)ModItems.CARVED_TOPAZ_HIGH.get()
            );
         } else if (rawItem == ModItems.RAW_WHITE_GEMSTONE.get()) {
            resultGem = this.getRandomQualityGem(
               (Item)ModItems.CARVED_WHITE_GEMSTONE_POOR.get(), (Item)ModItems.CARVED_WHITE_GEMSTONE.get(), (Item)ModItems.CARVED_WHITE_GEMSTONE_HIGH.get()
            );
         } else if (rawItem == ModItems.RAW_CYAN_GEMSTONE.get()) {
            resultGem = this.getRandomQualityGem(
               (Item)ModItems.CARVED_CYAN_GEMSTONE_POOR.get(), (Item)ModItems.CARVED_CYAN_GEMSTONE.get(), (Item)ModItems.CARVED_CYAN_GEMSTONE_HIGH.get()
            );
         } else if (rawItem == Items.OBSIDIAN) {
            resultGem = this.getRandomQualityGem(
               (Item)ModItems.CARVED_BLACK_SHARD_POOR.get(), (Item)ModItems.CARVED_BLACK_SHARD.get(), (Item)ModItems.CARVED_BLACK_SHARD_HIGH.get()
            );
         }

         if (!resultGem.isEmpty()) {
            offhandStack.shrink(1);
            chiselStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            if (!player.getInventory().add(resultGem)) {
               player.drop(resultGem, false);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(this, 2);
            return InteractionResultHolder.success(chiselStack);
         }
      }

      return InteractionResultHolder.pass(chiselStack);
   }

   private ItemStack getRandomQualityGem(Item poor, Item normal, Item high) {
      int roll = this.random.nextInt(100);
      if (roll < 10) {
         return new ItemStack(poor);
      } else {
         return roll < 70 ? new ItemStack(normal) : new ItemStack(high);
      }
   }
}
