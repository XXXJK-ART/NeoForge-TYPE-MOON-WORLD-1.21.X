package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class RandomStartAttributesItem extends Item {
   public RandomStartAttributesItem(Properties properties) {
      super(properties.stacksTo(1));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide) {
         reroll(player, level.getRandom());
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
         if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 32, 0.55, 0.55, 0.55, 0.08);
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.4, 0.45, 0.4, 0.04);
         }
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }

   private static void reroll(Player player, RandomSource random) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.is_magus = true;
      vars.player_max_mana = Math.round((100.0 + random.nextDouble() * 900.0) * 10.0) / 10.0;
      vars.player_mana = vars.player_max_mana;
      vars.player_mana_egenerated_every_moment = Math.round((1.0 + random.nextDouble() * 9.0) * 10.0) / 10.0;
      vars.player_restore_magic_moment = Math.round((1.0 + random.nextDouble() * 9.0) * 10.0) / 10.0;
      clearAttributes(vars);
      if (random.nextInt(100) < 10) {
         if (random.nextBoolean()) {
            vars.player_magic_attributes_none = true;
         } else {
            vars.player_magic_attributes_imaginary_number = true;
         }
      } else {
         int roll = random.nextInt(100);
         int count = 1;
         if (roll >= 99) {
            count = 5;
         } else if (roll >= 95) {
            count = 4;
         } else if (roll >= 80) {
            count = 3;
         } else if (roll >= 50) {
            count = 2;
         }
         List<Integer> available = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
         for (int i = 0; i < count && !available.isEmpty(); i++) {
            int attr = available.remove(random.nextInt(available.size()));
            switch (attr) {
               case 0 -> vars.player_magic_attributes_earth = true;
               case 1 -> vars.player_magic_attributes_water = true;
               case 2 -> vars.player_magic_attributes_fire = true;
               case 3 -> vars.player_magic_attributes_wind = true;
               case 4 -> vars.player_magic_attributes_ether = true;
               default -> {
               }
            }
         }
      }
      vars.magic_circuit_color_rgb = MagicCircuitColorHelper.resolveColor(vars);
      vars.syncPlayerVariables(player);
      player.displayClientMessage(
         Component.translatable(
            "message.typemoonworld.random_start_attributes.applied",
            String.format(java.util.Locale.ROOT, "%.1f", vars.player_max_mana),
            String.format(java.util.Locale.ROOT, "%.1f", vars.player_mana_egenerated_every_moment),
            String.format(java.util.Locale.ROOT, "%.1f", vars.player_restore_magic_moment / 20.0)
         ),
         true
      );
   }

   private static void clearAttributes(TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.player_magic_attributes_earth = false;
      vars.player_magic_attributes_water = false;
      vars.player_magic_attributes_fire = false;
      vars.player_magic_attributes_wind = false;
      vars.player_magic_attributes_ether = false;
      vars.player_magic_attributes_none = false;
      vars.player_magic_attributes_imaginary_number = false;
      vars.player_magic_attributes_sword = false;
   }
}
