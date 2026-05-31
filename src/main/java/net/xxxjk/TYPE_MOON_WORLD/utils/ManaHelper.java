package net.xxxjk.TYPE_MOON_WORLD.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class ManaHelper {
   private static final String NO_EMERGENCY_RESTORE_UNTIL = "TypeMoonNoEmergencyRestoreUntil";
   private static final double MAGIC_FRAGMENT_MANA = 10.0;
   private static final double SPIRIT_VEIN_BLOCK_MANA = 90.0;

   public static boolean consumeManaOrHealth(ServerPlayer player, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.is_magus) {
         return false;
      } else if (vars.player_mana >= amount) {
         vars.player_mana -= amount;
         vars.syncMana(player);
         return true;
      } else {
         double missingMana = amount - vars.player_mana;
         double healthCost = missingMana / 10.0;
         if (healthCost < 0.5) {
            healthCost = 0.5;
         }

         if (player.getHealth() > healthCost) {
            vars.player_mana = 0.0;
            vars.syncMana(player);
            DamageSource source = player.damageSources().magic();
            player.hurt(source, (float)healthCost);
            player.displayClientMessage(
               Component.translatable("message.typemoonworld.mana.health_conversion", String.format("%.1f", healthCost), (int)amount), true
            );
            return true;
         } else {
            player.displayClientMessage(Component.translatable("message.typemoonworld.mana.not_enough_health"), true);
            return false;
         }
      }
   }

   public static boolean consumeManaWithInventoryOrHealth(ServerPlayer player, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.is_magus) {
         return false;
      } else if (vars.player_mana >= amount) {
         vars.player_mana -= amount;
         vars.syncMana(player);
         return true;
      } else {
         refillFromInventoryForCost(player, vars, amount);
         if (vars.player_mana >= amount) {
            vars.player_mana -= amount;
            vars.syncMana(player);
            return true;
         } else {
            return consumeManaOrHealth(player, amount);
         }
      }
   }

   public static boolean consumeOneTimeMagicCost(ServerPlayer player, double amount) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.is_magus) {
         return false;
      } else if (amount <= 0.0) {
         return true;
      } else {
         ManaHelper.HandSourceCandidate mainHandMedium = createHandCandidate(player.getMainHandItem(), InteractionHand.MAIN_HAND);
         ManaHelper.HandSourceCandidate offHandMedium = createHandCandidate(player.getOffhandItem(), InteractionHand.OFF_HAND);
         boolean hasHandMedium = mainHandMedium != null || offHandMedium != null;
         if (!hasHandMedium) {
            return consumeManaOrHealth(player, amount);
         } else {
            ManaHelper.HandSourceCandidate chosen = null;
            if (mainHandMedium != null && mainHandMedium.mana >= amount) {
               chosen = mainHandMedium;
            } else if (offHandMedium != null && offHandMedium.mana >= amount) {
               chosen = offHandMedium;
            }

            if (chosen == null) {
               suppressEmergencyManaRestore(player, 40);
               player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
               return false;
            } else {
               consumeHandCandidate(player, chosen);
               return true;
            }
         }
      }
   }

   public static boolean consumeManaStrict(ServerPlayer player, double amount, boolean drainOnFail) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.is_magus) {
         return false;
      } else if (vars.player_mana >= amount) {
         vars.player_mana -= amount;
         vars.syncMana(player);
         return true;
      } else {
         if (drainOnFail && vars.player_mana > 0.0) {
            vars.player_mana = 0.0;
            vars.syncMana(player);
         }

         suppressEmergencyManaRestore(player, 40);
         return false;
      }
   }

   public static void suppressEmergencyManaRestore(ServerPlayer player, int ticks) {
      long now = player.level().getGameTime();
      long until = now + Math.max(1, ticks);
      player.getPersistentData().putLong("TypeMoonNoEmergencyRestoreUntil", until);
   }

   public static boolean isEmergencyManaRestoreSuppressed(Entity entity) {
      if (entity == null) {
         return false;
      } else {
         long now = entity.level().getGameTime();
         long until = entity.getPersistentData().getLong("TypeMoonNoEmergencyRestoreUntil");
         return until > now;
      }
   }

   private static void refillFromInventoryForCost(Player player, TypeMoonWorldModVariables.PlayerVariables vars, double requiredCost) {
      for (double need = requiredCost - vars.player_mana; need > 1.0E-4; need = requiredCost - vars.player_mana) {
         ManaHelper.SourceCandidate best = findBestCandidate(player, need);
         if (best == null) {
            return;
         }

         consumeCandidate(player, best);
         vars.player_mana = Math.min(vars.player_mana + best.mana, vars.player_max_mana);
      }
   }

   private static ManaHelper.SourceCandidate findBestCandidate(Player player, double need) {
      ManaHelper.SourceCandidate best = null;

      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (!stack.isEmpty()) {
            ManaHelper.SourceCandidate candidate = createCandidate(i, stack);
            if (candidate != null) {
               if (best == null) {
                  best = candidate;
               } else {
                  boolean candidateEnough = candidate.mana >= need;
                  boolean bestEnough = best.mana >= need;
                  if (candidateEnough && !bestEnough) {
                     best = candidate;
                  } else if (candidateEnough && bestEnough && candidate.mana < best.mana) {
                     best = candidate;
                  } else if (!candidateEnough && !bestEnough && candidate.mana > best.mana) {
                     best = candidate;
                  }
               }
            }
         }
      }

      return best;
   }

   private static ManaHelper.SourceCandidate createCandidate(int slot, ItemStack stack) {
      if (stack.getItem() instanceof FullManaCarvedGemItem fullGemItem) {
         return new ManaHelper.SourceCandidate(slot, fullGemItem.getManaAmount(), new ItemStack(fullGemItem.getEmptyGemItem()));
      } else if (stack.is(ModItems.MAGIC_FRAGMENTS.get())) {
         return new ManaHelper.SourceCandidate(slot, 10.0, ItemStack.EMPTY);
      } else {
         return stack.is(((Block)ModBlocks.SPIRIT_VEIN_BLOCK.get()).asItem()) ? new ManaHelper.SourceCandidate(slot, 90.0, ItemStack.EMPTY) : null;
      }
   }

   private static void consumeCandidate(Player player, ManaHelper.SourceCandidate candidate) {
      ItemStack stack = player.getInventory().getItem(candidate.slot);
      if (!stack.isEmpty()) {
         if (stack.getCount() > 1) {
            stack.shrink(1);
            if (!candidate.remainder.isEmpty()) {
               ItemStack remainderCopy = candidate.remainder.copy();
               if (!player.getInventory().add(remainderCopy)) {
                  player.drop(remainderCopy, false);
               }
            }
         } else if (candidate.remainder.isEmpty()) {
            player.getInventory().setItem(candidate.slot, ItemStack.EMPTY);
         } else {
            player.getInventory().setItem(candidate.slot, candidate.remainder.copy());
         }

         player.getInventory().setChanged();
      }
   }

   private static ManaHelper.HandSourceCandidate createHandCandidate(ItemStack stack, InteractionHand hand) {
      if (stack.isEmpty()) {
         return null;
      } else if (stack.getItem() instanceof FullManaCarvedGemItem fullGemItem) {
         return new ManaHelper.HandSourceCandidate(hand, fullGemItem.getManaAmount(), new ItemStack(fullGemItem.getEmptyGemItem()));
      } else if (stack.is(ModItems.MAGIC_FRAGMENTS.get())) {
         return new ManaHelper.HandSourceCandidate(hand, 10.0, ItemStack.EMPTY);
      } else {
         return stack.is(((Block)ModBlocks.SPIRIT_VEIN_BLOCK.get()).asItem()) ? new ManaHelper.HandSourceCandidate(hand, 90.0, ItemStack.EMPTY) : null;
      }
   }

   private static void consumeHandCandidate(ServerPlayer player, ManaHelper.HandSourceCandidate candidate) {
      ItemStack handStack = player.getItemInHand(candidate.hand);
      if (!handStack.isEmpty()) {
         if (handStack.getCount() > 1) {
            handStack.shrink(1);
            if (!candidate.remainder.isEmpty()) {
               ItemStack remainderCopy = candidate.remainder.copy();
               if (!player.getInventory().add(remainderCopy)) {
                  player.drop(remainderCopy, false);
               }
            }
         } else if (candidate.remainder.isEmpty()) {
            player.setItemInHand(candidate.hand, ItemStack.EMPTY);
         } else {
            player.setItemInHand(candidate.hand, candidate.remainder.copy());
         }

         player.getInventory().setChanged();
      }
   }

   private record HandSourceCandidate(InteractionHand hand, double mana, ItemStack remainder) {
   }

   private record SourceCandidate(int slot, double mana, ItemStack remainder) {
   }
}
