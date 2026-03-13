package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan.MagicCyanThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldUse;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldWinterRiver;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity.GemGravityFieldMagic;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireWinterFrost;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazReinforcement;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazThrow;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class GemUseService {
   private static final String BASIC_JEWEL_MAGIC_ID = "jewel_magic_shoot";
   private static final String ADVANCED_JEWEL_MAGIC_ID = "jewel_magic_release";

   private GemUseService() {
   }

   public static ItemStack useFilledGem(Level level, Player player, InteractionHand hand, ItemStack heldStack, GemType type) {
      if (player instanceof ServerPlayer serverPlayer) {
         if (!isBasicMagicUnlocked(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.gem.throw.locked"), true);
            return heldStack;
         } else {
            GemEngravingService.CastResult engravedCast = GemEngravingService.tryCastEngravedMagic(serverPlayer, hand, heldStack);
            if (engravedCast != GemEngravingService.CastResult.NOT_ENGRAVED) {
               return serverPlayer.getItemInHand(hand);
            } else if (type == GemType.WHITE_GEMSTONE) {
               useWhiteGem(serverPlayer, hand, heldStack);
               return serverPlayer.getItemInHand(hand);
            } else if (type == GemType.BLACK_SHARD) {
               useBlackShardGem(serverPlayer, hand, heldStack);
               return serverPlayer.getItemInHand(hand);
            } else {
               double qualityMultiplier = resolveQualityMultiplier(heldStack);
               if (tryCastAdvancedOnlyWithHighQualityGem(serverPlayer, hand, heldStack, type, qualityMultiplier)) {
                  return serverPlayer.getItemInHand(hand);
               } else {
                  int beforeCount = countFullGemCount(serverPlayer, type);
                  int beforeHighCount = countFullGemCountByQuality(serverPlayer, type, GemQuality.HIGH);
                  castBasicByType(serverPlayer, type);
                  int afterCount = countFullGemCount(serverPlayer, type);
                  int afterHighCount = countFullGemCountByQuality(serverPlayer, type, GemQuality.HIGH);
                  if (afterCount < beforeCount) {
                     triggerAdvancedOnHighQuality(serverPlayer, type, qualityMultiplier, afterHighCount < beforeHighCount);
                  }

                  return serverPlayer.getItemInHand(hand);
               }
            }
         }
      } else {
         return heldStack;
      }
   }

   private static void castBasicByType(ServerPlayer player, GemType type) {
      switch (type) {
         case RUBY:
            MagicRubyThrow.execute(player);
            break;
         case SAPPHIRE:
            MagicSapphireThrow.execute(player);
            break;
         case EMERALD:
            MagicEmeraldUse.execute(player);
            break;
         case TOPAZ:
            MagicTopazThrow.execute(player);
            break;
         case CYAN:
            MagicCyanThrow.execute(player);
         case WHITE_GEMSTONE:
         case BLACK_SHARD:
      }
   }

   private static void useWhiteGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack) {
      double manaAmount = 100.0;
      if (heldStack.getItem() instanceof FullManaCarvedGemItem fullGem) {
         manaAmount = Math.max(1.0, fullGem.getManaAmount(heldStack));
      }

      double powerNormalized = normalizeWhiteGemPower(manaAmount);
      double radius = 6.0 + 8.0 * powerNormalized;
      float baseDamage = (float)(1.0 + 2.0 * powerNormalized);
      double baseHorizontalKnockback = 1.2 + 2.4 * powerNormalized;
      double baseVerticalKnockback = 0.25 + 0.55 * powerNormalized;
      if (player.level() instanceof ServerLevel serverLevel) {
         spawnWhiteShockwaveParticles(serverLevel, player, radius, powerNormalized);
      }

      for (LivingEntity target : player.level()
         .getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(radius, 3.5 + powerNormalized * 1.5, radius),
            targetx -> isWhiteShockwaveTarget(player, targetx)
         )) {
         double dx = target.getX() - player.getX();
         double dz = target.getZ() - player.getZ();
         double distance = Math.sqrt(dx * dx + dz * dz);
         if (distance < 1.0E-4) {
            double randomAngle = player.getRandom().nextDouble() * (Math.PI * 2);
            dx = Math.cos(randomAngle);
            dz = Math.sin(randomAngle);
            distance = 1.0;
         }

         double nx = dx / distance;
         double nz = dz / distance;
         double falloff = 1.0 - Math.min(1.0, distance / radius);
         float damage = (float)Math.max(0.5, baseDamage * (0.5 + 0.5 * falloff));
         target.hurt(player.damageSources().indirectMagic(player, player), damage);
         double horizontal = baseHorizontalKnockback * (0.35 + 0.65 * falloff);
         double vertical = baseVerticalKnockback * (0.45 + 0.55 * falloff);
         target.push(nx * horizontal, vertical, nz * horizontal);
      }

      heldStack.shrink(1);
      if (heldStack.isEmpty()) {
         player.setItemInHand(hand, ItemStack.EMPTY);
      }
   }

   private static boolean isWhiteShockwaveTarget(ServerPlayer player, LivingEntity target) {
      if (!target.isAlive() || target == player) {
         return false;
      } else if (!player.isAlliedTo(target) && !target.isAlliedTo(player)) {
         return target instanceof Player other && (other.isSpectator() || other.isCreative())
            ? false
            : !(target instanceof TamableAnimal tamable && tamable.isOwnedBy(player));
      } else {
         return false;
      }
   }

   private static double normalizeWhiteGemPower(double manaAmount) {
      double normalized = (manaAmount - 50.0) / 150.0;
      return Math.max(0.0, Math.min(1.0, normalized));
   }

   private static void spawnWhiteShockwaveParticles(ServerLevel level, ServerPlayer player, double radius, double powerNormalized) {
      double cx = player.getX();
      double cy = player.getY() + 1.0;
      double cz = player.getZ();
      int burstLevel = (int)Math.round(1.0 + powerNormalized * 2.0);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 2 + burstLevel, 0.25, 0.25, 0.25, 0.0);
      level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 10 + 8 * burstLevel, 0.5, 0.3, 0.5, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.1, cz, 420 + 260 * burstLevel, radius * 0.42, 0.9, radius * 0.42, 0.25);
      level.sendParticles(ParticleTypes.CLOUD, cx, cy - 0.2, cz, 560 + 360 * burstLevel, radius * 0.5, 0.35, radius * 0.5, 0.18);
      level.sendParticles(ParticleTypes.POOF, cx, cy, cz, 520 + 320 * burstLevel, radius * 0.56, 0.4, radius * 0.56, 0.12);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 240 + 160 * burstLevel, radius * 0.45, 0.55, radius * 0.45, 0.22);
      level.sendParticles(ParticleTypes.WAX_ON, cx, cy + 0.2, cz, 180 + 120 * burstLevel, radius * 0.52, 0.7, radius * 0.52, 0.09);
      int rings = 4;

      for (int r = 1; r <= rings; r++) {
         double ringRadius = radius * ((double)r / rings);
         int points = 72 + r * 24;
         double yOffset = 0.08 + (r % 2 == 0 ? 0.18 : 0.03);

         for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2) * i / points;
            double px = cx + Math.cos(angle) * ringRadius;
            double pz = cz + Math.sin(angle) * ringRadius;
            level.sendParticles(ParticleTypes.END_ROD, px, cy + yOffset, pz, 2, 0.04, 0.02, 0.04, 0.06);
            level.sendParticles(ParticleTypes.CLOUD, px, cy + yOffset - 0.08, pz, 1, 0.03, 0.01, 0.03, 0.02);
         }
      }
   }

   private static void useBlackShardGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack) {
      if (GemGravityFieldMagic.throwBlackShardGravityProjectile(player, heldStack)) {
         heldStack.shrink(1);
         if (heldStack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
         }
      }
   }

   private static int countFullGemCount(ServerPlayer player, GemType type) {
      int count = 0;

      for (ItemStack stack : player.getInventory().items) {
         if (stack.getItem() instanceof FullManaCarvedGemItem fullGemItem && fullGemItem.getType() == type) {
            count += stack.getCount();
         }
      }

      ItemStack offhand = player.getOffhandItem();
      if (offhand.getItem() instanceof FullManaCarvedGemItem fullGemItem && fullGemItem.getType() == type) {
         count += offhand.getCount();
      }

      return count;
   }

   private static int countFullGemCountByQuality(ServerPlayer player, GemType type, GemQuality quality) {
      int count = 0;

      for (ItemStack stack : player.getInventory().items) {
         if (stack.getItem() instanceof FullManaCarvedGemItem fullGemItem && fullGemItem.getType() == type && fullGemItem.getQuality() == quality) {
            count += stack.getCount();
         }
      }

      ItemStack offhand = player.getOffhandItem();
      if (offhand.getItem() instanceof FullManaCarvedGemItem fullGemItem && fullGemItem.getType() == type && fullGemItem.getQuality() == quality) {
         count += offhand.getCount();
      }

      return count;
   }

   private static void triggerAdvancedOnHighQuality(ServerPlayer player, GemType type, double qualityMultiplier, boolean highQualityGem) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.learned_magics.contains("jewel_magic_release") && highQualityGem) {
         float avgMultiplier = (float)Math.max(0.4, qualityMultiplier);
         switch (type) {
            case RUBY:
            case BLACK_SHARD:
            default:
               break;
            case SAPPHIRE:
               MagicSapphireWinterFrost.executeFromCombo(player, avgMultiplier);
               break;
            case EMERALD:
               MagicEmeraldWinterRiver.executeFromCombo(player, avgMultiplier);
               break;
            case TOPAZ:
               MagicTopazReinforcement.executeFromCombo(player, avgMultiplier);
               break;
            case CYAN:
               markLatestCyanProjectileAsTornado(player, avgMultiplier);
               break;
            case WHITE_GEMSTONE:
               player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
               if (player.level() instanceof ServerLevel serverLevel) {
                  serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.6, 0.6, 0.6, 0.02);
               }
         }
      }
   }

   private static boolean tryCastAdvancedOnlyWithHighQualityGem(
      ServerPlayer player, InteractionHand hand, ItemStack heldStack, GemType type, double qualityMultiplier
   ) {
      if (supportsAdvancedOnly(type) && isAdvancedMagicUnlocked(player) && isHighQualityGem(heldStack, type)) {
         ItemStack consumedGem = consumeGemFromHand(player, hand, heldStack);
         if (consumedGem.isEmpty()) {
            return false;
         } else {
            float avgMultiplier = (float)Math.max(0.4, qualityMultiplier);
            switch (type) {
               case SAPPHIRE:
                  MagicSapphireWinterFrost.executeFromCombo(player, avgMultiplier);
                  break;
               case EMERALD:
                  MagicEmeraldWinterRiver.executeFromCombo(player, avgMultiplier);
                  break;
               case TOPAZ:
                  MagicTopazReinforcement.executeFromCombo(player, avgMultiplier);
                  break;
               case CYAN:
                  throwCyanTornadoProjectile(player, consumedGem, avgMultiplier);
                  break;
               default:
                  return false;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean supportsAdvancedOnly(GemType type) {
      return type == GemType.EMERALD || type == GemType.SAPPHIRE || type == GemType.TOPAZ || type == GemType.CYAN;
   }

   private static boolean isAdvancedMagicUnlocked(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.learned_magics.contains("jewel_magic_release");
   }

   private static boolean isBasicMagicUnlocked(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.learned_magics.contains("jewel_magic_shoot");
   }

   private static boolean isHighQualityGem(ItemStack stack, GemType type) {
      return !(stack.getItem() instanceof FullManaCarvedGemItem fullGemItem)
         ? false
         : fullGemItem.getType() == type && fullGemItem.getQuality() == GemQuality.HIGH;
   }

   private static ItemStack consumeGemFromHand(ServerPlayer player, InteractionHand hand, ItemStack heldStack) {
      if (heldStack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack consumed = heldStack.copy();
         consumed.setCount(1);
         heldStack.shrink(1);
         if (heldStack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
         }

         return consumed;
      }
   }

   private static void throwCyanTornadoProjectile(ServerPlayer player, ItemStack consumedGem, float avgMultiplier) {
      ItemStack projectileStack = consumedGem.copy();
      projectileStack.setCount(1);
      CompoundTag tag = ((CustomData)projectileStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      float radius = (float)(6.0 * avgMultiplier);
      int duration = (int)(100.0 * (1.0 + 0.5 * avgMultiplier));
      tag.putBoolean("IsCyanTornado", true);
      tag.putFloat("CyanRadius", radius);
      tag.putInt("CyanDuration", duration);
      projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      RubyProjectileEntity projectile = new RubyProjectileEntity(player.level(), player);
      projectile.setGemType(4);
      projectile.setItem(projectileStack);
      Vec3 direction = EntityUtils.getAutoAimDirection(player, 48.0, 55.0);
      projectile.shoot(direction.x, direction.y, direction.z, 1.5F, 1.0F);
      player.level().addFreshEntity(projectile);
      if (player.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.CLOUD, projectile.getX(), projectile.getY(), projectile.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
      }
   }

   private static boolean markLatestCyanProjectileAsTornado(ServerPlayer player, double avgMultiplier) {
      List<RubyProjectileEntity> projectiles = player.level()
         .getEntitiesOfClass(
            RubyProjectileEntity.class,
            player.getBoundingBox().inflate(48.0),
            p -> p.isAlive() && p.getOwner() == player && p.getGemType() == 4 && p.tickCount <= 8
         );
      if (projectiles.isEmpty()) {
         return false;
      } else {
         RubyProjectileEntity target = projectiles.stream()
            .min(Comparator.<RubyProjectileEntity>comparingInt(p -> p.tickCount).thenComparingDouble(p -> p.distanceToSqr(player)))
            .orElse(null);
         if (target == null) {
            return false;
         } else {
            ItemStack projectileStack = target.getItem().copy();
            if (projectileStack.isEmpty()) {
               return false;
            } else {
               CompoundTag tag = ((CustomData)projectileStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
               if (tag.getBoolean("IsCyanTornado")) {
                  return false;
               } else {
                  float radius = (float)(6.0 * avgMultiplier);
                  int duration = (int)(100.0 * (1.0 + 0.5 * avgMultiplier));
                  tag.putBoolean("IsCyanTornado", true);
                  tag.putFloat("CyanRadius", radius);
                  tag.putInt("CyanDuration", duration);
                  projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                  target.setItem(projectileStack);
                  if (player.level() instanceof ServerLevel serverLevel) {
                     serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
                  }

                  return true;
               }
            }
         }
      }
   }

   private static double resolveQualityMultiplier(ItemStack stack) {
      return stack.getItem() instanceof FullManaCarvedGemItem fullGem ? fullGem.getQuality().getEffectMultiplier() : 1.0;
   }
}
