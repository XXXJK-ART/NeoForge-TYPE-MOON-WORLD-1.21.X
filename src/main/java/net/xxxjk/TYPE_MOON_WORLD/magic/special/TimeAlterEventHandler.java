package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.TimeAlterVisualStateMessage;
import org.joml.Vector3f;

@EventBusSubscriber(modid = "typemoonworld")
public final class TimeAlterEventHandler {
   private static final String TAG_ACTIVE_UNTIL = "TypeMoonTimeAlterActiveUntil";
   private static final String TAG_MODE = "TypeMoonTimeAlterMode";
   private static final String TAG_MULTIPLIER = "TypeMoonTimeAlterMultiplier";
   private static final String TAG_COOLDOWN_UNTIL = "TypeMoonTimeAlterCooldownUntil";
   private static final String TAG_STRAIN = "TypeMoonTimeAlterStrain";
   private static final String TAG_STRAIN_UNTIL = "TypeMoonTimeAlterStrainUntil";
   private static final ResourceLocation SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_speed");
   private static final ResourceLocation ATTACK_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_attack_speed");
   private static final ResourceLocation LEGACY_JUMP_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_jump");
   private static final ResourceLocation LEGACY_GRAVITY_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_gravity");
   private static final ResourceLocation BLOCK_BREAK_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_block_break_speed");
   private static final ResourceLocation FLYING_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_flying_speed");
   private static final ResourceKey<DamageType> BACKLASH_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_backlash"));
   private static final DustParticleOptions ACCEL_DUST = new DustParticleOptions(new Vector3f(0.28F, 0.78F, 1.0F), 1.0F);
   private static final DustParticleOptions STAGNATE_DUST = new DustParticleOptions(new Vector3f(0.62F, 0.78F, 0.92F), 0.9F);
   private static final Map<UUID, Double> SERVER_ITEM_USE_PROGRESS = new HashMap<>();

   private TimeAlterEventHandler() {
   }

   public static boolean isActive(ServerPlayer player) {
      return player != null && player.getPersistentData().getLong(TAG_ACTIVE_UNTIL) > player.level().getGameTime();
   }

   public static double getEffectiveActionRate(ServerPlayer player) {
      if (!isActive(player)) {
         return 1.0;
      }
      return TimeAlterRateMath.effectiveActionRate(
         player.getPersistentData().getInt(TAG_MODE),
         player.getPersistentData().getDouble(TAG_MULTIPLIER)
      );
   }

   public static boolean canStart(ServerPlayer player, int mode, double multiplier) {
      clearLegacyRecoveryState(player);
      return true;
   }

   public static void release(ServerPlayer player) {
      if (player != null) {
         finish(player);
      }
   }

   public static void start(ServerPlayer player, int mode, double multiplier, int durationTicks) {
      long now = player.level().getGameTime();
      player.getPersistentData().putLong(TAG_ACTIVE_UNTIL, now + Math.max(1, durationTicks));
      player.getPersistentData().putInt(TAG_MODE, mode);
      player.getPersistentData().putDouble(TAG_MULTIPLIER, multiplier);
      applyModifiers(player, mode, multiplier);
      broadcastVisualState(player, true, mode, multiplier, Math.max(1, durationTicks));
   }

   @SubscribeEvent
   public static void onStartTracking(PlayerEvent.StartTracking event) {
      if (!(event.getEntity() instanceof ServerPlayer tracker) || !(event.getTarget() instanceof ServerPlayer target) || !isActive(target)) {
         return;
      }
      long remaining = target.getPersistentData().getLong(TAG_ACTIVE_UNTIL) - target.level().getGameTime();
      ModNetwork.sendToPlayer(
         tracker,
         new TimeAlterVisualStateMessage(
            target.getUUID(),
            true,
            target.getPersistentData().getInt(TAG_MODE),
            (int)Math.min(Integer.MAX_VALUE, remaining),
            getEffectiveActionRate(target)
         )
      );
   }

