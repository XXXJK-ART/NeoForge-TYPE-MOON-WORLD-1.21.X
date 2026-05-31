package net.xxxjk.TYPE_MOON_WORLD.procedures;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;

@EventBusSubscriber
public class Restore_mana {
   private static final double CRITICAL_MANA_THRESHOLD = 20.0;
   private static final double MAGIC_FRAGMENT_MANA = 10.0;
   private static final double SPIRIT_VEIN_BLOCK_MANA = 90.0;
   private static final double HEALTH_CONVERT_MANA = 20.0;
   private static final float HEALTH_CONVERT_COST = 2.0F;

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      execute(event, event.getEntity().level(), event.getEntity());
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerRespawnEvent event) {
      TYPE_MOON_WORLD.queueServerWork(20, () -> execute(event, event.getEntity().level(), event.getEntity()));
   }

   public static void execute(LevelAccessor world, Entity entity) {
      execute(null, world, entity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
      if (entity != null && entity.isAlive()) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.is_magus) {
            TYPE_MOON_WORLD.queueServerWork(100, () -> execute(world, entity));
         } else {
            double manaRegen = vars.player_mana_egenerated_every_moment;
            double regenInterval = vars.player_restore_magic_moment;
            double regenMultiplier = 1.0;
            if (entity.level() instanceof ServerLevel serverLevel) {
               regenMultiplier = LeylineService.getRegenMultiplier(serverLevel, entity.blockPosition());
            }

            vars.current_mana_regen_multiplier = regenMultiplier;
            if (vars.is_magic_circuit_open) {
               regenInterval /= 2.0;
               if (regenInterval < 1.0) {
                  regenInterval = 1.0;
               }

               vars.magic_circuit_open_timer += regenInterval;
               if (vars.magic_circuit_open_timer >= 72000.0) {
                  vars.is_magic_circuit_open = false;
                  vars.magic_circuit_open_timer = 0.0;
                  if (entity instanceof LivingEntity living) {
                     living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 1));
                     living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1));
                     living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, 1));
                  }

                  if (entity instanceof Player player && !player.level().isClientSide()) {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.circuit.forced_close"), true);
                  }
               }
            } else {
               vars.magic_circuit_open_timer = 0.0;
            }

            double maxMana = vars.player_max_mana;
            double currentMana = vars.player_mana;
            if (currentMana > maxMana) {
               if (entity instanceof Player player && !player.level().isClientSide()) {
                  ChatFormatting color = ChatFormatting.YELLOW;
                  if (currentMana > maxMana * 1.25) {
                     color = ChatFormatting.DARK_RED;
                  } else if (currentMana > maxMana * 1.2) {
                     color = ChatFormatting.RED;
                  }

                  player.displayClientMessage(
                     Component.translatable("message.typemoonworld.mana.overload.warning", (int)currentMana, (int)maxMana).withStyle(color), true
                  );
               }

               if (currentMana > maxMana * 1.3 && entity instanceof LivingEntity living) {
                  living.hurt(living.damageSources().magic(), Float.MAX_VALUE);
                  if (entity instanceof Player player) {
                     player.sendSystemMessage(Component.translatable("message.typemoonworld.mana.overload.death").withStyle(ChatFormatting.DARK_RED));
                  }
               }
            } else {
               double effectiveRegen = manaRegen * regenMultiplier;
               vars.player_mana = Math.min(vars.player_mana + effectiveRegen, vars.player_max_mana);
            }

            if (vars.magic_cooldown > 0.0) {
               vars.magic_cooldown = Math.max(0.0, vars.magic_cooldown - regenInterval);
            }

            if (shouldTriggerEmergencyMana(vars)) {
               boolean restoredByItem = false;
               if (entity instanceof Player player) {
                  restoredByItem = tryRestoreManaFromInventory(player, vars);
               }

               if (!restoredByItem) {
                  tryRestoreManaByHealth(entity, vars);
               }
            }

            vars.syncMana(entity);
            int delay = (int)regenInterval;
            if (delay < 1) {
               delay = 1;
            }

            TYPE_MOON_WORLD.queueServerWork(delay, () -> execute(world, entity));
         }
      }
   }

   private static boolean shouldTriggerEmergencyMana(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.player_max_mana < 20.0) {
         return false;
      } else {
         return vars.player_mana >= 20.0 ? false : vars.player_mana < vars.player_max_mana;
      }
   }

   private static boolean tryRestoreManaFromInventory(Player player, TypeMoonWorldModVariables.PlayerVariables vars) {
      Restore_mana.SourceCandidate best = null;
      double need = vars.player_max_mana - vars.player_mana;
      if (need <= 0.0) {
         return false;
      } else {
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
               Restore_mana.SourceCandidate candidate = createCandidate(i, stack);
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

         if (best == null) {
            return false;
         } else {
            consumeCandidate(player, best);
            vars.player_mana = Math.min(vars.player_mana + best.mana, vars.player_max_mana);
            return true;
         }
      }
   }

   private static Restore_mana.SourceCandidate createCandidate(int slot, ItemStack stack) {
      if (stack.getItem() instanceof FullManaCarvedGemItem fullGemItem) {
         return new Restore_mana.SourceCandidate(slot, fullGemItem.getManaAmount(), new ItemStack(fullGemItem.getEmptyGemItem()));
      } else if (stack.is(ModItems.MAGIC_FRAGMENTS.get())) {
         return new Restore_mana.SourceCandidate(slot, 10.0, ItemStack.EMPTY);
      } else {
         return stack.is(((Block)ModBlocks.SPIRIT_VEIN_BLOCK.get()).asItem()) ? new Restore_mana.SourceCandidate(slot, 90.0, ItemStack.EMPTY) : null;
      }
   }

   private static void consumeCandidate(Player player, Restore_mana.SourceCandidate candidate) {
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

   private static void tryRestoreManaByHealth(Entity entity, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (entity instanceof LivingEntity living) {
         if (!(living.getHealth() <= 2.0F)) {
            vars.player_mana = Math.min(vars.player_mana + 20.0, vars.player_max_mana);
            living.setHealth(living.getHealth() - 2.0F);
         }
      }
   }

   private record SourceCandidate(int slot, double mana, ItemStack remainder) {
   }
}
