package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class MagicJewelMachineGun {
   private static final int MIN_GEMS_REQUIRED = 3;
   private static final int BURST_COUNT = 3;
   private static final int CHANT_TICKS = 20;
   private static final int BURST_INTERVAL_TICKS = 2;
   private static final double EMPTY_GEM_MANA_COST = 50.0;
   private static final String TAG_ACTIVE = "TypeMoonMachineGunActive";
   private static final String TAG_CHANTING = "TypeMoonMachineGunChanting";
   private static final String TAG_CHANT_END_TICK = "TypeMoonMachineGunChantEndTick";
   private static final String TAG_NEXT_BURST_TICK = "TypeMoonMachineGunNextBurstTick";
   private static final String TAG_LAST_MANA_SYNC_TICK = "TypeMoonMachineGunLastManaSyncTick";
   private static final String TAG_LAST_PROF_SYNC_TICK = "TypeMoonMachineGunLastProfSyncTick";
   private static final int MANA_SYNC_INTERVAL_TICKS = 4;
   private static final int PROF_SYNC_INTERVAL_TICKS = 20;
   private static final float[] SHOT_YAW_OFFSETS = new float[]{0.0F, -1.2F, 1.2F};
   private static final float[] SHOT_PITCH_OFFSETS = new float[]{0.0F, -0.8F, 0.8F};
   private static final double[] SHOT_SIDE_OFFSETS = new double[]{0.0, -0.12, 0.12};
   private static final double[] SHOT_UP_OFFSETS = new double[]{0.0, 0.07, -0.07};

   public static boolean execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean crestCast = vars.isCurrentSelectionFromCrest("jewel_machine_gun");
         if (!crestCast && !vars.learned_magics.contains("jewel_machine_gun")) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_learned"), true);
            return false;
         } else if (!crestCast && !vars.learned_magics.contains("jewel_magic_shoot")) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_learned"), true);
            return false;
         } else if (!crestCast && !vars.learned_magics.contains("gander")) {
            player.displayClientMessage(
               Component.translatable(
                  "message.typemoonworld.scroll.requirement_not_met", new Object[]{Component.translatable("magic.typemoonworld.gander.name")}
               ),
               true
            );
            return false;
         } else {
            CompoundTag state = player.getPersistentData();
            if (state.getBoolean("TypeMoonMachineGunActive") || state.getBoolean("TypeMoonMachineGunChanting")) {
               clearState(player);
               return true;
            } else if (!EntityUtils.hasAnyEmptyHand(player)) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.need_empty_hand"), true);
               return false;
            } else if (!hasEnoughResourceForBurst(player, vars)) {
               return false;
            } else {
               long gameTime = player.level().getGameTime();
               if (crestCast) {
                  state.putBoolean("TypeMoonMachineGunChanting", false);
                  state.putBoolean("TypeMoonMachineGunActive", true);
                  state.putLong("TypeMoonMachineGunChantEndTick", gameTime);
                  state.putLong("TypeMoonMachineGunNextBurstTick", gameTime);
               } else {
                  state.putBoolean("TypeMoonMachineGunChanting", true);
                  state.putBoolean("TypeMoonMachineGunActive", false);
                  state.putLong("TypeMoonMachineGunChantEndTick", gameTime + 20L);
                  state.putLong("TypeMoonMachineGunNextBurstTick", gameTime + 20L);
                  player.displayClientMessage(getMachineGunChantComponent(), true);
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static void tick(ServerPlayer player) {
      if (!player.level().isClientSide()) {
         CompoundTag state = player.getPersistentData();
         boolean chanting = state.getBoolean("TypeMoonMachineGunChanting");
         boolean active = state.getBoolean("TypeMoonMachineGunActive");
         if (chanting || active) {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (!canMaintainMachineGun(player, vars)) {
               clearState(player);
            } else {
               long now = player.level().getGameTime();
               if (chanting) {
                  long chantEndTick = state.getLong("TypeMoonMachineGunChantEndTick");
                  if (now < chantEndTick) {
                     return;
                  }

                  state.putBoolean("TypeMoonMachineGunChanting", false);
                  state.putBoolean("TypeMoonMachineGunActive", true);
                  state.putLong("TypeMoonMachineGunNextBurstTick", now);
                  player.level()
                     .playSound(
                        null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.CYQ_GEM_SHOOT_STAR.get(), SoundSource.PLAYERS, 1.0F, 1.0F
                     );
               }

               if (state.getBoolean("TypeMoonMachineGunActive")) {
                  long nextBurstTick = state.getLong("TypeMoonMachineGunNextBurstTick");
                  if (now >= nextBurstTick) {
                     boolean fired = fireBurst(player, vars);
                     if (!fired) {
                        clearState(player);
                     } else {
                        vars.magic_cooldown = 2.0;
                        boolean crestCast = vars.isCurrentSelectionFromCrest("jewel_machine_gun");
                        if (!crestCast) {
                           vars.proficiency_jewel_magic_release = Math.min(100.0, vars.proficiency_jewel_magic_release + 0.1);
                        }

                        syncBurstState(player, vars, now, !crestCast);
                        state.putLong("TypeMoonMachineGunNextBurstTick", now + 2L);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean canMaintainMachineGun(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!player.isAlive() || player.isSpectator()) {
         return false;
      } else if (!vars.is_magus || !vars.is_magic_circuit_open) {
         return false;
      } else if (!vars.learned_magics.contains("jewel_machine_gun") && !vars.isCurrentSelectionFromCrest("jewel_machine_gun")) {
         return false;
      } else if (vars.selected_magics.isEmpty()) {
         return false;
      } else if (!EntityUtils.hasAnyEmptyHand(player)) {
         return false;
      } else {
         int index = vars.current_magic_index;
         return index >= 0 && index < vars.selected_magics.size() ? "jewel_machine_gun".equals(vars.selected_magics.get(index)) : false;
      }
   }

   private static void clearState(ServerPlayer player) {
      CompoundTag state = player.getPersistentData();
      state.putBoolean("TypeMoonMachineGunActive", false);
      state.putBoolean("TypeMoonMachineGunChanting", false);
      state.putLong("TypeMoonMachineGunChantEndTick", 0L);
      state.putLong("TypeMoonMachineGunNextBurstTick", 0L);
      state.remove("TypeMoonMachineGunLastManaSyncTick");
      state.remove("TypeMoonMachineGunLastProfSyncTick");
   }

   private static void syncBurstState(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, long now, boolean syncProficiency) {
      CompoundTag state = player.getPersistentData();
      long lastManaSync = state.getLong("TypeMoonMachineGunLastManaSyncTick");
      if (now - lastManaSync >= 4L || vars.player_mana <= 0.0) {
         vars.syncMana(player);
         state.putLong("TypeMoonMachineGunLastManaSyncTick", now);
      }

      if (syncProficiency) {
         long lastProfSync = state.getLong("TypeMoonMachineGunLastProfSyncTick");
         if (now - lastProfSync >= 20L) {
            vars.syncProficiency(player);
            state.putLong("TypeMoonMachineGunLastProfSyncTick", now);
         }
      }
   }

   private static boolean hasEnoughResourceForBurst(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      int availableGems = countAvailableUnengravedGems(player);
      if (availableGems < 3) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.jewel_machine_gun.not_enough_gems", new Object[]{3}), true);
         return false;
      } else {
         List<MagicJewelMachineGun.ShotPlan> shotPlans = planShots(player, vars.player_mana, 3);
         if (shotPlans.size() < 3) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_enough_mana"), true);
            return false;
         } else {
            return true;
         }
      }
   }

   private static boolean fireBurst(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!hasEnoughResourceForBurst(player, vars)) {
         return false;
      } else {
         List<MagicJewelMachineGun.ShotPlan> shotPlans = planShots(player, vars.player_mana, 3);
         List<ItemStack> consumed = consumePlannedGems(player, shotPlans);
         if (consumed.size() < 3) {
            return false;
         } else {
            int emptyUsed = 0;

            for (MagicJewelMachineGun.ShotPlan plan : shotPlans) {
               if (!plan.full) {
                  emptyUsed++;
               }
            }

            if (emptyUsed > 0) {
               vars.player_mana = Math.max(0.0, vars.player_mana - emptyUsed * 50.0);
            }

            for (int i = 0; i < consumed.size(); i++) {
               shootBullet(player.level(), player, consumed.get(i), i);
            }

            return true;
         }
      }
   }

   private static int countAvailableUnengravedGems(ServerPlayer player) {
      int count = 0;

      for (ItemStack stack : player.getInventory().items) {
         if (isEligibleFullGem(stack) || isEligibleEmptyGem(stack)) {
            count += stack.getCount();
         }
      }

      ItemStack offhand = player.getOffhandItem();
      if (isEligibleFullGem(offhand) || isEligibleEmptyGem(offhand)) {
         count += offhand.getCount();
      }

      return count;
   }

   private static List<MagicJewelMachineGun.ShotPlan> planShots(ServerPlayer player, double startMana, int count) {
      int fullPoor = countGemsByBucket(player, true, GemQuality.POOR);
      int fullNormal = countGemsByBucket(player, true, GemQuality.NORMAL);
      int fullHigh = countGemsByBucket(player, true, GemQuality.HIGH);
      int emptyPoor = countGemsByBucket(player, false, GemQuality.POOR);
      int emptyNormal = countGemsByBucket(player, false, GemQuality.NORMAL);
      int emptyHigh = countGemsByBucket(player, false, GemQuality.HIGH);
      double manaLeft = startMana;
      List<MagicJewelMachineGun.ShotPlan> plans = new ArrayList<>();

      for (int i = 0; i < count; i++) {
         if (fullPoor > 0) {
            fullPoor--;
            plans.add(new MagicJewelMachineGun.ShotPlan(true, GemQuality.POOR));
         } else if (fullNormal > 0) {
            fullNormal--;
            plans.add(new MagicJewelMachineGun.ShotPlan(true, GemQuality.NORMAL));
         } else if (fullHigh > 0) {
            fullHigh--;
            plans.add(new MagicJewelMachineGun.ShotPlan(true, GemQuality.HIGH));
         } else if (emptyPoor > 0 && manaLeft >= 50.0) {
            emptyPoor--;
            manaLeft -= 50.0;
            plans.add(new MagicJewelMachineGun.ShotPlan(false, GemQuality.POOR));
         } else if (emptyNormal > 0 && manaLeft >= 50.0) {
            emptyNormal--;
            manaLeft -= 50.0;
            plans.add(new MagicJewelMachineGun.ShotPlan(false, GemQuality.NORMAL));
         } else {
            if (emptyHigh <= 0 || !(manaLeft >= 50.0)) {
               break;
            }

            emptyHigh--;
            manaLeft -= 50.0;
            plans.add(new MagicJewelMachineGun.ShotPlan(false, GemQuality.HIGH));
         }
      }

      return plans;
   }

   private static int countGemsByBucket(ServerPlayer player, boolean full, GemQuality quality) {
      int count = 0;

      for (ItemStack stack : player.getInventory().items) {
         if (matchesPlan(stack, full, quality)) {
            count += stack.getCount();
         }
      }

      ItemStack offhand = player.getOffhandItem();
      if (matchesPlan(offhand, full, quality)) {
         count += offhand.getCount();
      }

      return count;
   }

   private static List<ItemStack> consumePlannedGems(ServerPlayer player, List<MagicJewelMachineGun.ShotPlan> plans) {
      List<ItemStack> consumed = new ArrayList<>();

      for (MagicJewelMachineGun.ShotPlan plan : plans) {
         ItemStack one = consumeOneMatchingGem(player, plan.full, plan.quality);
         if (one.isEmpty()) {
            return consumed;
         }

         consumed.add(one);
      }

      return consumed;
   }

   private static ItemStack consumeOneMatchingGem(ServerPlayer player, boolean full, GemQuality quality) {
      int totalMatched = 0;

      for (ItemStack stack : player.getInventory().items) {
         if (matchesPlan(stack, full, quality)) {
            totalMatched += stack.getCount();
         }
      }

      ItemStack offhand = player.getOffhandItem();
      if (matchesPlan(offhand, full, quality)) {
         totalMatched += offhand.getCount();
      }

      if (totalMatched <= 0) {
         return ItemStack.EMPTY;
      } else {
         int pick = player.getRandom().nextInt(totalMatched);

         for (ItemStack stackx : player.getInventory().items) {
            if (matchesPlan(stackx, full, quality)) {
               if (pick < stackx.getCount()) {
                  return consumeOneFromStack(stackx);
               }

               pick -= stackx.getCount();
            }
         }

         return matchesPlan(offhand, full, quality) && pick < offhand.getCount() ? consumeOneFromStack(offhand) : ItemStack.EMPTY;
      }
   }

   private static ItemStack consumeOneFromStack(ItemStack stack) {
      ItemStack consumed = stack.copy();
      consumed.setCount(1);
      stack.shrink(1);
      return consumed;
   }

   private static boolean matchesPlan(ItemStack stack, boolean full, GemQuality quality) {
      return full
         ? isEligibleFullGem(stack) && stack.getItem() instanceof FullManaCarvedGemItem fullGem && fullGem.getQuality() == quality
         : isEligibleEmptyGem(stack) && stack.getItem() instanceof CarvedGemItem carvedGem && carvedGem.getQuality() == quality;
   }

   private static boolean isEligibleFullGem(ItemStack stack) {
      return !(stack.getItem() instanceof FullManaCarvedGemItem) ? false : GemEngravingService.getEngravedMagicId(stack) == null;
   }

   private static boolean isEligibleEmptyGem(ItemStack stack) {
      return !(stack.getItem() instanceof CarvedGemItem) ? false : GemEngravingService.getEngravedMagicId(stack) == null;
   }

   private static void shootBullet(Level level, ServerPlayer player, ItemStack gemStack, int shotIndex) {
      if (!gemStack.isEmpty()) {
         RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);
         CompoundTag tag = new CompoundTag();
         CustomData existingData = (CustomData)gemStack.get(DataComponents.CUSTOM_DATA);
         if (existingData != null) {
            tag = existingData.copyTag();
         }

         tag.putBoolean("IsMachineGunMode", true);
         ItemStack projectileStack = gemStack.copy();
         projectileStack.setCount(1);
         projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         projectile.setItem(projectileStack);
         int typeId = 0;
         GemType gemType = null;
         if (projectileStack.getItem() instanceof FullManaCarvedGemItem gemItem) {
            gemType = gemItem.getType();
         } else if (projectileStack.getItem() instanceof CarvedGemItem gemItem) {
            gemType = gemItem.getType();
         }

         if (gemType != null) {
            if (gemType == GemType.RUBY) {
               typeId = 0;
            } else if (gemType == GemType.SAPPHIRE) {
               typeId = 1;
            } else if (gemType == GemType.EMERALD) {
               typeId = 2;
            } else if (gemType == GemType.TOPAZ) {
               typeId = 3;
            } else if (gemType == GemType.CYAN) {
               typeId = 4;
            } else if (gemType == GemType.WHITE_GEMSTONE) {
               typeId = 5;
            } else if (gemType == GemType.BLACK_SHARD) {
               typeId = 6;
            }
         }

         int pattern = Math.floorMod(shotIndex, 3);
         Vec3 forward = EntityUtils.getAutoAimDirection(player, 48.0, 58.0);
         Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
         if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
         } else {
            right = right.normalize();
         }

         Vec3 up = right.cross(forward).normalize();
         Vec3 handAnchor = EntityUtils.getCurrentEmptyHandCastAnchor(player);
         Vec3 spawnPos = handAnchor.add(forward.scale(0.08)).add(right.scale(SHOT_SIDE_OFFSETS[pattern])).add(up.scale(SHOT_UP_OFFSETS[pattern]));
         projectile.setPos(spawnPos);
         Vec3 shotDirection = forward.add(right.scale(SHOT_YAW_OFFSETS[pattern] * 0.015)).add(up.scale(-SHOT_PITCH_OFFSETS[pattern] * 0.015)).normalize();
         projectile.setGemType(typeId);
         projectile.shoot(shotDirection.x, shotDirection.y, shotDirection.z, 2.8F, 0.06F);
         level.addFreshEntity(projectile);
         level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            (SoundEvent)ModSounds.CYM_GEM_BIUBIUBIU.get(),
            SoundSource.PLAYERS,
            0.5F,
            1.2F + level.random.nextFloat() * 0.2F
         );
      }
   }

   private static Component getMachineGunChantComponent() {
      return Component.literal("Call ")
         .withStyle(ChatFormatting.GRAY)
         .append(Component.literal("blue").withStyle(ChatFormatting.AQUA))
         .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
         .append(Component.literal("red").withStyle(ChatFormatting.RED))
         .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
         .append(Component.literal("green").withStyle(ChatFormatting.GREEN))
         .append(Component.literal(", for your ").withStyle(ChatFormatting.GRAY))
         .append(Component.literal("queen").withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC}));
   }

   private record ShotPlan(boolean full, GemQuality quality) {
   }
}