   @SubscribeEvent
   public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         SERVER_ITEM_USE_PROGRESS.remove(player.getUUID());
      }
   }

   @SubscribeEvent
   public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }
      double actionRate = getEffectiveActionRate(player);
      if (Math.abs(actionRate - 1.0) <= 1.0E-6) {
         SERVER_ITEM_USE_PROGRESS.remove(player.getUUID());
         return;
      }
      TimeAlterRateMath.UseAdvance advance = TimeAlterRateMath.advanceItemUse(
         SERVER_ITEM_USE_PROGRESS.getOrDefault(player.getUUID(), 0.0), actionRate
      );
      SERVER_ITEM_USE_PROGRESS.put(player.getUUID(), advance.remainingProgress());
      event.setDuration(TimeAlterRateMath.durationBeforeVanillaDecrement(event.getDuration(), advance.elapsedUseTicks()));
   }

   @SubscribeEvent
   public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         SERVER_ITEM_USE_PROGRESS.remove(player.getUUID());
      }
   }

   @SubscribeEvent
   public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         SERVER_ITEM_USE_PROGRESS.remove(player.getUUID());
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
      SERVER_ITEM_USE_PROGRESS.remove(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
         return;
      }
      long activeUntil = player.getPersistentData().getLong(TAG_ACTIVE_UNTIL);
      if (activeUntil <= 0L) {
         if (hasAnyTimeAlterModifier(player)) {
            removeModifiers(player);
         }
         return;
      }

      long now = player.level().getGameTime();
      if (activeUntil <= now || !player.isAlive()) {
         finish(player);
         return;
      }

      if (player.tickCount % 4 == 0) {
         applyModifiers(player, player.getPersistentData().getInt(TAG_MODE), player.getPersistentData().getDouble(TAG_MULTIPLIER));
      }
      if (player.tickCount % 3 == 0 && player.level() instanceof ServerLevel level) {
         int mode = player.getPersistentData().getInt(TAG_MODE);
         level.sendParticles(mode == MagicTimeAlter.MODE_ACCEL ? ACCEL_DUST : STAGNATE_DUST,
            player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(),
            mode == MagicTimeAlter.MODE_ACCEL ? 3 : 2, 0.28, 0.36, 0.28, 0.0);
      }
   }

   private static void finish(ServerPlayer player) {
      double multiplier = player.getPersistentData().getDouble(TAG_MULTIPLIER);
      int mode = player.getPersistentData().getInt(TAG_MODE);
      removeModifiers(player);
      player.getPersistentData().remove(TAG_ACTIVE_UNTIL);
      player.getPersistentData().remove(TAG_MODE);
      player.getPersistentData().remove(TAG_MULTIPLIER);
      SERVER_ITEM_USE_PROGRESS.remove(player.getUUID());
      broadcastVisualState(player, false, mode, 1.0, 0);

      float damage = (float)(multiplier * 3.0 + 2.0 * Math.pow(Math.max(0.0, multiplier - 1.0), 2.0));
      if (hasAvalon(player)) {
         damage *= 0.5F;
      }
      clearLegacyRecoveryState(player);
      player.invulnerableTime = 0;
      player.hurt(player.damageSources().source(BACKLASH_DAMAGE), damage);
      player.invulnerableTime = 0;

      player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.7F, 0.8F);
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.time_alter.release", String.format("%.1f", damage)), true);
   }

   private static void broadcastVisualState(ServerPlayer player, boolean active, int mode, double configuredMultiplier, int remainingTicks) {
      try {
         PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new TimeAlterVisualStateMessage(
               player.getUUID(),
               active,
               mode,
               Math.max(0, remainingTicks),
               active ? TimeAlterRateMath.effectiveActionRate(mode, configuredMultiplier) : 1.0
            ),
            new CustomPacketPayload[0]
         );
      } catch (UnsupportedOperationException ignored) {
         // Visual-only state may be unavailable on test or compatibility connections.
      }
   }

   private static void clearLegacyRecoveryState(ServerPlayer player) {
      player.getPersistentData().remove(TAG_COOLDOWN_UNTIL);
      player.getPersistentData().remove(TAG_STRAIN);
      player.getPersistentData().remove(TAG_STRAIN_UNTIL);
   }

   private static void applyModifiers(ServerPlayer player, int mode, double multiplier) {
      double actionRate = TimeAlterRateMath.effectiveActionRate(mode, multiplier);
      double linearAmount = TimeAlterRateMath.linearModifierAmount(actionRate);
      updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER, linearAmount);
      updateModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER, linearAmount);
       updateModifier(player.getAttribute(Attributes.BLOCK_BREAK_SPEED), BLOCK_BREAK_SPEED_MODIFIER, linearAmount);
      updateModifier(player.getAttribute(Attributes.FLYING_SPEED), FLYING_SPEED_MODIFIER, linearAmount);
   }

   private static void removeModifiers(ServerPlayer player) {
       removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER);
       removeModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER);
       removeModifier(player.getAttribute(Attributes.JUMP_STRENGTH), LEGACY_JUMP_MODIFIER);
       removeModifier(player.getAttribute(Attributes.GRAVITY), LEGACY_GRAVITY_MODIFIER);
       removeModifier(player.getAttribute(Attributes.BLOCK_BREAK_SPEED), BLOCK_BREAK_SPEED_MODIFIER);
      removeModifier(player.getAttribute(Attributes.FLYING_SPEED), FLYING_SPEED_MODIFIER);
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
      if (attribute == null) {
         return;
      }
      AttributeModifier existing = attribute.getModifier(id);
      if (Math.abs(amount) <= 1.0E-6) {
         if (existing != null) {
            attribute.removeModifier(id);
         }
         return;
      }
      if (existing != null
         && existing.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
         && Math.abs(existing.amount() - amount) <= 1.0E-6) {
         return;
      }
      if (existing != null) {
         attribute.removeModifier(id);
      }
      attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) {
         attribute.removeModifier(id);
      }
   }

   private static boolean hasAnyTimeAlterModifier(ServerPlayer player) {
      return hasModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER)
         || hasModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER)
          || hasModifier(player.getAttribute(Attributes.JUMP_STRENGTH), LEGACY_JUMP_MODIFIER)
          || hasModifier(player.getAttribute(Attributes.GRAVITY), LEGACY_GRAVITY_MODIFIER)
          || hasModifier(player.getAttribute(Attributes.BLOCK_BREAK_SPEED), BLOCK_BREAK_SPEED_MODIFIER)
         || hasModifier(player.getAttribute(Attributes.FLYING_SPEED), FLYING_SPEED_MODIFIER);
   }

   private static boolean hasModifier(AttributeInstance attribute, ResourceLocation id) {
      return attribute != null && attribute.getModifier(id) != null;
   }

   private static boolean hasAvalon(ServerPlayer player) {
      for (ItemStack stack : player.getInventory().items) {
         if (stack.is(ModItems.AVALON.get())) {
            return true;
         }
      }
      return player.getMainHandItem().is(ModItems.AVALON.get()) || player.getOffhandItem().is(ModItems.AVALON.get());
   }
}
