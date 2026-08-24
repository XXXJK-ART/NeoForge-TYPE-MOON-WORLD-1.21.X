package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.Random;
import org.joml.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ProjectionCircuitEffectEntity;
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

   @Override
   public InteractionResult useOn(UseOnContext context) {
      if (context.getHand() != InteractionHand.MAIN_HAND || context.getClickedFace() != Direction.UP) {
         return InteractionResult.PASS;
      }

      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      }

      ItemStack offhandStack = player.getOffhandItem();
      boolean mercuryCircuit = offhandStack.is(ModItems.MERCURY_BOTTLE.get());
      if (!mercuryCircuit && !(offhandStack.getItem() instanceof MoltenGemBottleItem)) {
         return InteractionResult.PASS;
      }

      Level level = context.getLevel();
      if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
         int color = mercuryCircuit ? 0xB5C1C9 : circuitColor(((MoltenGemBottleItem)offhandStack.getItem()).getType());
         drawCircuit(serverLevel, context.getClickedPos(), color, mercuryCircuit);
         consumeBottle(player, offhandStack);
         context.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
         player.displayClientMessage(
            Component.translatable(mercuryCircuit
               ? "message.typemoonworld.chisel.draw_mercury_circuit"
               : "message.typemoonworld.chisel.draw_molten_circuit"),
            true
         );
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
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
               ModItems.CARVED_EMERALD_POOR.get(), ModItems.CARVED_EMERALD.get(), ModItems.CARVED_EMERALD_HIGH.get()
            );
         } else if (rawItem == ModItems.RAW_RUBY.get()) {
            resultGem = this.getRandomQualityGem(ModItems.CARVED_RUBY_POOR.get(), ModItems.CARVED_RUBY.get(), ModItems.CARVED_RUBY_HIGH.get());
         } else if (rawItem == ModItems.RAW_SAPPHIRE.get()) {
            resultGem = this.getRandomQualityGem(
               ModItems.CARVED_SAPPHIRE_POOR.get(), ModItems.CARVED_SAPPHIRE.get(), ModItems.CARVED_SAPPHIRE_HIGH.get()
            );
         } else if (rawItem == ModItems.RAW_TOPAZ.get()) {
            resultGem = this.getRandomQualityGem(
               ModItems.CARVED_TOPAZ_POOR.get(), ModItems.CARVED_TOPAZ.get(), ModItems.CARVED_TOPAZ_HIGH.get()
            );
         } else if (rawItem == ModItems.RAW_WHITE_GEMSTONE.get()) {
            resultGem = this.getRandomQualityGem(
               ModItems.CARVED_WHITE_GEMSTONE_POOR.get(), ModItems.CARVED_WHITE_GEMSTONE.get(), ModItems.CARVED_WHITE_GEMSTONE_HIGH.get()
            );
         } else if (rawItem == ModItems.RAW_CYAN_GEMSTONE.get()) {
            resultGem = this.getRandomQualityGem(
               ModItems.CARVED_CYAN_GEMSTONE_POOR.get(), ModItems.CARVED_CYAN_GEMSTONE.get(), ModItems.CARVED_CYAN_GEMSTONE_HIGH.get()
            );
         } else if (rawItem == Items.OBSIDIAN) {
            resultGem = this.getRandomQualityGem(
               ModItems.CARVED_BLACK_SHARD_POOR.get(), ModItems.CARVED_BLACK_SHARD.get(), ModItems.CARVED_BLACK_SHARD_HIGH.get()
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

   private static void consumeBottle(Player player, ItemStack bottleStack) {
      if (player.getAbilities().instabuild) {
         return;
      }

      bottleStack.shrink(1);
      ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
      if (bottleStack.isEmpty()) {
         player.setItemInHand(InteractionHand.OFF_HAND, emptyBottle);
      } else if (!player.getInventory().add(emptyBottle)) {
         player.drop(emptyBottle, false);
      }
   }

   private static int circuitColor(GemType type) {
      return switch (type) {
         case RUBY -> 0xE5453A;
         case SAPPHIRE -> 0x3F72D8;
         case EMERALD -> 0x3CB66A;
         case TOPAZ -> 0xE5BE37;
         case WHITE_GEMSTONE -> 0xE8E8F2;
         case CYAN -> 0x35C6CC;
         case BLACK_SHARD -> 0x4A4355;
      };
   }

   private static void drawCircuit(ServerLevel level, BlockPos pos, int color, boolean mercuryCircuit) {
      double centerX = pos.getX() + 0.5;
      double centerY = pos.getY() + 1.015;
      double centerZ = pos.getZ() + 0.5;
      level.addFreshEntity(new ProjectionCircuitEffectEntity(level, centerX, centerY, centerZ, 1.85F, 1.85F, 0.8F, 100, color));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, centerX, centerY, centerZ, 0.35F, 2.0F, 0.11F, 24, color, 0.9F, 0.0F));

      float red = ((color >> 16) & 255) / 255.0F;
      float green = ((color >> 8) & 255) / 255.0F;
      float blue = (color & 255) / 255.0F;
      DustParticleOptions dust = new DustParticleOptions(new Vector3f(red, green, blue), mercuryCircuit ? 1.0F : 1.25F);
      for (int i = 0; i < 36; i++) {
         double angle = Math.PI * 2.0 * i / 36.0;
         double radius = 1.7;
         level.sendParticles(dust, centerX + Math.cos(angle) * radius, centerY, centerZ + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
      }

      for (int i = -10; i <= 10; i++) {
         double offset = i * 0.14;
         level.sendParticles(dust, centerX + offset, centerY, centerZ, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(dust, centerX, centerY, centerZ + offset, 1, 0.0, 0.0, 0.0, 0.0);
      }

      level.sendParticles(mercuryCircuit ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.ENCHANT,
         centerX, centerY + 0.04, centerZ, 16, 1.45, 0.03, 1.45, 0.015);
      level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, mercuryCircuit ? 0.8F : 1.2F);
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
